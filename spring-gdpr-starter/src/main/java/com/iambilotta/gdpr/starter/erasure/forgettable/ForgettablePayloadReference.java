package com.iambilotta.gdpr.starter.erasure.forgettable;

/**
 * The pointer a domain object or an event carries <strong>instead</strong> of an externalised PII
 * value (ADR-0010, the forgettable-payload pattern). It identifies a row in the mutable external
 * {@link ForgettablePayloadStore} by {@code (subjectId, fieldKey)}; it never holds the value, so
 * the carrier (a row, an immutable event) holds no personal data at all.
 *
 * <p>Erasure of the subject {@code DELETE}s (or anonymises) those store rows: the reference then
 * dangles and {@link ForgettablePayloadResolver} resolves it to empty. Because the carrier only
 * ever held the reference, this is <em>actual deletion</em> of the personal data, not the
 * key-destruction of crypto-shredding (which leaves ciphertext that is, in law, pseudonymised data;
 * see ADR-0010 on Recital 26 / EDPB 01/2025).
 *
 * <p>The {@link #toUrn() URN} form ({@code urn:gdpr:fp:<subjectId>:<fieldKey>}) is the convenient
 * single-string serialisation for storing the reference in one column or one event field.
 */
public record ForgettablePayloadReference(String subjectId, String fieldKey) {

    private static final String URN_PREFIX = "urn:gdpr:fp:";

    public ForgettablePayloadReference {
        if (subjectId == null || subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId must be provided");
        }
        if (fieldKey == null || fieldKey.isBlank()) {
            throw new IllegalArgumentException("fieldKey must be provided");
        }
    }

    /** Builds a reference, rejecting blank coordinates. */
    public static ForgettablePayloadReference of(String subjectId, String fieldKey) {
        return new ForgettablePayloadReference(subjectId, fieldKey);
    }

    /** The single-string URN serialisation: {@code urn:gdpr:fp:<subjectId>:<fieldKey>}. */
    public String toUrn() {
        return URN_PREFIX + subjectId + ":" + fieldKey;
    }

    /**
     * Parses a {@link #toUrn() URN} back into a reference. Rejects a wrong scheme or a missing
     * segment rather than returning a half-built reference (fail-closed on malformed input).
     */
    public static ForgettablePayloadReference fromUrn(String urn) {
        if (urn == null || !urn.startsWith(URN_PREFIX)) {
            throw new IllegalArgumentException("not a forgettable-payload URN: " + urn);
        }
        String body = urn.substring(URN_PREFIX.length());
        int sep = body.indexOf(':');
        if (sep < 0) {
            throw new IllegalArgumentException("forgettable-payload URN missing fieldKey: " + urn);
        }
        return of(body.substring(0, sep), body.substring(sep + 1));
    }
}
