package com.iambilotta.gdpr.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type as participating in the right-to-erasure flow (Art. 17).
 *
 * <p>The erasure REST endpoint locates all {@code @GdprErasable} types whose records
 * reference the requested subject id, then applies the declared {@link #strategy()}.
 *
 * <p>Use {@link #subjectIdField()} to override the default field name used to bind
 * a record to a data subject.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GdprErasable {

    Strategy strategy() default Strategy.DELETE;

    /**
     * Field name on the annotated entity that holds the subject identifier. Defaults to "subjectId".
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
