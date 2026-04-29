package com.iambilotta.gdpr.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import com.iambilotta.gdpr.annotations.GdprDataSubjects;
import com.iambilotta.gdpr.annotations.GdprErasable;
import com.iambilotta.gdpr.annotations.GdprLegalBasis;
import com.iambilotta.gdpr.annotations.GdprPersonalData;
import com.iambilotta.gdpr.annotations.GdprRetention;

/**
 * Build-time generator. Walks every annotated type and emits two artifacts under
 * {@code target/generated-sources/annotations/spring/gdpr/}:
 * <ul>
 *   <li>{@code dpia.md}, scaffold for Data Protection Impact Assessment Art. 35</li>
 *   <li>{@code ropa.csv}, Records of Processing Activities Art. 30</li>
 * </ul>
 *
 * <p>Two distinct concerns are tracked:
 * <ul>
 *   <li><b>Records of processing activities</b> (entity types carrying type-level annotations
 *       like {@code @GdprDataSubjects}, {@code @GdprLegalBasis}, {@code @GdprRetention},
 *       {@code @GdprErasable}). These are the rows that ROPA Art. 30 demands.</li>
 *   <li><b>Access points</b> (methods or fields with {@code @GdprPersonalData} on a type that
 *       has no record-level annotations). These are listed separately as a code-map of
 *       where personal data is touched, useful for audit but not a ROPA row by themselves.</li>
 * </ul>
 *
 * <p>Output is deterministic (sorted by FQN) and committed-friendly. The Maven plugin
 * {@code spring-gdpr-maven-plugin} wraps this and exposes CLI goals.
 */
@SupportedAnnotationTypes({
        "com.iambilotta.gdpr.annotations.GdprPersonalData",
        "com.iambilotta.gdpr.annotations.GdprDataSubjects",
        "com.iambilotta.gdpr.annotations.GdprLegalBasis",
        "com.iambilotta.gdpr.annotations.GdprRetention",
        "com.iambilotta.gdpr.annotations.GdprErasable"
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class GdprAnnotationProcessor extends AbstractProcessor {

    private final Map<String, ProcessingRecord> recordsByType = new LinkedHashMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(GdprDataSubjects.class)) {
            collect(element).withSubjects(element.getAnnotation(GdprDataSubjects.class));
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(GdprLegalBasis.class)) {
            collect(element).withLegalBasis(element.getAnnotation(GdprLegalBasis.class));
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(GdprRetention.class)) {
            collect(element).withRetention(element.getAnnotation(GdprRetention.class));
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(GdprErasable.class)) {
            collect(element).withErasable(element.getAnnotation(GdprErasable.class));
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(GdprPersonalData.class)) {
            ProcessingRecord record = collect(typeOwner(element));
            record.touchedPersonalData(element);
            if (element.getAnnotation(GdprPersonalData.class).specialCategory()) {
                record.markSpecialCategory();
            }
        }

        if (roundEnv.processingOver() && !recordsByType.isEmpty()) {
            try {
                writeRopa();
                writeDpia();
                emitMissingDiagnostics();
            } catch (IOException ex) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR, "spring-gdpr: failed to write artifacts: " + ex.getMessage());
            }
        }
        return false;
    }

    private ProcessingRecord collect(Element element) {
        Element owner = typeOwner(element);
        String fqn = owner.toString();
        return recordsByType.computeIfAbsent(fqn, ProcessingRecord::new);
    }

    private static Element typeOwner(Element element) {
        Element current = element;
        while (current != null && !(current instanceof TypeElement)) {
            current = current.getEnclosingElement();
        }
        return current != null ? current : element;
    }

    private List<ProcessingRecord> sorted() {
        List<ProcessingRecord> all = new ArrayList<>(recordsByType.values());
        all.sort((a, b) -> a.entity.compareTo(b.entity));
        return all;
    }

    private List<ProcessingRecord> ropaRecords() {
        return sorted().stream().filter(ProcessingRecord::isRopaRecord).toList();
    }

    private List<ProcessingRecord> accessPoints() {
        return sorted().stream().filter(r -> !r.isRopaRecord() && r.touchedPersonalData).toList();
    }

    private void writeRopa() throws IOException {
        Filer filer = processingEnv.getFiler();
        FileObject file = filer.createResource(
                StandardLocation.SOURCE_OUTPUT, "spring.gdpr", "ropa.csv");
        try (Writer writer = file.openWriter()) {
            writer.write("entity,data_subjects,legal_basis,retention_period,strategy,special_category\n");
            for (ProcessingRecord record : ropaRecords()) {
                writer.write(record.toCsvRow());
                writer.write('\n');
            }
        }
    }

    private void writeDpia() throws IOException {
        Filer filer = processingEnv.getFiler();
        FileObject file = filer.createResource(
                StandardLocation.SOURCE_OUTPUT, "spring.gdpr", "dpia.md");
        try (Writer writer = file.openWriter()) {
            writer.write("# Data Protection Impact Assessment (Art. 35) Scaffold\n\n");
            writer.write("Generated by spring-gdpr-processor. Review every section before submission.\n\n");

            writer.write("## 1. Records of processing activities (Art. 30)\n\n");
            List<ProcessingRecord> ropa = ropaRecords();
            if (ropa.isEmpty()) {
                writer.write("(No type carries record-level annotations. Add `@GdprDataSubjects` + `@GdprLegalBasis` + `@GdprRetention` to your domain entities.)\n\n");
            } else {
                writer.write("| Entity | Data subjects | Legal basis | Retention | Strategy | Special category |\n");
                writer.write("|---|---|---|---|---|---|\n");
                for (ProcessingRecord record : ropa) {
                    writer.write(record.toMarkdownRow());
                    writer.write('\n');
                }
                writer.write('\n');
            }

            writer.write("## 2. Personal-data access points\n\n");
            List<ProcessingRecord> access = accessPoints();
            if (access.isEmpty()) {
                writer.write("(None.)\n\n");
            } else {
                writer.write("Methods or fields annotated with `@GdprPersonalData` on types that do not themselves carry record-level annotations. Each access fires the audit advisor at runtime.\n\n");
                writer.write("| Type | Member |\n");
                writer.write("|---|---|\n");
                for (ProcessingRecord record : access) {
                    for (String member : record.touchedMembers) {
                        writer.write("| " + record.entity + " | " + member + " |\n");
                    }
                }
                writer.write('\n');
            }

            writer.write("## 3. Necessity and proportionality assessment\n\n");
            writer.write("(Fill in. Why each processing op is necessary for the declared purpose.)\n\n");
            writer.write("## 4. Risks to rights and freedoms of data subjects\n\n");
            writer.write("(Fill in. Likelihood + severity per risk scenario. Special-category rows above carry elevated risk.)\n\n");
            writer.write("## 5. Mitigation measures\n\n");
            writer.write("(Fill in. Encryption-at-rest, pseudonymization, access control, retention enforcement, etc.)\n\n");
            writer.write("## 6. DPO consultation\n\n");
            writer.write("(Fill in. Date, name, recommendation.)\n");
        }
    }

    /**
     * Emits compiler warnings for ROPA rows that are missing one of the mandatory Art. 30 fields,
     * so the user sees them in the IDE / build log without needing to read the CSV.
     */
    private void emitMissingDiagnostics() {
        for (ProcessingRecord record : ropaRecords()) {
            if (record.legalBasis.isEmpty()) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.WARNING,
                        "spring-gdpr: " + record.entity + " is a ROPA record but is missing @GdprLegalBasis (Art. 6).");
            }
            if (record.retentionPeriod.isEmpty()) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.WARNING,
                        "spring-gdpr: " + record.entity + " is a ROPA record but is missing @GdprRetention (Art. 5(1)(e)).");
            }
        }
    }

    /**
     * Internal aggregate. Mutable during the {@code process()} round, frozen at write time.
     */
    static final class ProcessingRecord {
        private final String entity;
        private final List<String> touchedMembers = new ArrayList<>();
        private String[] subjects = new String[0];
        private String legalBasis = "";
        private String retentionPeriod = "";
        private String strategy = "";
        private boolean specialCategory;
        private boolean touchedPersonalData;
        private boolean hasTypeLevelAnnotation;

        ProcessingRecord(String entity) {
            this.entity = entity;
        }

        void withSubjects(GdprDataSubjects ann) {
            this.subjects = ann.categories();
            this.hasTypeLevelAnnotation = true;
        }

        void withLegalBasis(GdprLegalBasis ann) {
            String article = ann.article() != null && !ann.article().isBlank() ? ann.article()
                    : "6(1) " + ann.value().name();
            this.legalBasis = article;
            this.hasTypeLevelAnnotation = true;
        }

        void withRetention(GdprRetention ann) {
            this.retentionPeriod = ann.period();
            this.strategy = ann.strategy().name();
            this.hasTypeLevelAnnotation = true;
        }

        void withErasable(GdprErasable ann) {
            if (this.strategy.isEmpty()) {
                this.strategy = ann.strategy().name();
            }
            this.hasTypeLevelAnnotation = true;
        }

        void touchedPersonalData(Element annotated) {
            this.touchedPersonalData = true;
            String memberName = annotated.getSimpleName().toString();
            if (!memberName.isEmpty() && !touchedMembers.contains(memberName)) {
                touchedMembers.add(memberName);
            }
        }

        void markSpecialCategory() {
            this.specialCategory = true;
        }

        boolean isRopaRecord() {
            return hasTypeLevelAnnotation;
        }

        String toCsvRow() {
            return String.join(",",
                    csv(entity),
                    csv(String.join("|", subjects)),
                    csv(legalBasis),
                    csv(retentionPeriod),
                    csv(strategy),
                    Boolean.toString(specialCategory));
        }

        String toMarkdownRow() {
            return "| " + entity + " | " + String.join(", ", subjects)
                    + " | " + (legalBasis.isEmpty() ? "MISSING" : legalBasis)
                    + " | " + (retentionPeriod.isEmpty() ? "MISSING" : retentionPeriod)
                    + " | " + (strategy.isEmpty() ? "MISSING" : strategy)
                    + " | " + (specialCategory ? "yes" : "no") + " |";
        }

        private static String csv(String value) {
            if (value == null) {
                return "";
            }
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                return "\"" + value.replace("\"", "\"\"") + "\"";
            }
            return value;
        }
    }
}
