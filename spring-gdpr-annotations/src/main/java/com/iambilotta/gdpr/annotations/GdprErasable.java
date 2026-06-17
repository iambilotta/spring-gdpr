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
        PSEUDONYMIZE,

        /**
         * Forgettable-payload erasure (the PRIMARY append-only-safe pattern, ADR-0010): the type's
         * personal data lives in an external {@code ForgettablePayloadStore}, and erasure is an actual
         * {@code DELETE} of those rows while the immutable carrier keeps only a dangling reference.
         *
         * <p>Declaring it auto-wires a {@code ForgettablePayloadErasureHandler} for this type with no
         * hand config (the starter contributes the store + resolver beans, overridable). Prefer this
         * over {@link #CRYPTO_SHRED} for personal data: deleting the value is anonymisation, whereas a
         * dropped key leaves ciphertext that is, in law, still pseudonymised personal data (Recital 26,
         * EDPB 01/2025).
         */
        FORGETTABLE,

        /**
         * Crypto-shredding erasure (the append-only-safe EXCEPTION, ADR-0009): the type's personal data
         * is encrypted inline with a per-subject key, and erasure is the drop of that key, after which
         * the byte-immutable ciphertext is permanently unreadable while no event is touched.
         *
         * <p>Declaring it auto-wires a {@code CryptoShreddingErasureHandler} for this type with no hand
         * config (the starter contributes the key store + shredder beans, overridable). Use only where
         * an immutable event must legally carry the value inline and externalising it via
         * {@link #FORGETTABLE} is not acceptable (a signed/notarised event, an integrity requirement).
         */
        CRYPTO_SHRED
    }
}
