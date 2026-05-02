package com.iambilotta.gdpr.starter.retention;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Sweeps every registered {@link RetentionTarget} and applies the configured strategy
 * (delete / anonymize / pseudonymize) to records past their retention deadline (Art. 5(1)(e)).
 *
 * <p>The cron expression is configurable via {@code spring.gdpr.retention.cron}. The default
 * is {@code 0 0 3 * * *} (daily at 03:00).
 *
 * <p>Tests inject a {@link Clock} to drive deterministic cutoffs without waiting for the
 * scheduler tick.
 */
public class RetentionScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(RetentionScheduler.class);

    private final List<RetentionTarget> targets;
    private final Clock clock;

    public RetentionScheduler(List<RetentionTarget> targets) {
        this(targets, Clock.systemUTC());
    }

    public RetentionScheduler(List<RetentionTarget> targets, Clock clock) {
        this.targets = targets;
        this.clock = clock;
    }

    @Scheduled(cron = "${spring.gdpr.retention.cron:0 0 3 * * *}")
    public void sweep() {
        Instant now = clock.instant();
        for (RetentionTarget target : targets) {
            Instant cutoff = now.minus(target.retentionPeriod());
            long affected = target.applyDue(cutoff);
            if (affected > 0) {
                LOG.info(
                        "gdpr_retention applied entity={} strategy={} cutoff={} affected={}",
                        target.entityType().getName(),
                        target.strategy(),
                        cutoff,
                        affected
                );
            }
        }
    }

    public int targetCount() {
        return targets.size();
    }

    /**
     * Helper for callers (CLI, ops) wanting an on-demand sweep with a custom cutoff offset.
     */
    public void runWithOffset(Duration offset) {
        Instant cutoff = clock.instant().minus(offset);
        for (RetentionTarget target : targets) {
            target.applyDue(cutoff);
        }
    }
}
