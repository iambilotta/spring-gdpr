package com.iambilotta.gdpr.starter.erasure.crypto;

import java.util.Optional;

/**
 * Per-subject symmetric key store. The single source of recoverability for a subject's
 * encrypted personal data: while the key row exists the ciphertext can be decrypted, once it is
 * dropped the same ciphertext is permanently unrecoverable.
 *
 * <p><strong>Erasure = {@link #drop(String)} a subject's key.</strong> This is the GDPR Art. 17
 * act for an append-only event store (ADR-0009): no event is ever mutated or deleted; only the
 * key is removed, so the byte-immutable ciphertext in every event becomes irreversible noise.
 *
 * <p>The default implementation is JDBC ({@link JdbcSubjectKeyStore}, table
 * {@code gdpr_subject_key}). The SPI exists so an adopter can back it with a KMS / Secret Manager
 * (where the wrapped key bytes are themselves encrypted under a KEK) without changing the rest of
 * the mechanism.
 *
 * <p><strong>Key-backup constraint (ADR-0009, hard).</strong> The key store is a new critical
 * asset. Its backups MUST honour the erasure retention: a drop has to propagate to every backup,
 * or a restore would resurrect a dropped key and silently un-erase a subject. Do not back the key
 * store up with a longer retention than the erasure SLA, and never restore a key-store backup
 * taken before an erasure without re-applying the recorded drops.
 */
public interface SubjectKeyStore {

    /**
     * The subject's key if it exists, otherwise empty. Empty after {@link #drop(String)} (erased),
     * and empty for a subject that has never been minted. Never mints a key as a side effect.
     */
    Optional<byte[]> keyFor(String subjectId);

    /**
     * Mints (if absent) and returns the subject's key. Idempotent: a second call for the same
     * subject returns the same key bytes. After a {@link #drop(String)} this MUST NOT silently mint
     * a fresh key for the same id (that would un-erase the subject); implementations record the
     * drop and refuse re-creation.
     */
    byte[] getOrCreate(String subjectId);

    /**
     * Irreversibly removes the subject's key. This is the erasure act. Idempotent: dropping an
     * already-dropped or never-existing subject is a no-op (still records the tombstone).
     */
    void drop(String subjectId);

    /** True while the subject's key is present (i.e. before erasure). */
    boolean exists(String subjectId);
}
