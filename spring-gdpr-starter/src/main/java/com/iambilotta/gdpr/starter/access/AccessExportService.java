package com.iambilotta.gdpr.starter.access;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.iambilotta.gdpr.annotations.GdprPersonalData;

/**
 * Assembles the Article 15 access export for a subject by asking every registered
 * {@link SubjectDataProvider} for the subject's objects, then reflecting each object for its
 * {@code @GdprPersonalData} fields. The library does the assembly; the providers do the lookup
 * (symmetric to the erasure orchestration, ADR-0004).
 *
 * <p>The library cannot guarantee the export is complete: completeness depends on the adopter
 * registering a provider for every store that holds the subject's personal data. The export lists
 * exactly what the registered providers returned, never more (same honesty contract as erasure).
 */
public class AccessExportService {

    private final List<SubjectDataProvider> providers;

    public AccessExportService(List<SubjectDataProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    public SubjectAccessExport exportSubject(String subjectId) {
        if (subjectId == null || subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId must not be blank");
        }
        List<ExportedField> fields = new ArrayList<>();
        for (SubjectDataProvider provider : providers) {
            List<?> objects = provider.dataFor(subjectId);
            if (objects == null) {
                continue;
            }
            for (Object object : objects) {
                if (object != null) {
                    collectFields(object, fields);
                }
            }
        }
        return new SubjectAccessExport(subjectId, List.copyOf(fields));
    }

    private static void collectFields(Object object, List<ExportedField> sink) {
        Class<?> type = object.getClass();
        for (Field field : type.getDeclaredFields()) {
            GdprPersonalData annotation = field.getAnnotation(GdprPersonalData.class);
            if (annotation == null || field.isSynthetic()) {
                continue;
            }
            String value;
            try {
                field.setAccessible(true);
                value = String.valueOf(field.get(object));
            } catch (ReflectiveOperationException | RuntimeException ex) {
                // The field is classified but unreadable; record its presence without a value
                // rather than dropping it (the subject is entitled to know the field exists).
                value = "<unreadable>";
            }
            sink.add(new ExportedField(
                    type.getName(),
                    field.getName(),
                    value,
                    annotation.description(),
                    annotation.category()));
        }
    }
}
