package com.iambilotta.gdpr.starter.retention;

import java.time.Duration;
import java.time.Instant;

import com.iambilotta.gdpr.annotations.GdprRetention;

/**
 * Single contract for "things subject to retention enforcement". One target per
 * {@link GdprRetention}-annotated entity.
 *
 * <p>The starter does not assume JPA. Implementations adapt to JPA, JDBC, MongoDB, or any
 * persistence layer the user already runs. The scheduler calls {@link #countDue(Instant)}
 * for metrics and {@link #applyDue(Instant)} to enforce the policy.
 */
public interface RetentionTarget {

    /**
     * FQN of the annotated entity. Used in logs and ROPA exports.
     */
    String entityType();

    /**
     * Effective retention period derived from the annotation.
     */
    Duration retentionPeriod();

    GdprRetention.Strategy strategy();

    long countDue(Instant cutoff);

    /**
     * Apply the strategy to all records older than {@code cutoff}.
     *
     * @return number of records affected
     */
    long applyDue(Instant cutoff);
}
