package com.iambilotta.gdpr.starter.erasure.forgettable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Pins the in-memory {@link ForgettablePayloadStore} used by tests and by the in-test "external
 * vault" harness. Same contract as the JDBC default, including the no-resurrection tombstone
 * (ADR-0010): a heap-only store, never for production.
 */
class InMemoryForgettablePayloadStoreTest {

    /**
     * @spec.given an in-memory payload store
     * @spec.when  a value is put and resolved
     * @spec.then  resolve returns it; a missing field resolves empty (fail-closed)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void putsResolvesAndMissesEmpty() {
        InMemoryForgettablePayloadStore store = new InMemoryForgettablePayloadStore();

        store.put("alice-1", "full_name", "Alice");

        assertThat(store.resolve("alice-1", "full_name")).contains("Alice");
        assertThat(store.resolve("alice-1", "email")).isEmpty();
    }

    /**
     * @spec.given a subject with externalised values
     * @spec.when  the subject is erased then a put is attempted again
     * @spec.then  values resolve empty and the put is refused (tombstone, no resurrection)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void eraseDeletesAndTombstones() {
        InMemoryForgettablePayloadStore store = new InMemoryForgettablePayloadStore();
        store.put("alice-1", "full_name", "Alice");
        store.put("alice-1", "email", "alice@example.com");

        int affected = store.erase("alice-1");

        assertThat(affected).isEqualTo(2);
        assertThat(store.resolve("alice-1", "full_name")).isEmpty();
        assertThatThrownBy(() -> store.put("alice-1", "full_name", "Alice again"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("erased");
    }
}
