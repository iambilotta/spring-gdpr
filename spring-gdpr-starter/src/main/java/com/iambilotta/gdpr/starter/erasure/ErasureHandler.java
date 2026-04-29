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

    String entityType();

    GdprErasable.Strategy strategy();

    /**
     * Run the strategy and return the number of records affected.
     */
    int erase(String subjectId);

    default int order() {
        return 100;
    }
}
