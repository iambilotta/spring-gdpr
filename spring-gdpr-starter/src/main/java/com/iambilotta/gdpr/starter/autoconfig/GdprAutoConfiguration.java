package com.iambilotta.gdpr.starter.autoconfig;

import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

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

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.gdpr.audit", name = "jdbc-enabled", havingValue = "true")
    @ConditionalOnClass(name = "org.springframework.jdbc.core.JdbcTemplate")
    public AuditSink gdprJdbcAuditSink(GdprProperties properties, DataSource dataSource) {
        return new JdbcAuditSink(dataSource, properties.getAudit().getTable());
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditSink gdprAuditSink() {
        return new Slf4jAuditSink();
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
