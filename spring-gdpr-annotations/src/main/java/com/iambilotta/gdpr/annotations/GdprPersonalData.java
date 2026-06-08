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

    /**
     * Coarse classification of the personal-data element, orthogonal to {@link #specialCategory()}.
     * Drives category-grouped ROPA/DPIA output and (downstream) selective retention and redaction.
     *
     * <p>Default {@link Category#UNCATEGORISED} keeps every pre-existing annotation valid: a field
     * left unclassified is not an error, it simply carries no category axis.
     */
    Category category() default Category.UNCATEGORISED;

    /**
     * Where the field's value physically lives, which selects the erasure mechanism (ADR-0010).
     *
     * <p>Default {@link Storage#INLINE} keeps every pre-existing annotation valid: the value is
     * stored inline on the carrier and erased by a mutable-store {@code ErasureHandler} (or, for an
     * immutable event, by crypto-shredding). {@link Storage#FORGETTABLE_PAYLOAD} declares the value
     * externalised to the {@code ForgettablePayloadStore}; the carrier holds only a reference, and
     * erasure is an actual {@code DELETE} of the external row, the library's <strong>primary</strong>
     * personal-data erasure path.
     */
    Storage storage() default Storage.INLINE;

    /**
     * Coarse personal-data category dimension (Art. 5(1)(c) data minimisation + Art. 30 ROPA).
     * Deliberately small: a fine-grained taxonomy belongs in the adopter's data catalogue, not in
     * a compile-time annotation everyone has to maintain.
     */
    enum Category {

        /** No category declared. The backward-compatible default. */
        UNCATEGORISED,

        /** Identity data: legal name, national id, date of birth, photo. */
        IDENTITY,

        /** Contact data: email, phone, postal address. */
        CONTACT,

        /** Financial data: IBAN, card number, tax id, income. */
        FINANCIAL
    }

    /**
     * Physical storage location of a personal-data field, selecting its erasure mechanism (ADR-0010).
     */
    enum Storage {

        /**
         * Value stored inline on the carrier (the backward-compatible default). Erased by a mutable
         * store handler, or by crypto-shredding when the carrier is an immutable event.
         */
        INLINE,

        /**
         * Value externalised to the {@code ForgettablePayloadStore}; the carrier holds only a
         * reference. Erasure is an actual {@code DELETE} of the external row, the PRIMARY path for
         * personal data: it is anonymisation, not the pseudonymisation of crypto-shredding (Recital
         * 26, EDPB 01/2025).
         */
        FORGETTABLE_PAYLOAD
    }
}
