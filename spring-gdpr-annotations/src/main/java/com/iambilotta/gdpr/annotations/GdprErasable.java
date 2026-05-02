package com.iambilotta.gdpr.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type as participating in the right-to-erasure flow (Art. 17).
 *
 * <p>The erasure REST endpoint discovers every {@code ErasureHandler} bean, calls
 * {@code erase(subjectId)} on each in {@link #order()} ascending, and aggregates the
 * affected counts per type into the response. The {@code @GdprErasable} annotation
 * declares the user-visible metadata of the participation; the actual lookup logic
 * lives in the {@code ErasureHandler} implementation you provide.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GdprErasable {

    Strategy strategy() default Strategy.DELETE;

    /**
     * Documentation hint about which field on the annotated entity holds the subject
     * identifier. Surfaced in the generated DPIA so a reviewer can spot the binding
     * at a glance.
     *
     * <p><strong>This value does not drive the erasure lookup.</strong> The actual
     * lookup is whatever your {@code ErasureHandler.erase(subjectId)} does, with the
     * subject id resolved by {@code SubjectIdResolver} (default: a method parameter
     * literally named {@code subjectId}, case-insensitive). To change the lookup
     * strategy, override the {@code SubjectIdResolver} bean or pass the id explicitly.
     *
     * <p>Kept as documentation because GDPR audits expect a clear record of which
     * column on each table is the subject foreign key.
     */
    String subjectIdField() default "subjectId";

    /**
     * Cascade order. Lower values are erased first. Use to enforce FK-safe deletion order.
     */
    int order() default 100;

    enum Strategy {
        DELETE,
        ANONYMIZE,
        PSEUDONYMIZE
    }
}
