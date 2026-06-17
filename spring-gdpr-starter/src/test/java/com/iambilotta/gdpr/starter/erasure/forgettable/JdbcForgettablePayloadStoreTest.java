package com.iambilotta.gdpr.starter.erasure.forgettable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the default production external PII store ({@link JdbcForgettablePayloadStore}) against H2,
 * including the no-resurrection tombstone: a subject erased once can never have a payload silently
 * re-written for them (ADR-0010). This is the forgettable-payload analogue of
 * {@code JdbcSubjectKeyStoreTest}, except erasure here is an ACTUAL {@code DELETE} of the value,
 * not a key drop.
 */
class JdbcForgettablePayloadStoreTest {

    private DataSource dataSource;

    @BeforeEach
    void initDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:gdpr-fp-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        this.dataSource = ds;
    }

    private JdbcForgettablePayloadStore store() {
        return new JdbcForgettablePayloadStore(dataSource, "gdpr_forgettable_payload", true);
    }

    /**
     * @spec.given a JDBC payload store on a fresh schema
     * @spec.when  a value is put for (subject, field) and resolved back
     * @spec.then  resolve returns the stored value (the externalised PII lives only here)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void putsAndResolvesAValue() {
        JdbcForgettablePayloadStore store = store();

        store.put("alice-1", "full_name", "Alice Liddell");

        assertThat(store.resolve("alice-1", "full_name")).contains("Alice Liddell");
    }

    /**
     * @spec.given an existing value for (subject, field)
     * @spec.when  a new value is put for the same coordinates (the store is mutable)
     * @spec.then  resolve returns the latest value (last-writer-wins upsert)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void putIsAnUpsert() {
        JdbcForgettablePayloadStore store = store();

        store.put("alice-1", "email", "old@example.com");
        store.put("alice-1", "email", "new@example.com");

        assertThat(store.resolve("alice-1", "email")).contains("new@example.com");
    }

    /**
     * @spec.given a subject with two externalised fields
     * @spec.when  the subject is erased
     * @spec.then  every field for that subject resolves to empty (actual deletion of the PII)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void eraseDeletesEveryFieldForTheSubject() {
        JdbcForgettablePayloadStore store = store();
        store.put("alice-1", "full_name", "Alice Liddell");
        store.put("alice-1", "email", "alice@example.com");

        int affected = store.erase("alice-1");

        assertThat(affected).isEqualTo(2);
        assertThat(store.resolve("alice-1", "full_name")).isEmpty();
        assertThat(store.resolve("alice-1", "email")).isEmpty();
    }

    /**
     * @spec.given two subjects with externalised values
     * @spec.when  one is erased
     * @spec.then  the other's values are untouched (per-subject erasure granularity)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void eraseIsPerSubject() {
        JdbcForgettablePayloadStore store = store();
        store.put("alice-1", "full_name", "Alice");
        store.put("bob-2", "full_name", "Bob");

        store.erase("alice-1");

        assertThat(store.resolve("alice-1", "full_name")).isEmpty();
        assertThat(store.resolve("bob-2", "full_name")).contains("Bob");
    }

    /**
     * @spec.given a subject that was erased (its rows deleted, a tombstone recorded)
     * @spec.when  a new value is put for that same subject
     * @spec.then  the put is refused (the tombstone forbids silently re-creating an erased subject)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void erasedSubjectCannotHaveAPayloadReWritten() {
        JdbcForgettablePayloadStore store = store();
        store.put("alice-1", "full_name", "Alice");
        store.erase("alice-1");

        assertThatThrownBy(() -> store.put("alice-1", "full_name", "Alice again"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("erased");
    }

    /**
     * @spec.given a subject that never had any payload
     * @spec.when  erase is called for it (idempotent erasure)
     * @spec.then  a tombstone is recorded so the subject can never be populated afterwards
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void eraseOfNeverSeenSubjectStillTombstones() {
        JdbcForgettablePayloadStore store = store();

        int affected = store.erase("ghost-9");

        assertThat(affected).isEqualTo(0);
        assertThatThrownBy(() -> store.put("ghost-9", "full_name", "Ghost"))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * @spec.given a subject without a value for a given field
     * @spec.when  that field is resolved
     * @spec.then  resolve returns empty (fail-closed: a missing value is never a partial/placeholder)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void resolveOfMissingFieldIsEmpty() {
        JdbcForgettablePayloadStore store = store();

        Optional<String> resolved = store.resolve("alice-1", "full_name");

        assertThat(resolved).isEmpty();
    }

    /**
     * @spec.given a free-text PII value longer than 4096 characters (the old VARCHAR cap)
     * @spec.when  that value is stored and resolved
     * @spec.then  the full value round-trips without truncation (payload_value is TEXT, unbounded)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void handlesLargeFreetextPayloadBeyondOldVarcharLimit() {
        JdbcForgettablePayloadStore store = store();
        // 8 000-char string: well above the old VARCHAR(4096) cap, representative of a long
        // operator note or free-text comment (the canonical FORGETTABLE_PAYLOAD use case).
        String longNote = "A".repeat(8_000);

        store.put("alice-1", "operator_note", longNote);

        assertThat(store.resolve("alice-1", "operator_note")).contains(longNote);
    }

    /**
     * @spec.given a hostile table name that is not a bare SQL identifier
     * @spec.when  the store is constructed with it
     * @spec.then  construction fails fast before any SQL is built (injection-safe)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void rejectsTableNamesThatLookLikeSqlInjection() {
        assertThatThrownBy(
                () -> new JdbcForgettablePayloadStore(dataSource, "payload; DROP TABLE users--", true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
