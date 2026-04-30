package com.iambilotta.gdpr.starter.audit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

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
 * Verifies the bundled Flyway migration applies cleanly on a real PostgreSQL 16 container,
 * and that the JDBC sink reads/writes against that schema without auto-create.
 *
 * <p>Skipped when Docker is not available (CI without docker-in-docker, dev machines without
 * Docker installed). Enable explicitly with {@code -Dspring-gdpr.it=true}, or run
 * {@code mvn -Dspring-gdpr.it=true verify}.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "spring-gdpr.it", matches = "true")
class JdbcAuditSinkPostgresIT {

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
        try (InputStream in = JdbcAuditSinkPostgresIT.class.getResourceAsStream(
                "/db/migration/V1__gdpr_audit_access.sql")) {
            if (in == null) {
                throw new IllegalStateException("migration script not on classpath");
            }
            migrationSql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        new JdbcTemplate(dataSource).execute(migrationSql);
    }

    @AfterAll
    static void cleanup() {
        if (dataSource != null) {
            new JdbcTemplate(dataSource).execute("DROP TABLE IF EXISTS gdpr_audit_access");
        }
    }

    @Test
    void writesAndReadsAgainstMigrationApplied() {
        JdbcAuditSink sink = new JdbcAuditSink(dataSource, "gdpr_audit_access");

        sink.write(new AuditAccessRecord(
                "e1", Instant.parse("2030-01-01T10:00:00Z"), "alice",
                "alice-1", "com.x.Customer", "findById", "6(1)(b)", false));
        sink.write(new AuditAccessRecord(
                "e2", Instant.parse("2030-01-01T10:05:00Z"), "alice",
                "alice-1", "com.x.Customer", "findById", "6(1)(a) + 9(2)(a)", true));

        List<AuditAccessRecord> records = sink.findBySubject("alice-1", null, null);
        assertThat(records).hasSize(2);
        assertThat(records.get(1).specialCategory()).isTrue();
        assertThat(records.get(1).legalBasis()).isEqualTo("6(1)(a) + 9(2)(a)");
    }

    @Test
    void failsFastWhenTableMissingAndAutoCreateOff() {
        new JdbcTemplate(dataSource).execute("DROP TABLE IF EXISTS missing_table");
        try {
            new JdbcAuditSink(dataSource, "missing_table", false);
            assertThat(false).as("should have thrown IllegalStateException").isTrue();
        } catch (IllegalStateException expected) {
            assertThat(expected).hasMessageContaining("not present");
        }
    }
}
