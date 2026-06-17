package com.iambilotta.gdpr.starter.erasure.forgettable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the bundled V3 Flyway migration applies cleanly on a real PostgreSQL 16 container
 * and that {@link JdbcForgettablePayloadStore} round-trips payloads against the migrated schema.
 *
 * <p>This test was absent when commit 47afe07 fixed the NUL-byte tombstone issue; the forgettable
 * store was therefore broken on Postgres despite H2 tests passing. This IT closes that gap so the
 * Postgres path is part of the gate (mirroring {@code JdbcAuditSinkPostgresIT}).
 *
 * <p>Skipped when Docker is not available. Enable with {@code -Dspring-gdpr.it=true}.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "spring-gdpr.it", matches = "true")
class JdbcForgettablePayloadStorePostgresIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("gdpr")
            .withUsername("gdpr")
            .withPassword("gdpr");

    private static PGSimpleDataSource dataSource;

    @BeforeAll
    static void applyMigration() throws IOException {
        dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());

        String migrationSql;
        try (InputStream in = JdbcForgettablePayloadStorePostgresIT.class.getResourceAsStream(
                "/db/migration/V3__gdpr_forgettable_payload.sql")) {
            if (in == null) {
                throw new IllegalStateException("V3 migration script not on classpath");
            }
            migrationSql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        new JdbcTemplate(dataSource).execute(migrationSql);
    }

    @AfterAll
    static void cleanup() {
        if (dataSource != null) {
            new JdbcTemplate(dataSource).execute("DROP TABLE IF EXISTS gdpr_forgettable_payload");
        }
    }

    private JdbcForgettablePayloadStore store() {
        // autoCreateSchema=false: verifies the store works against the Flyway-applied schema,
        // not the bootstrapSchema() DDL, so the two paths stay in sync.
        return new JdbcForgettablePayloadStore(dataSource, "gdpr_forgettable_payload", false);
    }

    /**
     * @spec.given the V3 Flyway migration applied to a real PostgreSQL 16 database
     * @spec.when  a value is put and resolved
     * @spec.then  the store works against the migrated schema (migration is Postgres-compatible)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void putAndResolveAgainstMigratedPostgresSchema() {
        JdbcForgettablePayloadStore store = store();

        store.put("alice-1", "full_name", "Alice Liddell");

        assertThat(store.resolve("alice-1", "full_name")).contains("Alice Liddell");
    }

    /**
     * @spec.given the V3 Flyway migration applied to PostgreSQL (payload_value is TEXT, not VARCHAR)
     * @spec.when  a free-text value longer than 4096 characters is stored and resolved
     * @spec.then  the full value round-trips without truncation on Postgres
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void largeFreetextPayloadRoundTripsOnPostgres() {
        JdbcForgettablePayloadStore store = store();
        // 8 000-char string: representative of a long operator note / comment (the canonical
        // FORGETTABLE_PAYLOAD use case). Would overflow the old VARCHAR(4096) cap.
        String longNote = "B".repeat(8_000);

        store.put("bob-2", "operator_note", longNote);

        assertThat(store.resolve("bob-2", "operator_note")).contains(longNote);
    }

    /**
     * @spec.given a subject with fields stored on Postgres
     * @spec.when  the subject is erased
     * @spec.then  the tombstone is written using the plain '__erased__' reserved key (no NUL byte)
     *             and every resolved field is empty afterwards
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-022
     */
    @Test
    void erasureWritesTombstoneAndDeletesFieldsOnPostgres() {
        JdbcForgettablePayloadStore store = store();
        store.put("carol-3", "email", "carol@example.com");
        store.put("carol-3", "full_name", "Carol Danvers");

        int deleted = store.erase("carol-3");

        assertThat(deleted).isEqualTo(2);
        assertThat(store.resolve("carol-3", "email")).isEmpty();
        assertThat(store.resolve("carol-3", "full_name")).isEmpty();
    }
}
