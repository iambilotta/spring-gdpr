package com.iambilotta.gdpr.starter.erasure.forgettable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Pins the {@link ForgettablePayloadReference} value type: the only thing a domain object / event
 * carries inline once a field is externalised. It is the pointer (subjectId + fieldKey) to the row
 * in the external PII store, never the value itself (ADR-0010).
 */
class ForgettablePayloadReferenceTest {

    /**
     * @spec.given a subject id and a field key
     * @spec.when  a reference is built and rendered as a URN
     * @spec.then  the URN is stable and round-trips back to the same reference
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void roundTripsThroughItsUrn() {
        ForgettablePayloadReference ref = ForgettablePayloadReference.of("alice-1", "full_name");

        assertThat(ref.subjectId()).isEqualTo("alice-1");
        assertThat(ref.fieldKey()).isEqualTo("full_name");
        assertThat(ref.toUrn()).isEqualTo("urn:gdpr:fp:alice-1:full_name");
        assertThat(ForgettablePayloadReference.fromUrn(ref.toUrn())).isEqualTo(ref);
    }

    /**
     * @spec.given a blank subject id or a blank field key
     * @spec.when  a reference is constructed
     * @spec.then  construction is rejected (a reference must always point somewhere)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void rejectsBlankCoordinates() {
        assertThatThrownBy(() -> ForgettablePayloadReference.of("", "full_name"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ForgettablePayloadReference.of("alice-1", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * @spec.given a malformed URN (wrong scheme or missing segments)
     * @spec.when  it is parsed back into a reference
     * @spec.then  parsing is rejected rather than producing a half-built reference
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void rejectsMalformedUrn() {
        assertThatThrownBy(() -> ForgettablePayloadReference.fromUrn("urn:other:alice-1:full_name"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ForgettablePayloadReference.fromUrn("urn:gdpr:fp:alice-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
