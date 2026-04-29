package com.iambilotta.gdpr.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field, parameter, type, or method as carrying GDPR personal data
 * (any data relating to an identified or identifiable natural person, Art. 4(1)).
 *
 * <p>Presence of this annotation triggers AOP audit logging at access time and
 * inclusion in the build-time DPIA + ROPA generators.
 *
 * <p>Removing this annotation removes the compliance claim. Code review catches
 * inconsistencies (refactoring-safe evidence-as-code).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE, ElementType.METHOD})
public @interface GdprPersonalData {

    /**
     * Free-form description of the personal data element. Surfaces in DPIA + ROPA artifacts.
     */
    String description() default "";

    /**
     * Whether the data falls under Art. 9 (special categories: race, health, biometric, etc.)
     * or Art. 10 (criminal convictions). Increases DPIA risk score.
     */
    boolean specialCategory() default false;
}
