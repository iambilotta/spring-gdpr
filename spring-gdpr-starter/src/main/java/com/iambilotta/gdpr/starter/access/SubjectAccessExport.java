package com.iambilotta.gdpr.starter.access;

import java.util.List;

/**
 * The Article 15 dossier for one subject: the subject id and every classified field found across
 * the registered {@link SubjectDataProvider} beans. Human-readable by construction (field name +
 * value + description + category); a JSON serialization is the controller's concern.
 */
public record SubjectAccessExport(String subjectId, List<ExportedField> fields) {
}
