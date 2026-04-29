package com.iambilotta.gdpr.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the retention policy applied to the annotated type's records (Art. 5(1)(e)
 * storage limitation).
 *
 * <p>The retention scheduler reads this annotation and applies the configured strategy
 * (delete / anonymize / pseudonymize) to records whose age exceeds {@link #period()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GdprRetention {

    /**
     * ISO-8601 period (e.g. "P30D", "P2Y", "P6M"). Time elapsed since the entity's
     * creation timestamp before the {@link #strategy()} is applied.
     */
    String period();

    Strategy strategy() default Strategy.DELETE;

    /**
     * Name of the field carrying the creation timestamp on the entity (default "createdAt").
     */
    String createdAtField() default "createdAt";

    enum Strategy {
        /** Hard delete from the database. */
        DELETE,
        /** Replace personal-data fields with null / sentinel values. */
        ANONYMIZE,
        /** Replace personal-data fields with a deterministic pseudonym (subject id mapped). */
        PSEUDONYMIZE
    }
}
