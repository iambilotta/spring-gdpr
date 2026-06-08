package com.iambilotta.gdpr.starter.erasure.crypto;

import java.time.Instant;
import java.util.UUID;

import com.iambilotta.gdpr.annotations.GdprErasable;
import com.iambilotta.gdpr.starter.audit.ActorResolver;
import com.iambilotta.gdpr.starter.audit.AuditAccessRecord;
import com.iambilotta.gdpr.starter.audit.AuditSink;
import com.iambilotta.gdpr.starter.erasure.ErasureHandler;

/**
 * {@link ErasureHandler} for an append-only event-sourced domain (ADR-0009). Erasure here is NOT a
 * {@code DELETE} of rows: it is {@link SubjectKeyStore#drop(String) dropping the subject's key},
 * after which every encrypted PII field in the immutable event log is permanently unrecoverable
 * while no event is touched.
 *
 * <p>The erasure is a <strong>recorded fact</strong>: before/after dropping the key this handler
 * writes an {@link AuditAccessRecord} (action {@code ERASURE}, legal basis Art. 17) to the
 * {@link AuditSink}. The audit trail is append-only and survives the erasure, proving it happened
 * even though no event changed.
 *
 * <p>Reported strategy is {@link GdprErasable.Strategy#DELETE}: from the subject's point of view
 * the personal data is gone (the strongest of the three), even though the bytes physically remain
 * as unreadable ciphertext.
 */
public final class CryptoShreddingErasureHandler implements ErasureHandler {

    /** Marker used in the audit record's {@code targetMember} to denote the erasure action. */
    public static final String ERASURE_ACTION = "ERASURE";

    /** Art. 17 right to erasure: the legal basis stamped on the audit fact. */
    public static final String ERASURE_LEGAL_BASIS = "17";

    private final SubjectKeyStore keyStore;
    private final AuditSink auditSink;
    private final ActorResolver actorResolver;
    private final Class<?> entityType;
    private final int order;

    public CryptoShreddingErasureHandler(
            SubjectKeyStore keyStore,
            AuditSink auditSink,
            ActorResolver actorResolver,
            Class<?> entityType) {
        this(keyStore, auditSink, actorResolver, entityType, 100);
    }

    public CryptoShreddingErasureHandler(
            SubjectKeyStore keyStore,
            AuditSink auditSink,
            ActorResolver actorResolver,
            Class<?> entityType,
            int order) {
        this.keyStore = keyStore;
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
     * Drops the subject's key (the erasure) and records the fact. Returns 1 when a live key was
     * dropped, 0 when the subject had no key (already erased or never present); the audit fact is
     * written either way so the request itself is logged.
     */
    @Override
    public int erase(String subjectId) {
        if (subjectId == null || subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId must be provided");
        }
        boolean hadKey = keyStore.exists(subjectId);
        keyStore.drop(subjectId);
        recordErasureFact(subjectId);
        return hadKey ? 1 : 0;
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
