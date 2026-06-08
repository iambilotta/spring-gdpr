package com.iambilotta.gdpr.starter.access;

import java.util.List;

/**
 * Per-source contract that knows how to fetch the objects holding a given subject's personal data
 * (Art. 15 right of access). Symmetric to {@code ErasureHandler} (ADR-0004): the library assembles
 * the dossier from the {@code @GdprPersonalData} fields of whatever objects you return, but only the
 * application knows where the subject's data lives (which table, which external system).
 *
 * <p>Spring discovers all beans of this type; {@code AccessExportService} calls each and flattens
 * the classified fields of every returned object into the export.
 */
@FunctionalInterface
public interface SubjectDataProvider {

    /**
     * Returns every object that carries personal data for {@code subjectId}. Each object is reflected
     * for {@code @GdprPersonalData} fields. Return an empty list when this source holds nothing for
     * the subject; never return {@code null}.
     */
    List<?> dataFor(String subjectId);
}
