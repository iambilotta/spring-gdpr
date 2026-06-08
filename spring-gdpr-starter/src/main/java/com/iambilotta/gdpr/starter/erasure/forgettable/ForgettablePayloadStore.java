package com.iambilotta.gdpr.starter.erasure.forgettable;

import java.util.Optional;

/**
 * The mutable external PII store behind the <strong>forgettable-payload</strong> pattern (ADR-0010),
 * the library's <strong>primary</strong> erasure mechanism for personal data. A field marked
 * forgettable is not stored inline on the domain object or event; its value lives here, keyed by
 * {@code (subjectId, fieldKey)}, and the carrier holds only a {@link ForgettablePayloadReference}.
 *
 * <p><strong>Erasure = {@link #erase(String) DELETE} the subject's rows.</strong> This is
 * <em>actual</em> deletion of the personal data, not the key-destruction of crypto-shredding. That
 * is why it is the primary path: encryption-with-a-separately-held-key is, in law, pseudonymisation
 * (Recital 26, EDPB 01/2025), so the ciphertext crypto-shredding leaves behind is still personal
 * data; deleting the value outright removes it. Crypto-shredding stays the narrow exception, for the
 * case where an immutable event must legally carry the value (ADR-0009, ADR-0010).
 *
 * <p>The default implementation is JDBC ({@link JdbcForgettablePayloadStore}, table
 * {@code gdpr_forgettable_payload}). The SPI exists so an adopter can back it with any mutable store
 * (a document DB, an object store, a separate "PII vault" service) while the domain stays
 * append-only / event-sourced; only the field is externalised.
 *
 * <p><strong>Fail-closed.</strong> {@link #resolve(String, String)} returns empty for a missing or
 * erased value, never a partial or a placeholder; the value is never logged. {@link #put} refuses an
 * erased subject (the tombstone below), so an erasure cannot be silently undone.
 *
 * <p><strong>Tombstone / no-resurrection.</strong> {@link #erase(String)} records that the subject
 * was erased; a later {@link #put} for that subject throws rather than re-creating their data. This
 * mirrors the key-store's guarantee, so the two mechanisms behave identically from the adopter's
 * side.
 */
public interface ForgettablePayloadStore {

    /**
     * Writes (or overwrites) the value for {@code (subjectId, fieldKey)}. Last-writer-wins upsert:
     * the store is mutable, unlike the append-only carrier. MUST refuse a subject that was already
     * {@link #erase(String) erased} (the tombstone), so a put cannot resurrect erased personal data.
     *
     * @throws IllegalStateException if the subject was erased
     */
    void put(String subjectId, String fieldKey, String value);

    /**
     * The value for {@code (subjectId, fieldKey)} if present, otherwise empty. Empty after
     * {@link #erase(String)} (the rows were deleted) and empty for a coordinate that was never
     * written. Fail-closed: never returns a partial value or a placeholder.
     */
    Optional<String> resolve(String subjectId, String fieldKey);

    /**
     * Erases the subject: deletes every externalised value for them and records a tombstone so a
     * later {@link #put} cannot re-create their data. Returns the number of value rows actually
     * deleted (0 when the subject had none). Idempotent: erasing again is a no-op that still keeps
     * the tombstone.
     */
    int erase(String subjectId);
}
