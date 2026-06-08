package com.iambilotta.gdpr.starter.erasure.crypto;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.iambilotta.gdpr.starter.jdbc.SqlIdentifiers;

/**
 * Default JDBC {@link SubjectKeyStore}. Backs the per-subject keys with a {@code gdpr_subject_key}
 * table: {@code subject_id} (PK) + {@code wrapped_key} (the key bytes) + {@code created_at} +
 * {@code erased_at} (the tombstone).
 *
 * <p><strong>Erasure = drop the key.</strong> {@link #drop(String)} nulls {@code wrapped_key} and
 * stamps {@code erased_at}; the row stays as a tombstone. Keeping the tombstone is what makes the
 * erasure a <em>recorded fact</em> and stops {@link #getOrCreate(String)} from re-minting a key
 * for an erased subject (ADR-0009 involuntary-erasure constraint).
 *
 * <p><strong>Wrapped key.</strong> The column is named {@code wrapped_key} because the SPI lets an
 * adopter store the key wrapped under a KEK held in a KMS / Secret Manager. This default
 * implementation stores the raw 256-bit key; protect the table at rest (DB-level encryption,
 * least-privilege grants) and honour the key-backup constraint (see {@link SubjectKeyStore}).
 *
 * <p>Apply the migration {@code db/migration/V2__gdpr_subject_key.sql}, or set
 * {@code autoCreateSchema=true} for dev/tests.
 */
public final class JdbcSubjectKeyStore implements SubjectKeyStore {

    private final JdbcTemplate jdbc;
    private final String table;
    private final SubjectKeyFactory keyFactory;

    public JdbcSubjectKeyStore(DataSource dataSource) {
        this(dataSource, "gdpr_subject_key", false);
    }

    public JdbcSubjectKeyStore(DataSource dataSource, String table, boolean autoCreateSchema) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.table = SqlIdentifiers.requireIdentifier(table, "table");
        this.keyFactory = new SubjectKeyFactory();
        if (autoCreateSchema) {
            bootstrapSchema();
        }
    }

    public void bootstrapSchema() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS " + table + " ("
                + "subject_id  VARCHAR(255) PRIMARY KEY, "
                + "wrapped_key VARBINARY(255), "
                + "created_at  TIMESTAMP NOT NULL, "
                + "erased_at   TIMESTAMP"
                + ")");
    }

    @Override
    public Optional<byte[]> keyFor(String subjectId) {
        try {
            byte[] key = jdbc.queryForObject(
                    "SELECT wrapped_key FROM " + table + " WHERE subject_id = ?",
                    byte[].class,
                    subjectId);
            return Optional.ofNullable(key);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public byte[] getOrCreate(String subjectId) {
        Optional<RowState> existing = rowState(subjectId);
        if (existing.isPresent()) {
            RowState state = existing.get();
            if (state.erased()) {
                throw new IllegalStateException(
                        "subject " + subjectId + " was erased (key dropped); minting a new key would un-erase them");
            }
            return state.key();
        }
        byte[] key = keyFactory.newKey();
        try {
            jdbc.update(
                    "INSERT INTO " + table + " (subject_id, wrapped_key, created_at) VALUES (?, ?, ?)",
                    subjectId,
                    key,
                    Timestamp.from(Instant.now()));
            return key;
        } catch (org.springframework.dao.DuplicateKeyException raced) {
            // Another writer inserted concurrently; read back the winning key (or fail if it was erased).
            return getOrCreate(subjectId);
        }
    }

    @Override
    public void drop(String subjectId) {
        int updated = jdbc.update(
                "UPDATE " + table + " SET wrapped_key = NULL, erased_at = ? WHERE subject_id = ?",
                Timestamp.from(Instant.now()),
                subjectId);
        if (updated == 0) {
            // Never-minted subject: write a tombstone so a future getOrCreate cannot resurrect them.
            jdbc.update(
                    "INSERT INTO " + table + " (subject_id, wrapped_key, created_at, erased_at) VALUES (?, NULL, ?, ?)",
                    subjectId,
                    Timestamp.from(Instant.now()),
                    Timestamp.from(Instant.now()));
        }
    }

    @Override
    public boolean exists(String subjectId) {
        return keyFor(subjectId).isPresent();
    }

    private Optional<RowState> rowState(String subjectId) {
        try {
            return Optional.of(jdbc.queryForObject(
                    "SELECT wrapped_key, erased_at FROM " + table + " WHERE subject_id = ?",
                    (rs, rowNum) -> new RowState(
                            rs.getBytes("wrapped_key"),
                            rs.getTimestamp("erased_at") != null),
                    subjectId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private record RowState(byte[] key, boolean erased) {
    }
}
