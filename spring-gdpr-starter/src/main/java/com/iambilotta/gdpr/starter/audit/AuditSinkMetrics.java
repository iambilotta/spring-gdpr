package com.iambilotta.gdpr.starter.audit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Micrometer binder for the async decorator's three counters
 * ({@code spring.gdpr.audit.submitted}, {@code spring.gdpr.audit.dropped},
 * {@code spring.gdpr.audit.failed}).
 *
 * <p>Registered automatically by {@code GdprAutoConfiguration} when Micrometer is on the
 * classpath AND the audit sink is wrapped by {@link AsyncAuditSinkDecorator}. The three
 * counters are gauges over the decorator's internal LongAdder/AtomicLong state, so the
 * meter values reflect the live total at each scrape.
 *
 * <p>Alert recipe: page on {@code rate(spring_gdpr_audit_dropped_total[5m]) > 0}. A
 * positive drop rate means the audit pipeline is saturated and personal-data accesses
 * are missing from the compliance log.
 */
public class AuditSinkMetrics implements MeterBinder {

    private final AsyncAuditSinkDecorator decorator;

    public AuditSinkMetrics(AsyncAuditSinkDecorator decorator) {
        this.decorator = decorator;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        registry.gauge(
                "spring.gdpr.audit.submitted",
                decorator,
                AsyncAuditSinkDecorator::submittedCount);
        registry.gauge(
                "spring.gdpr.audit.dropped",
                decorator,
                AsyncAuditSinkDecorator::droppedCount);
        registry.gauge(
                "spring.gdpr.audit.failed",
                decorator,
                AsyncAuditSinkDecorator::failedCount);
    }
}
