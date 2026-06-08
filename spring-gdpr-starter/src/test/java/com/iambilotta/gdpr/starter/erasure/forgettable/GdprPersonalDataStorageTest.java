package com.iambilotta.gdpr.starter.erasure.forgettable;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.iambilotta.gdpr.annotations.GdprPersonalData;
import com.iambilotta.gdpr.annotations.GdprPersonalData.Storage;

/**
 * Pins the {@code storage} axis on {@link GdprPersonalData} (ADR-0010): a field declares whether its
 * value is stored inline (the legacy default) or externalised to the forgettable-payload store. The
 * axis is what lets a generator and an adopter route a field to the primary erasure path without a
 * separate marker annotation. Backward-compatible: the default stays {@code INLINE}.
 */
class GdprPersonalDataStorageTest {

    static final class Sample {
        @GdprPersonalData
        String legacyInline;

        @GdprPersonalData(storage = Storage.FORGETTABLE_PAYLOAD)
        String externalised;
    }

    /**
     * @spec.given a personal-data field with no storage declared
     * @spec.when  the annotation's storage axis is read
     * @spec.then  it defaults to INLINE (every pre-existing annotation stays valid)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void defaultsToInlineForBackwardCompatibility() throws NoSuchFieldException {
        Field field = Sample.class.getDeclaredField("legacyInline");

        Storage storage = field.getAnnotation(GdprPersonalData.class).storage();

        assertThat(storage).isEqualTo(Storage.INLINE);
    }

    /**
     * @spec.given a field marked storage = FORGETTABLE_PAYLOAD
     * @spec.when  the annotation's storage axis is read
     * @spec.then  it reports FORGETTABLE_PAYLOAD (the field routes to the external store + primary erasure)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void carriesTheForgettablePayloadStorageWhenDeclared() throws NoSuchFieldException {
        Field field = Sample.class.getDeclaredField("externalised");

        Storage storage = field.getAnnotation(GdprPersonalData.class).storage();

        assertThat(storage).isEqualTo(Storage.FORGETTABLE_PAYLOAD);
    }
}
