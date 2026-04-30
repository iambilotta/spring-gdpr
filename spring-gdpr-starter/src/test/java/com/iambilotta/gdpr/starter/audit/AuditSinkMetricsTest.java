package com.iambilotta.gdpr.starter.audit;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditSinkMetricsTest {

    private AsyncAuditSinkDecorator decorator;
    private MeterRegistry registry;

    @BeforeEach
    void setUp() {
        decorator = new AsyncAuditSinkDecorator(new CapturingSink(), 1, 16, 500L);
        registry = new SimpleMeterRegistry();
        new AuditSinkMetrics(decorator).bindTo(registry);
    }

    @AfterEach
    void tearDown() {
        decorator.close();
    }

    @Test
    void registersThreeGauges() {
        assertThat(registry.get("spring.gdpr.audit.submitted").gauge()).isNotNull();
        assertThat(registry.get("spring.gdpr.audit.dropped").gauge()).isNotNull();
        assertThat(registry.get("spring.gdpr.audit.failed").gauge()).isNotNull();
    }

    @Test
    void submittedGaugeReflectsLiveCount() throws Exception {
        decorator.write(record("e1"));
        decorator.write(record("e2"));
        Thread.sleep(100);

        double submitted = registry.get("spring.gdpr.audit.submitted").gauge().value();
        assertThat(submitted).isEqualTo(2.0);
    }

    private static AuditAccessRecord record(String eventId) {
        return new AuditAccessRecord(
                eventId, Instant.now(), "tester", "subject", "T", "m", "6(1)(b)", false);
    }

    static class CapturingSink implements AuditSink {
        @Override
        public void write(AuditAccessRecord record) {
            // no-op
        }

        @Override
        public List<AuditAccessRecord> findBySubject(String subjectId, Instant from, Instant to) {
            return List.of();
        }
    }
}
