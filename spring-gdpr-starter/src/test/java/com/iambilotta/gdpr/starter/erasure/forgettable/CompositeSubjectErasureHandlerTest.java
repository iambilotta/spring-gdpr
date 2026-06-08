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
import com.iambilotta.gdpr.starter.erasure.CompositeSubjectErasureHandler;
import com.iambilotta.gdpr.starter.erasure.ErasureHandler;
import com.iambilotta.gdpr.starter.erasure.crypto.AesGcmCryptoShredder;
import com.iambilotta.gdpr.starter.erasure.crypto.CryptoShredder;
import com.iambilotta.gdpr.starter.erasure.crypto.CryptoShreddingErasureHandler;
import com.iambilotta.gdpr.starter.erasure.crypto.InMemorySubjectKeyStore;
import com.iambilotta.gdpr.starter.erasure.crypto.SubjectKeyStore;

/**
 * Pins {@link CompositeSubjectErasureHandler}: a single {@link ErasureHandler} that erases a subject
 * across BOTH mechanisms at once (ADR-0010) — the forgettable-payload external store (primary) AND
 * the crypto-shredding key store (the exception, for immutable events). One erasure request for a
 * subject whose data is split across both must remove all of it, so neither path can be forgotten.
 */
class CompositeSubjectErasureHandlerTest {

    record Customer() {
    }

    /**
     * @spec.given a subject whose PII lives in BOTH the forgettable-payload store and (for an immutable event) a crypto-shredded field
     * @spec.when  the composite erasure handler runs once for that subject
     * @spec.then  the external value is deleted AND the crypto key is dropped (both paths erased)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void erasesBothTheForgettablePayloadAndTheCryptoKey() {
        InMemoryForgettablePayloadStore payloads = new InMemoryForgettablePayloadStore();
        ForgettablePayloadResolver resolver = new ForgettablePayloadResolver(payloads);
        SubjectKeyStore keys = new InMemorySubjectKeyStore();
        CryptoShredder shredder = new AesGcmCryptoShredder(keys);
        CapturingAuditSink audit = new CapturingAuditSink();

        ForgettablePayloadReference nameRef = ForgettablePayloadReference.of("alice-1", "full_name");
        payloads.put(nameRef.subjectId(), nameRef.fieldKey(), "Alice Liddell");
        byte[] eventCiphertext = shredder.encrypt("alice-1", "alice@example.com");

        ErasureHandler composite = new CompositeSubjectErasureHandler(
                Customer.class,
                List.of(
                        new ForgettablePayloadErasureHandler(payloads, audit, ActorResolver.fixed("dpo"), Customer.class),
                        new CryptoShreddingErasureHandler(keys, audit, ActorResolver.fixed("dpo"), Customer.class)));

        composite.erase("alice-1");

        assertThat(resolver.resolve(nameRef)).isEmpty();              // forgettable payload deleted
        assertThat(shredder.decrypt("alice-1", eventCiphertext)).isEmpty(); // crypto key dropped
    }

    /**
     * @spec.given two delegate handlers each affecting one row/key
     * @spec.when  the composite erases a subject
     * @spec.then  it sums the affected counts and reports the DELETE strategy
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void sumsAffectedCountsAcrossDelegates() {
        InMemoryForgettablePayloadStore payloads = new InMemoryForgettablePayloadStore();
        SubjectKeyStore keys = new InMemorySubjectKeyStore();
        CapturingAuditSink audit = new CapturingAuditSink();
        payloads.put("alice-1", "full_name", "Alice");
        keys.getOrCreate("alice-1");

        ErasureHandler composite = new CompositeSubjectErasureHandler(
                Customer.class,
                List.of(
                        new ForgettablePayloadErasureHandler(payloads, audit, ActorResolver.fixed("dpo"), Customer.class),
                        new CryptoShreddingErasureHandler(keys, audit, ActorResolver.fixed("dpo"), Customer.class)));

        int affected = composite.erase("alice-1");

        assertThat(affected).isEqualTo(2); // 1 deleted payload row + 1 dropped key
        assertThat(composite.strategy()).isEqualTo(GdprErasable.Strategy.DELETE);
        assertThat(composite.entityType()).isEqualTo(Customer.class);
    }

    /**
     * @spec.given a composite with no delegates
     * @spec.when  it is constructed
     * @spec.then  construction is rejected (a composite that erases nothing is a silent compliance hole)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void rejectsAnEmptyDelegateList() {
        assertThatThrownBy(() -> new CompositeSubjectErasureHandler(Customer.class, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

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
