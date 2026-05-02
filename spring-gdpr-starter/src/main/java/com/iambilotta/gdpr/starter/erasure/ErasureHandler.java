package com.iambilotta.gdpr.starter.erasure;

import com.iambilotta.gdpr.annotations.GdprErasable;

/**
 * Per-entity contract that knows how to erase, anonymize, or pseudonymize the records
 * tied to a given subject id.
 *
 * <p>Spring discovers all beans of this type and invokes them in {@link #order()} ascending,
 * so handlers can declare their FK-safe ordering.
 */
public interface ErasureHandler {

    /**
     * The domain type this handler erases for. Used as the key under
     * {@code affectedByType} in the {@code DELETE /gdpr/erasure/{subjectId}}
     * response and in the audit row {@code targetType} field.
     *
     * <p>Returning {@link Class} instead of a free-form FQN string keeps the
     * coupling to the type system: a typo or a renamed class fails at compile
     * time, not at audit-export time.
     */
    Class<?> entityType();

    GdprErasable.Strategy strategy();

    /**
     * Run the strategy and return the number of records affected.
     */
    int erase(String subjectId);

    default int order() {
        return 100;
    }
}
