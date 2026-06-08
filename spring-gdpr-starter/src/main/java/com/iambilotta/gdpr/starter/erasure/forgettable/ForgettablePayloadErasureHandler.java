package com.iambilotta.gdpr.starter.erasure.forgettable;

import java.time.Instant;
import java.util.UUID;

import com.iambilotta.gdpr.annotations.GdprErasable;
import com.iambilotta.gdpr.starter.audit.ActorResolver;
import com.iambilotta.gdpr.starter.audit.AuditAccessRecord;
import com.iambilotta.gdpr.starter.audit.AuditSink;
import com.iambilotta.gdpr.starter.erasure.ErasureHandler;

/**
 * The PRIMARY personal-data {@link ErasureHandler} (ADR-0010): erasure is an actual {@code DELETE}
 * of the subject's rows in the external {@link ForgettablePayloadStore}. After this runs, every
 * {@link ForgettablePayloadReference} the domain still carries dangles and resolves to empty, so the
 * personal data is genuinely <em>removed</em>, not merely rendered unreadable.
 *
 * <p>This is preferred over crypto-shredding for personal data because deleting the value is
 * anonymisation, whereas crypto-shredding leaves ciphertext that, in law, is still pseudonymised
 * personal data (Recital 26, EDPB 01/2025). Use {@code CryptoShreddingErasureHandler} only for the
 * narrow case where an immutable event must legally carry the value inline (ADR-0009, ADR-0010).
 *
 * <p>The erasure is a <strong>recorded fact</strong>: this handler writes an
 * {@link AuditAccessRecord} (action {@code ERASURE}, legal basis Art. 17) to the {@link AuditSink}.
 * The audit trail is append-only and survives the deletion, proving it happened.
 *
 * <p>Reported strategy is {@link GdprErasable.Strategy#DELETE}: the personal data is gone.
 */
public final class ForgettablePayloadErasureHandler implements ErasureHandler {

    /** Marker used in the audit record's {@code targetMember} to denote the erasure action. */
    public static final String ERASURE_ACTION = "ERASURE";

    /** Art. 17 right to erasure: the legal basis stamped on the audit fact. */
    public static final String ERASURE_LEGAL_BASIS = "17";

    private final ForgettablePayloadStore store;
    private final AuditSink auditSink;
    private final ActorResolver actorResolver;
    private final Class<?> entityType;
    private final int order;

    public ForgettablePayloadErasureHandler(
            ForgettablePayloadStore store,
            AuditSink auditSink,
            ActorResolver actorResolver,
            Class<?> entityType) {
        this(store, auditSink, actorResolver, entityType, 100);
    }

    public ForgettablePayloadErasureHandler(
            ForgettablePayloadStore store,
            AuditSink auditSink,
            ActorResolver actorResolver,
            Class<?> entityType,
            int order) {
        this.store = store;
        this.auditSink = auditSink;
        this.actorResolver = actorResolver;
        this.entityType = entityType;
        this.order = order;
    }

    @Override
    public Class<?> entityType() {
        return entityType;
    }

    @Override
    public GdprErasable.Strategy strategy() {
        return GdprErasable.Strategy.DELETE;
    }

    /**
     * Deletes the subject's externalised values (the erasure) and records the fact. Returns the
     * number of value rows actually deleted; the audit fact is written even when that is 0, so the
     * request itself is logged.
     */
    @Override
    public int erase(String subjectId) {
        if (subjectId == null || subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId must be provided");
        }
        int deleted = store.erase(subjectId);
        recordErasureFact(subjectId);
        return deleted;
    }

    private void recordErasureFact(String subjectId) {
        auditSink.write(new AuditAccessRecord(
                UUID.randomUUID().toString(),
                Instant.now(),
                actorResolver.currentActor(),
                subjectId,
                entityType.getName(),
                ERASURE_ACTION,
                ERASURE_LEGAL_BASIS,
                false));
    }

    @Override
    public int order() {
        return order;
    }
}
