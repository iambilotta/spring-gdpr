package com.iambilotta.gdpr.starter.access;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.iambilotta.gdpr.annotations.GdprPersonalData;

/**
 * Specifies REQ-GDPR-019: the Article 15 right-of-access export. The library assembles a
 * subject-facing dossier of the subject's {@code @GdprPersonalData}-classified fields from the
 * objects the adopter's {@code SubjectDataProvider} beans return. Symmetric to the erasure SPI
 * (ADR-0004): the library knows assembly, the application knows where the data lives.
 */
class AccessExportServiceTest {

    static final class Customer {
        @GdprPersonalData(description = "full legal name", category = GdprPersonalData.Category.IDENTITY)
        String fullName;

        @GdprPersonalData(category = GdprPersonalData.Category.CONTACT)
        String email;

        String internalRef;

        Customer(String fullName, String email, String internalRef) {
            this.fullName = fullName;
            this.email = email;
            this.internalRef = internalRef;
        }
    }

    private static SubjectDataProvider providerReturning(Object... objects) {
        return subjectId -> List.of(objects);
    }

    /**
     * @spec.given a provider returning a subject's Customer with two classified fields
     * @spec.when  the access export is assembled for that subject
     * @spec.then  the export carries one entry per classified field, with name, value and category
     * @spec.us    REQ-GDPR-019
     */
    @Test
    void exportsTheClassifiedFieldsOfTheSubjectsObjects() {
        AccessExportService service = new AccessExportService(
                List.of(providerReturning(new Customer("Alice", "alice@example.com", "ref-1"))));

        SubjectAccessExport export = service.exportSubject("alice-1");

        assertThat(export.subjectId()).isEqualTo("alice-1");
        assertThat(export.fields())
                .extracting(ExportedField::field)
                .containsExactlyInAnyOrder("fullName", "email");
        assertThat(export.fields())
                .extracting(ExportedField::value)
                .containsExactlyInAnyOrder("Alice", "alice@example.com");
    }

    /**
     * @spec.given any subject's classified field
     * @spec.when  the export is assembled
     * @spec.then  the declared category travels with the field (so the dossier is grouped by category)
     * @spec.us    REQ-GDPR-019
     */
    @Test
    void carriesTheCategoryOfEachExportedField() {
        AccessExportService service = new AccessExportService(
                List.of(providerReturning(new Customer("Alice", "alice@example.com", "ref-1"))));

        SubjectAccessExport export = service.exportSubject("alice-1");

        assertThat(export.fields())
                .filteredOn(f -> f.field().equals("email"))
                .singleElement()
                .extracting(ExportedField::category)
                .isEqualTo(GdprPersonalData.Category.CONTACT);
    }

    /**
     * @spec.given a non-classified field on the subject's object
     * @spec.when  the export is assembled
     * @spec.then  the non-personal field is NOT included (Art. 15 covers personal data only)
     * @spec.us    REQ-GDPR-019
     */
    @Test
    void doesNotExportNonPersonalFields() {
        AccessExportService service = new AccessExportService(
                List.of(providerReturning(new Customer("Alice", "alice@example.com", "ref-1"))));

        SubjectAccessExport export = service.exportSubject("alice-1");

        assertThat(export.fields()).extracting(ExportedField::field).doesNotContain("internalRef");
    }

    /**
     * @spec.given a subject for whom no provider returns any object
     * @spec.when  the export is assembled
     * @spec.then  an empty export is returned for that subject id (not null, not an error)
     * @spec.us    REQ-GDPR-019
     */
    @Test
    void returnsAnEmptyExportForAnUnknownSubject() {
        AccessExportService service = new AccessExportService(List.of(providerReturning()));

        SubjectAccessExport export = service.exportSubject("nobody");

        assertThat(export.subjectId()).isEqualTo("nobody");
        assertThat(export.fields()).isEmpty();
    }
}
