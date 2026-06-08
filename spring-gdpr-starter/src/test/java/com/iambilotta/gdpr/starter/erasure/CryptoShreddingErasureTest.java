package com.iambilotta.gdpr.starter.erasure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * RED specification for REQ-GDPR-016 / REQ-GDPR-017 (append-only-safe erasure via
 * crypto-shredding, ADR-0009). These tests define the desired behavior of an event-sourcing
 * erasure module the library does NOT yet ship.
 *
 * <p><strong>ONE-WAY DOOR.</strong> The strategy (crypto-shredding: per-subject key, erasure =
 * drop the key) touches the append-only event format and is irreversible once a store is
 * populated. Implementation is therefore gated on human GREEN approval: every test here is
 * {@link Disabled @Disabled("pending human GREEN approval, ADR-0009")}. Do not implement the
 * production code (the {@code SubjectKey*} / {@code CryptoShredder} types declared below as the
 * intended contract) until ADR-0009 flips from {@code proposed} to {@code accepted}.
 *
 * <p>The nested interfaces below are the contract surface this module will expose. They live in
 * the test on purpose: the file must compile to document the spec, but no production type
 * implements them yet (that is the GREEN that needs approval).
 */
class CryptoShreddingErasureTest {

    /** Per-subject symmetric key store. Erasure = {@link #drop(String)} a subject's key. */
    interface SubjectKeyStore {
        /** Returns the subject's key, minting one on first use; empty after the key was dropped. */
        Optional<byte[]> keyFor(String subjectId);

        /** Mints (if absent) and returns the subject's key. */
        byte[] getOrCreate(String subjectId);

        /** Irreversibly removes the subject's key. This is the erasure act. */
        void drop(String subjectId);

        boolean exists(String subjectId);
    }

    /** Encrypts a PII field for a subject on write, decrypts on read while the key exists. */
    interface CryptoShredder {
        /** Encrypts {@code plaintext} under the subject's key; the result is what the event stores. */
        byte[] encrypt(String subjectId, String plaintext);

        /**
         * Decrypts a stored ciphertext. Returns empty once the subject's key has been dropped:
         * the byte-immutable event still holds the ciphertext, but it is no longer recoverable.
         */
        Optional<String> decrypt(String subjectId, byte[] ciphertext);
    }

    /** A minimal append-only event carrying one encrypted PII field. */
    record EncryptedEvent(String subjectId, byte[] encryptedPayload) {
    }

    /**
     * @spec.given an event whose PII payload was encrypted under a subject's key
     * @spec.when  the subject's key is dropped (the erasure act) and a replay re-reads the event
     * @spec.then  the PII is no longer recoverable from the raw payload, while the event bytes are unchanged
     * @spec.adr   ADR-0009
     * @spec.us    REQ-GDPR-016
     */
    @Test
    @Disabled("pending human GREEN approval, ADR-0009")
    void droppingTheKeyRendersThePiiUnrecoverableWhileTheEventStaysImmutable() {
        CryptoShredder shredder = cryptoShredder();
        SubjectKeyStore keys = keyStore();

        byte[] ciphertext = shredder.encrypt("alice-1", "Alice Liddell");
        EncryptedEvent event = new EncryptedEvent("alice-1", ciphertext);

        assertThat(shredder.decrypt("alice-1", event.encryptedPayload())).contains("Alice Liddell");

        keys.drop("alice-1"); // the Article 17 erasure: a key drop, not an event mutation

        // the event bytes are untouched (append-only preserved) ...
        assertThat(event.encryptedPayload()).isEqualTo(ciphertext);
        // ... but the plaintext is gone forever
        assertThat(shredder.decrypt("alice-1", event.encryptedPayload())).isEmpty();
    }

    /**
     * @spec.given two subjects, each with their own per-subject key
     * @spec.when  one subject is erased (key dropped)
     * @spec.then  the other subject's data is still fully recoverable (per-subject key granularity)
     * @spec.adr   ADR-0009
     * @spec.us    REQ-GDPR-016
     */
    @Test
    @Disabled("pending human GREEN approval, ADR-0009")
    void erasureIsPerSubjectAndDoesNotAffectOtherSubjects() {
        CryptoShredder shredder = cryptoShredder();
        SubjectKeyStore keys = keyStore();

        byte[] alice = shredder.encrypt("alice-1", "Alice");
        byte[] bob = shredder.encrypt("bob-2", "Bob");

        keys.drop("alice-1");

        assertThat(shredder.decrypt("alice-1", alice)).isEmpty();
        assertThat(shredder.decrypt("bob-2", bob)).contains("Bob");
    }

    /**
     * @spec.given a stream of events for a subject whose key was dropped
     * @spec.when  the projection is rebuilt by replaying the stream twice
     * @spec.then  both replays yield the same read model with the PII blank (idempotent under replay)
     * @spec.adr   ADR-0009
     * @spec.us    REQ-GDPR-016
     */
    @Test
    @Disabled("pending human GREEN approval, ADR-0009")
    void replayAfterErasureIsIdempotentAndExposesNoPii() {
        CryptoShredder shredder = cryptoShredder();
        SubjectKeyStore keys = keyStore();

        List<EncryptedEvent> stream = List.of(
                new EncryptedEvent("alice-1", shredder.encrypt("alice-1", "Alice")),
                new EncryptedEvent("alice-1", shredder.encrypt("alice-1", "alice@example.com")));

        keys.drop("alice-1");

        String firstReplay = replayName(shredder, stream);
        String secondReplay = replayName(shredder, stream);

        assertThat(firstReplay).isEqualTo(secondReplay);
        assertThat(firstReplay).doesNotContain("Alice").doesNotContain("alice@example.com");
    }

    /**
     * @spec.given a subject whose key still exists
     * @spec.when  the erasure runs
     * @spec.then  the key is dropped AND an audit record of the erasure is preserved (who/when/why)
     * @spec.adr   ADR-0009
     * @spec.us    REQ-GDPR-016
     */
    @Test
    @Disabled("pending human GREEN approval, ADR-0009")
    void erasurePreservesTheAuditTrailAsARecordedFact() {
        SubjectKeyStore keys = keyStore();
        keys.getOrCreate("alice-1");

        // The erasure orchestration drops the key and writes an audit row; the audit trail is
        // append-only and survives the erasure (the erasure is itself a recorded fact).
        // Asserted here as the contract; wired to AuditSink at GREEN.
        keys.drop("alice-1");

        assertThat(keys.exists("alice-1")).isFalse();
        // TODO(GREEN, ADR-0009): assert an AuditAccessRecord with action=ERASURE was emitted.
    }

    // --- contract stubs: NOT the implementation. They exist so the file compiles; the real
    // crypto-shredding module is the GREEN that needs human approval (ADR-0009). ---

    private static CryptoShredder cryptoShredder() {
        throw new UnsupportedOperationException("crypto-shredding not implemented: ADR-0009 pending GREEN");
    }

    private static SubjectKeyStore keyStore() {
        throw new UnsupportedOperationException("crypto-shredding not implemented: ADR-0009 pending GREEN");
    }

    private static String replayName(CryptoShredder shredder, List<EncryptedEvent> stream) {
        StringBuilder readModel = new StringBuilder();
        for (EncryptedEvent event : stream) {
            shredder.decrypt(event.subjectId(), event.encryptedPayload()).ifPresent(readModel::append);
        }
        return readModel.toString();
    }
}
