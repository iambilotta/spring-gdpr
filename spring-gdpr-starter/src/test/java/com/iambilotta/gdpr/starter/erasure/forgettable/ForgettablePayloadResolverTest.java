package com.iambilotta.gdpr.starter.erasure.forgettable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link ForgettablePayloadResolver}: the read side of the forgettable-payload pattern. The
 * domain carries a {@link ForgettablePayloadReference}; the resolver fetches the value from the
 * external {@link ForgettablePayloadStore} on read. Fail-closed: an erased (or never-written) value
 * resolves to empty, and the "required" form throws rather than substituting a fake value
 * (ADR-0010).
 */
class ForgettablePayloadResolverTest {

    private final InMemoryForgettablePayloadStore store = new InMemoryForgettablePayloadStore();
    private final ForgettablePayloadResolver resolver = new ForgettablePayloadResolver(store);

    /**
     * @spec.given a stored value referenced by a ForgettablePayloadReference
     * @spec.when  the reference is resolved
     * @spec.then  the resolver returns the value from the external store
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void resolvesAReferenceToItsStoredValue() {
        store.put("alice-1", "full_name", "Alice Liddell");
        ForgettablePayloadReference ref = ForgettablePayloadReference.of("alice-1", "full_name");

        assertThat(resolver.resolve(ref)).contains("Alice Liddell");
    }

    /**
     * @spec.given a reference whose subject was erased
     * @spec.when  the reference is resolved
     * @spec.then  the resolver returns empty (the dangling reference exposes no PII)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void resolvesAnErasedReferenceToEmpty() {
        store.put("alice-1", "full_name", "Alice Liddell");
        store.erase("alice-1");
        ForgettablePayloadReference ref = ForgettablePayloadReference.of("alice-1", "full_name");

        assertThat(resolver.resolve(ref)).isEmpty();
    }

    /**
     * @spec.given a reference to a value that was erased
     * @spec.when  the value is required (not optional)
     * @spec.then  the resolver throws a typed not-available error instead of a placeholder
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void requireThrowsOnAnErasedValueRatherThanFakingOne() {
        store.put("alice-1", "full_name", "Alice Liddell");
        store.erase("alice-1");
        ForgettablePayloadReference ref = ForgettablePayloadReference.of("alice-1", "full_name");

        assertThatThrownBy(() -> resolver.require(ref))
                .isInstanceOf(ForgettablePayloadResolver.PayloadNotAvailableException.class);
    }

    /**
     * @spec.given a reference to a value that was never written
     * @spec.when  the value is required
     * @spec.then  the resolver throws (a missing value is never silently treated as present)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void requireThrowsOnAMissingValue() {
        ForgettablePayloadReference ref = ForgettablePayloadReference.of("ghost-9", "full_name");

        assertThatThrownBy(() -> resolver.require(ref))
                .isInstanceOf(ForgettablePayloadResolver.PayloadNotAvailableException.class);
    }
}
