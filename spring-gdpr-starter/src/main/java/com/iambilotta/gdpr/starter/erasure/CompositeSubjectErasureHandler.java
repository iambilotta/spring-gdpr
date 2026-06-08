package com.iambilotta.gdpr.starter.erasure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.iambilotta.gdpr.annotations.GdprErasable;

/**
 * An {@link ErasureHandler} that erases a subject across several delegate handlers as a single unit,
 * summing their affected counts. The motivating case (ADR-0010): a subject whose personal data is
 * split between the <strong>forgettable-payload external store</strong> (the primary path, mutable
 * carriers) and a <strong>crypto-shredded</strong> field (the exception, an immutable event that
 * must legally carry the value inline). One erasure request must remove BOTH, so neither mechanism
 * is silently forgotten.
 *
 * <p>Delegates run in their own {@link ErasureHandler#order()} ascending, so FK-safe ordering still
 * holds within the composite. Each delegate writes its own audit fact; this composite does not
 * suppress them, so the audit trail records each mechanism's erasure act distinctly.
 *
 * <p>This is generic: any two or more {@link ErasureHandler}s for the same logical subject can be
 * composed, not only the forgettable + crypto pair.
 */
public final class CompositeSubjectErasureHandler implements ErasureHandler {

    private final Class<?> entityType;
    private final List<ErasureHandler> delegates;
    private final int order;

    public CompositeSubjectErasureHandler(Class<?> entityType, List<ErasureHandler> delegates) {
        this(entityType, delegates, 100);
    }

    public CompositeSubjectErasureHandler(Class<?> entityType, List<ErasureHandler> delegates, int order) {
        if (delegates == null || delegates.isEmpty()) {
            throw new IllegalArgumentException("a composite erasure handler needs at least one delegate");
        }
        List<ErasureHandler> sorted = new ArrayList<>(delegates);
        sorted.sort(Comparator.comparingInt(ErasureHandler::order));
        this.entityType = entityType;
        this.delegates = List.copyOf(sorted);
        this.order = order;
    }

    @Override
    public Class<?> entityType() {
        return entityType;
    }

    /**
     * {@link GdprErasable.Strategy#DELETE}: from the subject's side the personal data is gone across
     * every delegate (the forgettable rows are deleted, the crypto key is dropped).
     */
    @Override
    public GdprErasable.Strategy strategy() {
        return GdprErasable.Strategy.DELETE;
    }

    /** Runs every delegate in order and returns the sum of their affected counts. */
    @Override
    public int erase(String subjectId) {
        if (subjectId == null || subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId must be provided");
        }
        int total = 0;
        for (ErasureHandler delegate : delegates) {
            total += delegate.erase(subjectId);
        }
        return total;
    }

    @Override
    public int order() {
        return order;
    }
}
