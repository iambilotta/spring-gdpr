package com.iambilotta.gdpr.starter.erasure.forgettable;

import java.util.Optional;

/**
 * The read side of the forgettable-payload pattern (ADR-0010): turns a
 * {@link ForgettablePayloadReference} carried by a domain object or event back into its value by
 * loading it from the external {@link ForgettablePayloadStore}.
 *
 * <p><strong>Fail-closed.</strong> {@link #resolve(ForgettablePayloadReference)} returns empty when
 * the value was erased (the rows deleted) or never written: a dangling reference exposes no PII. The
 * {@link #require(ForgettablePayloadReference)} form throws {@link PayloadNotAvailableException}
 * rather than substituting a placeholder, so an erased subject is never silently treated as a live
 * one. The value itself is never logged or placed in an exception message.
 */
public final class ForgettablePayloadResolver {

    private final ForgettablePayloadStore store;

    public ForgettablePayloadResolver(ForgettablePayloadStore store) {
        this.store = store;
    }

    /** The referenced value if it is still present, otherwise empty (erased or never written). */
    public Optional<String> resolve(ForgettablePayloadReference reference) {
        if (reference == null) {
            return Optional.empty();
        }
        return store.resolve(reference.subjectId(), reference.fieldKey());
    }

    /**
     * The referenced value, or {@link PayloadNotAvailableException} if it is not available. Use when
     * the calling flow genuinely needs the value and a dangling (erased) reference is an error, not a
     * routine empty. The exception names only the coordinates, never the value.
     */
    public String require(ForgettablePayloadReference reference) {
        return resolve(reference).orElseThrow(() -> new PayloadNotAvailableException(reference));
    }

    /**
     * Thrown by {@link #require(ForgettablePayloadReference)} when a referenced value is absent
     * (erased or never written). Carries the reference coordinates only, never any plaintext.
     */
    public static final class PayloadNotAvailableException extends RuntimeException {

        public PayloadNotAvailableException(ForgettablePayloadReference reference) {
            super("forgettable payload not available for " + reference.toUrn());
        }
    }
}
