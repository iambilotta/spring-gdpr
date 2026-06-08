package com.iambilotta.gdpr.processor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.iambilotta.gdpr.annotations.GdprPersonalData.Category;
import com.iambilotta.gdpr.processor.GdprAnnotationProcessor.ProcessingRecord;

/**
 * Specifies REQ-GDPR-015: the IDENTITY / CONTACT / FINANCIAL category dimension on
 * {@code @GdprPersonalData}, surfaced through the build-time ROPA / DPIA generator.
 * Unit-level against {@link ProcessingRecord} (no JSR-269 round needed).
 */
class ProcessingRecordCategoryTest {

    private static ProcessingRecord ropaRecord() {
        ProcessingRecord record = new ProcessingRecord("com.example.Customer");
        // a type-level annotation makes it a ROPA row (vs a bare access point)
        record.markTypeLevelAnnotation();
        return record;
    }

    /**
     * @spec.given a ROPA record that touched an IDENTITY field and a CONTACT field
     * @spec.when  the CSV row is rendered
     * @spec.then  the categories column lists the distinct categories, sorted and pipe-joined
     * @spec.us    REQ-GDPR-015
     */
    @Test
    void csvRowListsTheDistinctTouchedCategoriesSortedAndPipeJoined() {
        ProcessingRecord record = ropaRecord();
        record.touchedPersonalData("fullName", Category.IDENTITY);
        record.touchedPersonalData("email", Category.CONTACT);

        // header order: entity,data_subjects,legal_basis,retention_period,strategy,categories,special_category
        assertThat(record.toCsvRow()).contains(",CONTACT|IDENTITY,");
    }

    /**
     * @spec.given a ROPA record that touched a FINANCIAL field
     * @spec.when  the markdown row is rendered
     * @spec.then  the category appears in the row
     * @spec.us    REQ-GDPR-015
     */
    @Test
    void markdownRowSurfacesTheCategory() {
        ProcessingRecord record = ropaRecord();
        record.touchedPersonalData("iban", Category.FINANCIAL);

        assertThat(record.toMarkdownRow()).contains("FINANCIAL");
    }

    /**
     * @spec.given a ROPA record whose personal-data fields declared no category
     * @spec.when  the CSV row is rendered
     * @spec.then  the categories column is empty (backward-compatible: no category is not an error)
     * @spec.us    REQ-GDPR-015
     */
    @Test
    void uncategorisedFieldsLeaveTheCategoriesColumnEmpty() {
        ProcessingRecord record = ropaRecord();
        record.touchedPersonalData("legacyField", Category.UNCATEGORISED);

        // entity,data_subjects(empty),legal_basis(empty),retention(empty),strategy(empty),categories(empty),special_category
        assertThat(record.toCsvRow()).isEqualTo("com.example.Customer,,,,,,false");
    }
}
