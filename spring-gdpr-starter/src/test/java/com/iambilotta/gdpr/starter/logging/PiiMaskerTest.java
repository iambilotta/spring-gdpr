package com.iambilotta.gdpr.starter.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.iambilotta.gdpr.annotations.GdprPersonalData;

/**
 * Specifies REQ-GDPR-018: structured-log PII redaction. The masker takes any object and
 * renders it so that every {@code @GdprPersonalData}-annotated field value is masked, while
 * non-personal fields stay in clear text. This is the unit the Logback converter delegates to.
 */
class PiiMaskerTest {

    private final PiiMasker masker = new PiiMasker();

    static final class Customer {
        @GdprPersonalData(description = "full legal name")
        String fullName = "Alice Liddell";

        @GdprPersonalData(category = GdprPersonalData.Category.CONTACT)
        String email = "alice@example.com";

        String id = "cust-42";
    }

    /**
     * @spec.given an object whose name + email fields carry {@code @GdprPersonalData}
     * @spec.when  the masker renders it
     * @spec.then  neither personal-data value appears in clear text in the output
     * @spec.us    REQ-GDPR-018
     */
    @Test
    void classifiedFieldValuesNeverAppearInClearText() {
        String masked = masker.mask(new Customer());

        assertThat(masked).doesNotContain("Alice Liddell");
        assertThat(masked).doesNotContain("alice@example.com");
    }

    /**
     * @spec.given an object with a non-personal {@code id} field
     * @spec.when  the masker renders it
     * @spec.then  the non-personal value is preserved (only PII is masked, not the whole object)
     * @spec.us    REQ-GDPR-018
     */
    @Test
    void nonPersonalFieldsArePreserved() {
        String masked = masker.mask(new Customer());

        assertThat(masked).contains("cust-42");
        assertThat(masked).contains("fullName");
        assertThat(masked).contains("email");
    }

    /**
     * @spec.given a value that carries no {@code @GdprPersonalData} fields at all
     * @spec.when  the masker is asked whether it must mask it
     * @spec.then  it reports false, so the converter can skip the reflection cost
     * @spec.us    REQ-GDPR-018
     */
    @Test
    void reportsWhenAnObjectCarriesNoPersonalData() {
        assertThat(masker.carriesPersonalData("a plain string")).isFalse();
        assertThat(masker.carriesPersonalData(new Customer())).isTrue();
    }
}
