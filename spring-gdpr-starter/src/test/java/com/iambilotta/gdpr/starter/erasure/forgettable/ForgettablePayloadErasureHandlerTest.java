package com.iambilotta.gdpr.starter.erasure.forgettable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.iambilotta.gdpr.annotations.GdprErasable;
import com.iambilotta.gdpr.starter.audit.ActorResolver;
import com.iambilotta.gdpr.starter.audit.AuditAccessRecord;
import com.iambilotta.gdpr.starter.audit.AuditSink;

/**
 * Specification for the PRIMARY personal-data erasure path (ADR-0010): erasure as an actual
 * {@code DELETE} of the external PII store, recorded as an audit fact. This is the forgettable-
 * payload analogue of {@code CryptoShreddingErasureTest}, but the personal data is genuinely
 * removed (anonymisation), not merely rendered unreadable (pseudonymisation).
 */
class ForgettablePayloadErasureHandlerTest {

    /** A minimal carrier (a row or an immutable event) that holds only a reference, never the PII. */
    record CustomerSnapshot(String subjectId, ForgettablePayloadReference fullName) {
    }

    /**
     * @spec.given a carrier that holds only a reference to an externalised PII field
     * @spec.when  the value is resolved before erasure, then the subject is erased
     * @spec.then  the value is gone (resolve empty) while the carrier's reference is unchanged
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void erasureDeletesThePiiWhileTheCarrierKeepsOnlyADanglingReference() {
        InMemoryForgettablePayloadStore store = new InMemoryForgettablePayloadStore();
        ForgettablePayloadResolver resolver = new ForgettablePayloadResolver(store);
        ForgettablePayloadErasureHandler erasure = new ForgettablePayloadErasureHandler(
                store, new CapturingAuditSink(), ActorResolver.fixed("dpo-jane"), CustomerSnapshot.class);

        ForgettablePayloadReference ref = ForgettablePayloadReference.of("alice-1", "full_name");
        store.put(ref.subjectId(), ref.fieldKey(), "Alice Liddell");
        CustomerSnapshot snapshot = new CustomerSnapshot("alice-1", ref);

        assertThat(resolver.resolve(snapshot.fullName())).contains("Alice Liddell");

        erasure.erase("alice-1");

        assertThat(snapshot.fullName()).isEqualTo(ref); // the carrier is untouched
        assertThat(resolver.resolve(snapshot.fullName())).isEmpty(); // ... but the PII is actually gone
    }

    /**
     * @spec.given a subject with two externalised fields
     * @spec.when  the erasure handler runs
     * @spec.then  it reports the number of deleted value rows and uses the DELETE strategy
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void reportsDeletedRowCountAndDeleteStrategy() {
        InMemoryForgettablePayloadStore store = new InMemoryForgettablePayloadStore();
        ForgettablePayloadErasureHandler erasure = new ForgettablePayloadErasureHandler(
                store, new CapturingAuditSink(), ActorResolver.fixed("dpo-jane"), CustomerSnapshot.class);
        store.put("alice-1", "full_name", "Alice");
        store.put("alice-1", "email", "alice@example.com");

        int affected = erasure.erase("alice-1");

        assertThat(affected).isEqualTo(2);
        assertThat(erasure.strategy()).isEqualTo(GdprErasable.Strategy.DELETE);
        assertThat(erasure.entityType()).isEqualTo(CustomerSnapshot.class);
    }

    /**
     * @spec.given an erasure of a subject
     * @spec.when  the handler runs
     * @spec.then  an audit record (action ERASURE, basis Art.17, actor) is written: a recorded fact
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void recordsTheErasureAsAnAuditFact() {
        InMemoryForgettablePayloadStore store = new InMemoryForgettablePayloadStore();
        CapturingAuditSink audit = new CapturingAuditSink();
        ForgettablePayloadErasureHandler erasure = new ForgettablePayloadErasureHandler(
                store, audit, ActorResolver.fixed("dpo-jane"), CustomerSnapshot.class);
        store.put("alice-1", "full_name", "Alice");

        erasure.erase("alice-1");

        assertThat(audit.records).hasSize(1);
        AuditAccessRecord fact = audit.records.get(0);
        assertThat(fact.targetMember()).isEqualTo(ForgettablePayloadErasureHandler.ERASURE_ACTION);
        assertThat(fact.legalBasis()).isEqualTo(ForgettablePayloadErasureHandler.ERASURE_LEGAL_BASIS);
        assertThat(fact.subjectId()).isEqualTo("alice-1");
        assertThat(fact.actor()).isEqualTo("dpo-jane");
        assertThat(fact.at()).isNotNull();
    }

    /**
     * @spec.given a subject erased via the handler
     * @spec.when  a put is attempted again for that subject
     * @spec.then  the store refuses it (no silent resurrection through the erasure path)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void erasedSubjectStaysErased() {
        InMemoryForgettablePayloadStore store = new InMemoryForgettablePayloadStore();
        ForgettablePayloadErasureHandler erasure = new ForgettablePayloadErasureHandler(
                store, new CapturingAuditSink(), ActorResolver.fixed("dpo-jane"), CustomerSnapshot.class);
        store.put("alice-1", "full_name", "Alice");

        erasure.erase("alice-1");

        assertThatThrownBy(() -> store.put("alice-1", "full_name", "Alice again"))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * @spec.given a blank subject id
     * @spec.when  the handler is asked to erase
     * @spec.then  it is rejected (a blank id is never a real erasure target)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void rejectsBlankSubjectId() {
        InMemoryForgettablePayloadStore store = new InMemoryForgettablePayloadStore();
        ForgettablePayloadErasureHandler erasure = new ForgettablePayloadErasureHandler(
                store, new CapturingAuditSink(), ActorResolver.fixed("dpo-jane"), CustomerSnapshot.class);

        assertThatThrownBy(() -> erasure.erase("  "))
                .isInstanceOf(IllegalArgumentException.class);
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
