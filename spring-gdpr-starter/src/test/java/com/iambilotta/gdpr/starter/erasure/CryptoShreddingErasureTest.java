package com.iambilotta.gdpr.starter.erasure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.iambilotta.gdpr.annotations.GdprErasable;
import com.iambilotta.gdpr.starter.audit.ActorResolver;
import com.iambilotta.gdpr.starter.audit.AuditAccessRecord;
import com.iambilotta.gdpr.starter.audit.AuditSink;
import com.iambilotta.gdpr.starter.erasure.crypto.AesGcmCryptoShredder;
import com.iambilotta.gdpr.starter.erasure.crypto.CryptoShredder;
import com.iambilotta.gdpr.starter.erasure.crypto.CryptoShreddingErasureHandler;
import com.iambilotta.gdpr.starter.erasure.crypto.InMemorySubjectKeyStore;
import com.iambilotta.gdpr.starter.erasure.crypto.SubjectKeyStore;

/**
 * Specification for REQ-GDPR-016 / REQ-GDPR-017 (append-only-safe erasure via crypto-shredding,
 * ADR-0009). These tests drive the event-sourcing erasure module the library now ships.
 *
 * <p><strong>ONE-WAY DOOR.</strong> The strategy (crypto-shredding: per-subject key, erasure =
 * drop the key) touches the append-only event format and is irreversible once a store is
 * populated. ADR-0009 was approved (proposed -> accepted) before this implementation; the tests
 * are no longer {@code @Disabled}.
 *
 * <p>The mechanism is store-agnostic: spring-gdpr is not itself event-sourced. The harness below
 * is a tiny in-test "event store" ({@link EncryptedEvent} + an {@link InMemorySubjectKeyStore})
 * that an event-sourced adopter would replace with its own serializer + key store.
 */
class CryptoShreddingErasureTest {

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
    void droppingTheKeyRendersThePiiUnrecoverableWhileTheEventStaysImmutable() {
        SubjectKeyStore keys = keyStore();
        CryptoShredder shredder = cryptoShredder(keys);

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
    void erasureIsPerSubjectAndDoesNotAffectOtherSubjects() {
        SubjectKeyStore keys = keyStore();
        CryptoShredder shredder = cryptoShredder(keys);

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
    void replayAfterErasureIsIdempotentAndExposesNoPii() {
        SubjectKeyStore keys = keyStore();
        CryptoShredder shredder = cryptoShredder(keys);

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
    void erasurePreservesTheAuditTrailAsARecordedFact() {
        SubjectKeyStore keys = keyStore();
        CapturingAuditSink audit = new CapturingAuditSink();
        CryptoShreddingErasureHandler erasure = new CryptoShreddingErasureHandler(
                keys, audit, ActorResolver.fixed("dpo-jane"), EncryptedEvent.class);

        keys.getOrCreate("alice-1");
        assertThat(keys.exists("alice-1")).isTrue();

        // The erasure orchestration drops the key and writes an audit row; the audit trail is
        // append-only and survives the erasure (the erasure is itself a recorded fact).
        int affected = erasure.erase("alice-1");

        assertThat(affected).isEqualTo(1);
        assertThat(keys.exists("alice-1")).isFalse();
        assertThat(erasure.strategy()).isEqualTo(GdprErasable.Strategy.DELETE);

        // an AuditAccessRecord with action=ERASURE was emitted (who/when/why preserved).
        assertThat(audit.records).hasSize(1);
        AuditAccessRecord fact = audit.records.get(0);
        assertThat(fact.targetMember()).isEqualTo(CryptoShreddingErasureHandler.ERASURE_ACTION);
        assertThat(fact.legalBasis()).isEqualTo(CryptoShreddingErasureHandler.ERASURE_LEGAL_BASIS);
        assertThat(fact.subjectId()).isEqualTo("alice-1");
        assertThat(fact.actor()).isEqualTo("dpo-jane");
        assertThat(fact.at()).isNotNull();
    }

    // --- harness wiring: the real crypto-shredding module backing the in-test event store. ---

    private static SubjectKeyStore keyStore() {
        return new InMemorySubjectKeyStore();
    }

    private static CryptoShredder cryptoShredder(SubjectKeyStore keys) {
        return new AesGcmCryptoShredder(keys);
    }

    private static String replayName(CryptoShredder shredder, List<EncryptedEvent> stream) {
        StringBuilder readModel = new StringBuilder();
        for (EncryptedEvent event : stream) {
            shredder.decrypt(event.subjectId(), event.encryptedPayload()).ifPresent(readModel::append);
        }
        return readModel.toString();
    }

    /** Captures emitted audit facts so the test can assert the erasure was recorded. */
    private static final class CapturingAuditSink implements AuditSink {
        private final List<AuditAccessRecord> records = new ArrayList<>();

        @Override
        public void write(AuditAccessRecord record) {
            records.add(record);
        }

        @Override
        public List<AuditAccessRecord> findBySubject(String subjectId, Instant from, Instant to) {
            return records.stream().filter(r -> subjectId.equals(r.subjectId())).toList();
        }
    }
}
