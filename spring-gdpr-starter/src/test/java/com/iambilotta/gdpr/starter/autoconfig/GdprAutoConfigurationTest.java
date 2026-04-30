package com.iambilotta.gdpr.starter.autoconfig;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.iambilotta.gdpr.starter.audit.AsyncAuditSinkDecorator;
import com.iambilotta.gdpr.starter.audit.AuditSink;
import com.iambilotta.gdpr.starter.audit.JdbcAuditSink;
import com.iambilotta.gdpr.starter.audit.Slf4jAuditSink;

import static org.assertj.core.api.Assertions.assertThat;

class GdprAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    GdprAutoConfiguration.class))
            .withPropertyValues("spring.gdpr.audit.async.enabled=false");

    @Test
    void defaultsToSlf4jSinkWhenJdbcDisabled() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(AuditSink.class)
                .getBean(AuditSink.class)
                .isInstanceOf(Slf4jAuditSink.class));
    }

    @Test
    void usesJdbcSinkWhenEnabledAndDataSourcePresent() {
        runner
                .withPropertyValues(
                        "spring.gdpr.audit.jdbc-enabled=true",
                        "spring.datasource.url=jdbc:h2:mem:autoconfig-test;DB_CLOSE_DELAY=-1",
                        "spring.datasource.username=sa",
                        "spring.datasource.password=")
                .run(ctx -> assertThat(ctx).hasSingleBean(AuditSink.class)
                        .getBean(AuditSink.class)
                        .isInstanceOf(JdbcAuditSink.class));
    }

    @Test
    void fallsBackToSlf4jWhenJdbcEnabledButNoDataSource() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(GdprAutoConfiguration.class))
                .withPropertyValues(
                        "spring.gdpr.audit.jdbc-enabled=true",
                        "spring.gdpr.audit.async.enabled=false")
                .run(ctx -> assertThat(ctx).hasSingleBean(AuditSink.class)
                        .getBean(AuditSink.class)
                        .isInstanceOf(Slf4jAuditSink.class));
    }

    @Test
    void respectsUserProvidedAuditSinkBean() {
        runner
                .withUserConfiguration(CustomSinkConfig.class)
                .withPropertyValues("spring.gdpr.audit.jdbc-enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(AuditSink.class);
                    assertThat(ctx.getBean(AuditSink.class)).isExactlyInstanceOf(NoopAuditSink.class);
                });
    }

    @Test
    void disablesEverythingWhenSpringGdprEnabledFalse() {
        runner
                .withPropertyValues("spring.gdpr.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(AuditSink.class));
    }

    @Test
    void wrapsSinkInAsyncDecoratorByDefault() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(GdprAutoConfiguration.class))
                .run(ctx -> assertThat(ctx).hasSingleBean(AuditSink.class)
                        .getBean(AuditSink.class)
                        .isInstanceOf(AsyncAuditSinkDecorator.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomSinkConfig {

        @Bean
        AuditSink customAuditSink() {
            return new NoopAuditSink();
        }

        @Bean
        DataSource dataSource() {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setURL("jdbc:h2:mem:custom-sink-test;DB_CLOSE_DELAY=-1");
            ds.setUser("sa");
            ds.setPassword("");
            return ds;
        }
    }

    static class NoopAuditSink implements AuditSink {

        @Override
        public void write(com.iambilotta.gdpr.starter.audit.AuditAccessRecord record) {
            // no-op
        }

        @Override
        public java.util.List<com.iambilotta.gdpr.starter.audit.AuditAccessRecord> findBySubject(
                String subjectId, java.time.Instant from, java.time.Instant to) {
            return java.util.List.of();
        }
    }
}
