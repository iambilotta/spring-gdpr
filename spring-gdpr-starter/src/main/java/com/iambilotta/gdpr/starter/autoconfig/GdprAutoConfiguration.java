package com.iambilotta.gdpr.starter.autoconfig;

import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.iambilotta.gdpr.starter.GdprProperties;
import com.iambilotta.gdpr.starter.access.AccessExportService;
import com.iambilotta.gdpr.starter.access.SubjectDataProvider;
import com.iambilotta.gdpr.starter.audit.ActorResolver;
import com.iambilotta.gdpr.starter.audit.AsyncAuditSinkDecorator;
import com.iambilotta.gdpr.starter.audit.AuditSink;
import com.iambilotta.gdpr.starter.audit.AuditSinkMetrics;
import com.iambilotta.gdpr.starter.audit.JdbcAuditSink;
import com.iambilotta.gdpr.starter.audit.PersonalDataAccessAdvisor;
import com.iambilotta.gdpr.starter.audit.Slf4jAuditSink;
import com.iambilotta.gdpr.starter.audit.SubjectIdResolver;
import com.iambilotta.gdpr.starter.erasure.ErasureHandler;
import com.iambilotta.gdpr.starter.erasure.ErasureListener;
import com.iambilotta.gdpr.starter.erasure.ErasureService;
import com.iambilotta.gdpr.starter.erasure.crypto.InMemorySubjectKeyStore;
import com.iambilotta.gdpr.starter.erasure.crypto.JdbcSubjectKeyStore;
import com.iambilotta.gdpr.starter.erasure.crypto.SubjectKeyStore;
import com.iambilotta.gdpr.starter.erasure.forgettable.ForgettablePayloadResolver;
import com.iambilotta.gdpr.starter.erasure.forgettable.ForgettablePayloadStore;
import com.iambilotta.gdpr.starter.erasure.forgettable.InMemoryForgettablePayloadStore;
import com.iambilotta.gdpr.starter.erasure.forgettable.JdbcForgettablePayloadStore;
import com.iambilotta.gdpr.starter.retention.RetentionScheduler;
import com.iambilotta.gdpr.starter.retention.RetentionTarget;
import com.iambilotta.gdpr.starter.web.GdprController;
import com.iambilotta.gdpr.starter.web.GdprExceptionHandler;
import com.iambilotta.gdpr.starter.web.GdprSecurityWarning;

@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnProperty(prefix = "spring.gdpr", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(GdprProperties.class)
public class GdprAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(GdprAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public ActorResolver gdprActorResolver() {
        return ActorResolver.fixed("system");
    }

    @Bean
    @ConditionalOnMissingBean
    public SubjectIdResolver gdprSubjectIdResolver() {
        return SubjectIdResolver.byParameterName();
    }

    /**
     * Single decision point for the audit sink. Three outcomes, one bean:
     * <ol>
     *   <li>{@code spring.gdpr.audit.jdbc-enabled=true} AND a {@link DataSource} bean
     *       exists AND {@code JdbcTemplate} on classpath: returns {@link JdbcAuditSink}</li>
     *   <li>{@code jdbc-enabled=true} but DataSource missing: logs WARN and falls back
     *       to {@link Slf4jAuditSink} so the app starts. Misconfiguration is a runtime
     *       warning, not a startup crash.</li>
     *   <li>Default: {@link Slf4jAuditSink}</li>
     * </ol>
     *
     * <p>Consumers that want a custom sink declare their own {@link AuditSink} bean and
     * the {@code @ConditionalOnMissingBean} guard skips this factory entirely.
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditSink gdprAuditSink(GdprProperties properties, ObjectProvider<DataSource> dataSourceProvider) {
        AuditSink base = buildBaseSink(properties, dataSourceProvider);
        GdprProperties.Async async = properties.getAudit().getAsync();
        if (!async.isEnabled()) {
            return base;
        }
        return new AsyncAuditSinkDecorator(
                base, async.getThreadCount(), async.getQueueCapacity(), async.getAwaitMillis());
    }

    private AuditSink buildBaseSink(GdprProperties properties, ObjectProvider<DataSource> dataSourceProvider) {
        if (!properties.getAudit().isJdbcEnabled()) {
            return new Slf4jAuditSink();
        }
        if (!isJdbcTemplateOnClasspath()) {
            LOG.warn(
                    "spring.gdpr.audit.jdbc-enabled=true but spring-jdbc is not on the classpath. "
                            + "Falling back to Slf4jAuditSink. Add spring-boot-starter-jdbc to enable JDBC persistence.");
            return new Slf4jAuditSink();
        }
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            LOG.warn(
                    "spring.gdpr.audit.jdbc-enabled=true but no DataSource bean is available. "
                            + "Falling back to Slf4jAuditSink. Configure spring.datasource.* or supply a DataSource bean.");
            return new Slf4jAuditSink();
        }
        return new JdbcAuditSink(
                dataSource,
                properties.getAudit().getTable(),
                properties.getAudit().isAutoCreateSchema());
    }

    @Bean
    @ConditionalOnMissingBean
    public PersonalDataAccessAdvisor gdprPersonalDataAccessAdvisor(
            AuditSink sink, ActorResolver actorResolver, SubjectIdResolver subjectIdResolver) {
        return new PersonalDataAccessAdvisor(sink, actorResolver, subjectIdResolver);
    }

    /**
     * The Art. 17 orchestrator. Wires every {@link ErasureHandler} the adopter registered, plus the
     * post-erasure extension points (issue #37): every {@link ErasureListener} bean and the context's
     * {@link ApplicationEventPublisher}, so an event-sourced / CQRS consumer can rebuild a projection
     * after a forgettable-payload reference is erased (ADR-0010). No listener registered = no-op.
     */
    @Bean
    @ConditionalOnMissingBean
    public ErasureService gdprErasureService(
            ObjectProvider<ErasureHandler> handlers,
            ObjectProvider<ErasureListener> listeners,
            ApplicationEventPublisher eventPublisher) {
        List<ErasureHandler> resolvedHandlers = handlers.orderedStream().toList();
        List<ErasureListener> resolvedListeners = listeners.orderedStream().toList();
        return new ErasureService(
                resolvedHandlers.isEmpty() ? Collections.emptyList() : resolvedHandlers,
                resolvedListeners,
                eventPublisher);
    }

    /**
     * Article 15 access export (REQ-GDPR-019). Collects every {@link SubjectDataProvider} bean the
     * adopter registered; with none registered the export is simply empty, mirroring the honesty
     * contract of erasure (the library exports exactly what the providers return, never more).
     */
    @Bean
    @ConditionalOnMissingBean
    public AccessExportService gdprAccessExportService(ObjectProvider<SubjectDataProvider> providers) {
        return new AccessExportService(providers.orderedStream().toList());
    }

    private static boolean isJdbcTemplateOnClasspath() {
        try {
            Class.forName("org.springframework.jdbc.core.JdbcTemplate", false,
                    GdprAutoConfiguration.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /**
     * Declarative append-only-safe erasure (issue #36). Contributes the default store beans for the
     * two library-owned strategies and imports {@link GdprErasableScannerRegistrar}, which scans the
     * application packages and auto-wires a {@code CryptoShreddingErasureHandler} /
     * {@code ForgettablePayloadErasureHandler} per type that declares
     * {@code @GdprErasable(strategy = CRYPTO_SHRED | FORGETTABLE)}. Each store bean is
     * {@code @ConditionalOnMissingBean}, so an adopter overrides it (e.g. a KMS-backed key store or a
     * PII-vault payload store) just by declaring their own.
     *
     * <p>The default store is JDBC when a {@link DataSource} and {@code JdbcTemplate} are present, and
     * an in-memory store otherwise, mirroring the audit-sink fallback so the app starts in dev / tests
     * without a DB. The in-memory store is logged as not-for-production (keys / values live only in the
     * heap and vanish on restart, an involuntary erasure of everyone).
     */
    @Configuration(proxyBeanMethods = false)
    @Import(GdprErasableScannerRegistrar.class)
    public static class ErasureStrategyConfig {

        @Bean
        @ConditionalOnMissingBean
        public SubjectKeyStore gdprSubjectKeyStore(ObjectProvider<DataSource> dataSourceProvider) {
            DataSource dataSource = dataSourceProvider.getIfAvailable();
            if (isJdbcTemplateOnClasspath() && dataSource != null) {
                return new JdbcSubjectKeyStore(dataSource);
            }
            LOG.warn("No DataSource / spring-jdbc for the crypto-shred key store; using an in-memory "
                    + "SubjectKeyStore. NOT for production (keys live in the heap and vanish on restart, "
                    + "an involuntary erasure). Provide a DataSource or a KMS-backed SubjectKeyStore bean.");
            return new InMemorySubjectKeyStore();
        }

        @Bean
        @ConditionalOnMissingBean
        public ForgettablePayloadStore gdprForgettablePayloadStore(ObjectProvider<DataSource> dataSourceProvider) {
            DataSource dataSource = dataSourceProvider.getIfAvailable();
            if (isJdbcTemplateOnClasspath() && dataSource != null) {
                return new JdbcForgettablePayloadStore(dataSource);
            }
            LOG.warn("No DataSource / spring-jdbc for the forgettable-payload store; using an in-memory "
                    + "ForgettablePayloadStore. NOT for production (values live in the heap and vanish on "
                    + "restart). Provide a DataSource or a vault-backed ForgettablePayloadStore bean.");
            return new InMemoryForgettablePayloadStore();
        }

        @Bean
        @ConditionalOnMissingBean
        public ForgettablePayloadResolver gdprForgettablePayloadResolver(ForgettablePayloadStore store) {
            return new ForgettablePayloadResolver(store);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    public static class MetricsConfig {

        @Bean
        @ConditionalOnMissingBean
        public AuditSinkMetrics gdprAuditSinkMetrics(AuditSink sink) {
            if (sink instanceof AsyncAuditSinkDecorator decorator) {
                return new AuditSinkMetrics(decorator);
            }
            return null;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "spring.gdpr.retention", name = "enabled", havingValue = "true", matchIfMissing = true)
    @EnableScheduling
    public static class RetentionConfig {

        @Bean
        @ConditionalOnMissingBean
        public RetentionScheduler gdprRetentionScheduler(ObjectProvider<RetentionTarget> targets) {
            return new RetentionScheduler(targets.orderedStream().toList());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication
    @ConditionalOnClass(name = "org.springframework.web.bind.annotation.RestController")
    @ConditionalOnProperty(prefix = "spring.gdpr.erasure", name = "rest-enabled", havingValue = "true", matchIfMissing = true)
    public static class WebConfig {

        @Bean
        @ConditionalOnMissingBean
        public GdprController gdprController(
                ErasureService erasureService,
                AccessExportService accessExportService,
                AuditSink auditSink) {
            return new GdprController(erasureService, accessExportService, auditSink);
        }

        @Bean
        @ConditionalOnMissingBean
        public GdprExceptionHandler gdprExceptionHandler() {
            return new GdprExceptionHandler();
        }

        @Bean
        @ConditionalOnMissingBean
        public GdprSecurityWarning gdprSecurityWarning(GdprProperties properties) {
            return new GdprSecurityWarning(properties.getWeb().getBasePath());
        }
    }
}
