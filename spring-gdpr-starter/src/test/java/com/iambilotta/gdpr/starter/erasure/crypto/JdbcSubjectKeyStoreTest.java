package com.iambilotta.gdpr.starter.erasure.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the default production key store ({@link JdbcSubjectKeyStore}) against H2, including the
 * tombstone invariant: a dropped subject can never be re-minted (no involuntary un-erasure).
 */
class JdbcSubjectKeyStoreTest {

    private DataSource dataSource;

    @BeforeEach
    void initDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:gdpr-keys-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        this.dataSource = ds;
    }

    /**
     * @spec.given a JDBC key store on a fresh schema
     * @spec.when  a subject's key is minted then read back
     * @spec.then  getOrCreate is idempotent and keyFor returns the same key bytes
     * @spec.adr   ADR-0009
     * @spec.us    REQ-GDPR-016
     */
    @Test
    void mintsAndReadsBackAStableKey() {
        JdbcSubjectKeyStore store = new JdbcSubjectKeyStore(dataSource, "gdpr_subject_key", true);

        byte[] first = store.getOrCreate("alice-1");
        byte[] second = store.getOrCreate("alice-1");

        assertThat(first).hasSize(32); // AES-256
        assertThat(second).isEqualTo(first);
        assertThat(store.keyFor("alice-1")).contains(first);
        assertThat(store.exists("alice-1")).isTrue();
    }

    /**
     * @spec.given a subject with a minted key
     * @spec.when  the key is dropped (erasure)
     * @spec.then  keyFor is empty, exists is false, and the bytes are gone
     * @spec.adr   ADR-0009
     * @spec.us    REQ-GDPR-016
     */
    @Test
    void dropRemovesTheKey() {
        JdbcSubjectKeyStore store = new JdbcSubjectKeyStore(dataSource, "gdpr_subject_key", true);
        store.getOrCreate("alice-1");

        store.drop("alice-1");

        assertThat(store.keyFor("alice-1")).isEmpty();
        assertThat(store.exists("alice-1")).isFalse();
    }

    /**
     * @spec.given a subject whose key was dropped (erased)
     * @spec.when  getOrCreate is called again for the same subject
     * @spec.then  it refuses to mint a new key (the tombstone prevents un-erasure)
     * @spec.adr   ADR-0009
     * @spec.us    REQ-GDPR-016
     */
    @Test
    void erasedSubjectCannotBeReMinted() {
        JdbcSubjectKeyStore store = new JdbcSubjectKeyStore(dataSource, "gdpr_subject_key", true);
        store.getOrCreate("alice-1");
        store.drop("alice-1");

        assertThatThrownBy(() -> store.getOrCreate("alice-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("erased");
    }

    /**
     * @spec.given a subject that never had a key
     * @spec.when  drop is called for it (idempotent erasure)
     * @spec.then  a tombstone is written so the subject can never be minted afterwards
     * @spec.adr   ADR-0009
     * @spec.us    REQ-GDPR-016
     */
    @Test
    void dropOfNeverMintedSubjectStillTombstones() {
        JdbcSubjectKeyStore store = new JdbcSubjectKeyStore(dataSource, "gdpr_subject_key", true);

        store.drop("ghost-9");

        assertThat(store.keyFor("ghost-9")).isEmpty();
        assertThatThrownBy(() -> store.getOrCreate("ghost-9"))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * @spec.given two subjects with keys
     * @spec.when  one is erased
     * @spec.then  the other's key is untouched (per-subject granularity at the store level)
     * @spec.adr   ADR-0009
     * @spec.us    REQ-GDPR-016
     */
    @Test
    void dropIsPerSubject() {
        JdbcSubjectKeyStore store = new JdbcSubjectKeyStore(dataSource, "gdpr_subject_key", true);
        byte[] bobKey = store.getOrCreate("bob-2");
        store.getOrCreate("alice-1");

        store.drop("alice-1");

        Optional<byte[]> bob = store.keyFor("bob-2");
        assertThat(bob).contains(bobKey);
    }
}
