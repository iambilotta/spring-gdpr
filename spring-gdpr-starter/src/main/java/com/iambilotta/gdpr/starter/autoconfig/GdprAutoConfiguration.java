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
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.iambilotta.gdpr.starter.GdprProperties;
import com.iambilotta.gdpr.starter.audit.ActorResolver;
import com.iambilotta.gdpr.starter.audit.AsyncAuditSinkDecorator;
import com.iambilotta.gdpr.starter.audit.AuditSink;
import com.iambilotta.gdpr.starter.audit.JdbcAuditSink;
import com.iambilotta.gdpr.starter.audit.PersonalDataAccessAdvisor;
import com.iambilotta.gdpr.starter.audit.Slf4jAuditSink;
import com.iambilotta.gdpr.starter.audit.SubjectIdResolver;
import com.iambilotta.gdpr.starter.erasure.ErasureHandler;
import com.iambilotta.gdpr.starter.erasure.ErasureService;
import com.iambilotta.gdpr.starter.retention.RetentionScheduler;
import com.iambilotta.gdpr.starter.retention.RetentionTarget;
import com.iambilotta.gdpr.starter.web.GdprController;

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

    @Bean
    @ConditionalOnMissingBean
    public ErasureService gdprErasureService(ObjectProvider<ErasureHandler> handlers) {
        List<ErasureHandler> resolved = handlers.orderedStream().toList();
        return new ErasureService(resolved.isEmpty() ? Collections.emptyList() : resolved);
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
        public GdprController gdprController(ErasureService erasureService, AuditSink auditSink) {
            return new GdprController(erasureService, auditSink);
        }
    }
}
