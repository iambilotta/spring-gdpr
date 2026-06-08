package com.iambilotta.gdpr.starter.erasure.crypto;

import java.util.Optional;

/**
 * Encrypts a PII field for a subject on write, decrypts it on read while the subject's key still
 * exists. The crypto half of crypto-shredding (ADR-0009): the produced ciphertext is what an
 * event stores, byte-immutable; once the subject's key is dropped, {@link #decrypt} returns empty
 * forever, even though the stored bytes are unchanged.
 *
 * <p>Store-agnostic by design: this is a field-level encrypt/decrypt primitive, not an
 * event-store integration. spring-gdpr is not itself event-sourced; an event-sourced adopter
 * (e.g. housetree {@code gest}) wires this into its serializer / upcaster so the encrypted field
 * stays opaque to schema evolution.
 */
public interface CryptoShredder {

    /**
     * Encrypts {@code plaintext} under the subject's key (minting the key on first use). The
     * returned bytes are the self-describing ciphertext stored in the event: they carry the IV and
     * the GCM authentication tag, so {@link #decrypt} needs only the subject's key to reverse them.
     */
    byte[] encrypt(String subjectId, String plaintext);

    /**
     * Decrypts a stored ciphertext. Returns empty once the subject's key has been dropped: the
     * byte-immutable event still holds the ciphertext, but it is no longer recoverable. Fails
     * closed: a tampered or malformed ciphertext (failed GCM tag) also yields empty, never a
     * partial plaintext and never an exception that leaks the cause.
     */
    Optional<String> decrypt(String subjectId, byte[] ciphertext);
}
