package com.iambilotta.gdpr.starter.retention;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.iambilotta.gdpr.annotations.GdprRetention;

import static org.assertj.core.api.Assertions.assertThat;

class RetentionSchedulerTest {

    static class Customer {}
    static class AccessLog {}
    static class Anything {}
    static class A {}
    static class B {}
    static class C {}

    @Test
    void sweepAppliesEachTargetWithCutoffDerivedFromInjectedClock() {
        Instant fixedNow = Instant.parse("2030-01-01T00:00:00Z");
        Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);

        FakeTarget customers = new FakeTarget(Customer.class, Duration.ofDays(365 * 5L), GdprRetention.Strategy.ANONYMIZE);
        FakeTarget logs = new FakeTarget(AccessLog.class, Duration.ofDays(90), GdprRetention.Strategy.DELETE);
        RetentionScheduler scheduler = new RetentionScheduler(List.of(customers, logs), clock);

        scheduler.sweep();

        assertThat(customers.cutoffsObserved).containsExactly(fixedNow.minus(Duration.ofDays(365 * 5L)));
        assertThat(logs.cutoffsObserved).containsExactly(fixedNow.minus(Duration.ofDays(90)));
    }

    @Test
    void runWithOffsetUsesCallerProvidedDuration() {
        Instant fixedNow = Instant.parse("2030-06-15T12:00:00Z");
        Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);
        FakeTarget target = new FakeTarget(Anything.class, Duration.ofDays(30), GdprRetention.Strategy.DELETE);
        RetentionScheduler scheduler = new RetentionScheduler(List.of(target), clock);

        scheduler.runWithOffset(Duration.ofDays(7));

        assertThat(target.cutoffsObserved).containsExactly(fixedNow.minus(Duration.ofDays(7)));
    }

    @Test
    void targetCountReflectsRegisteredTargets() {
        RetentionScheduler scheduler = new RetentionScheduler(List.of(
                new FakeTarget(A.class, Duration.ofDays(1), GdprRetention.Strategy.DELETE),
                new FakeTarget(B.class, Duration.ofDays(1), GdprRetention.Strategy.DELETE),
                new FakeTarget(C.class, Duration.ofDays(1), GdprRetention.Strategy.DELETE)
        ));

        assertThat(scheduler.targetCount()).isEqualTo(3);
    }

    private static final class FakeTarget implements RetentionTarget {

        private final Class<?> type;
        private final Duration period;
        private final GdprRetention.Strategy strategy;
        private final List<Instant> cutoffsObserved = new ArrayList<>();

        FakeTarget(Class<?> type, Duration period, GdprRetention.Strategy strategy) {
            this.type = type;
            this.period = period;
            this.strategy = strategy;
        }

        @Override
        public Class<?> entityType() {
            return type;
        }

        @Override
        public Duration retentionPeriod() {
            return period;
        }

        @Override
        public GdprRetention.Strategy strategy() {
            return strategy;
        }

        @Override
        public long countDue(Instant cutoff) {
            return 0;
        }

        @Override
        public long applyDue(Instant cutoff) {
            cutoffsObserved.add(cutoff);
            return 0;
        }
    }
}
