package com.iambilotta.gdpr.starter.erasure.forgettable;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.iambilotta.gdpr.starter.jdbc.SqlIdentifiers;

/**
 * Default JDBC {@link ForgettablePayloadStore}. Backs the externalised PII with a
 * {@code gdpr_forgettable_payload} table: {@code subject_id} + {@code field_key} (composite PK) +
 * {@code value} + {@code updated_at}, plus a per-subject tombstone {@code erased_at}.
 *
 * <p><strong>Erasure = actual {@code DELETE}.</strong> {@link #erase(String)} deletes every value
 * row for the subject and stamps the tombstone. After that, {@link #resolve} returns empty and
 * {@link #put} for the same subject throws (the row only ever held a reference on the carrier, so
 * the personal data is gone, not merely unreadable; see ADR-0010 on why this is the primary path
 * over crypto-shredding's pseudonymising key-drop).
 *
 * <p><strong>Tombstone.</strong> The tombstone is a reserved row with {@code field_key =}
 * {@link #TOMBSTONE_FIELD_KEY} and a non-null {@code erased_at}. Keeping it is what makes the
 * erasure a recorded fact and stops {@link #put} from re-creating an erased subject's data. The
 * reserved key is rejected as a caller-supplied {@code fieldKey}, so it can never collide with a
 * real field.
 *
 * <p>Apply the migration {@code db/migration/V3__gdpr_forgettable_payload.sql}, or set
 * {@code autoCreateSchema=true} for dev/tests.
 */
public final class JdbcForgettablePayloadStore implements ForgettablePayloadStore {

    /** Reserved {@code field_key} of the per-subject tombstone row. Never a real field. */
    static final String TOMBSTONE_FIELD_KEY = "__erased__";

    private final JdbcTemplate jdbc;
    private final String table;

    public JdbcForgettablePayloadStore(DataSource dataSource) {
        this(dataSource, "gdpr_forgettable_payload", false);
    }

    public JdbcForgettablePayloadStore(DataSource dataSource, String table, boolean autoCreateSchema) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.table = SqlIdentifiers.requireIdentifier(table, "table");
        if (autoCreateSchema) {
            bootstrapSchema();
        }
    }

    public void bootstrapSchema() {
        // TEXT (unbounded) mirrors V3__gdpr_forgettable_payload.sql: the primary use case is
        // free-text PII (operator notes, comments) that can easily exceed 4096 characters.
        jdbc.execute("CREATE TABLE IF NOT EXISTS " + table + " ("
                + "subject_id VARCHAR(255) NOT NULL, "
                + "field_key  VARCHAR(255) NOT NULL, "
                + "payload_value TEXT, "
                + "updated_at TIMESTAMP NOT NULL, "
                + "erased_at  TIMESTAMP, "
                + "PRIMARY KEY (subject_id, field_key)"
                + ")");
    }

    @Override
    public void put(String subjectId, String fieldKey, String value) {
        requireSubject(subjectId);
        requireFieldKey(fieldKey);
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        if (isErased(subjectId)) {
            throw new IllegalStateException(
                    "subject " + subjectId + " was erased; writing a payload would un-erase them");
        }
        Timestamp now = Timestamp.from(Instant.now());
        int updated = jdbc.update(
                "UPDATE " + table + " SET payload_value = ?, updated_at = ? WHERE subject_id = ? AND field_key = ?",
                value, now, subjectId, fieldKey);
        if (updated == 0) {
            try {
                jdbc.update(
                        "INSERT INTO " + table + " (subject_id, field_key, payload_value, updated_at) VALUES (?, ?, ?, ?)",
                        subjectId, fieldKey, value, now);
            } catch (org.springframework.dao.DuplicateKeyException raced) {
                // Lost the insert race; re-apply as an update (still honouring the tombstone above).
                put(subjectId, fieldKey, value);
            }
        }
    }

    @Override
    public Optional<String> resolve(String subjectId, String fieldKey) {
        if (subjectId == null || fieldKey == null) {
            return Optional.empty();
        }
        try {
            String value = jdbc.queryForObject(
                    "SELECT payload_value FROM " + table + " WHERE subject_id = ? AND field_key = ?",
                    String.class,
                    subjectId, fieldKey);
            return Optional.ofNullable(value);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public int erase(String subjectId) {
        requireSubject(subjectId);
        int deleted = jdbc.update(
                "DELETE FROM " + table + " WHERE subject_id = ? AND field_key <> ?",
                subjectId, TOMBSTONE_FIELD_KEY);
        Timestamp now = Timestamp.from(Instant.now());
        int tomb = jdbc.update(
                "UPDATE " + table + " SET erased_at = ? WHERE subject_id = ? AND field_key = ?",
                now, subjectId, TOMBSTONE_FIELD_KEY);
        if (tomb == 0) {
            jdbc.update(
                    "INSERT INTO " + table + " (subject_id, field_key, payload_value, updated_at, erased_at) "
                            + "VALUES (?, ?, NULL, ?, ?)",
                    subjectId, TOMBSTONE_FIELD_KEY, now, now);
        }
        return deleted;
    }

    private boolean isErased(String subjectId) {
        try {
            Timestamp erasedAt = jdbc.queryForObject(
                    "SELECT erased_at FROM " + table + " WHERE subject_id = ? AND field_key = ?",
                    Timestamp.class,
                    subjectId, TOMBSTONE_FIELD_KEY);
            return erasedAt != null;
        } catch (EmptyResultDataAccessException ex) {
            return false;
        }
    }

    private static void requireSubject(String subjectId) {
        if (subjectId == null || subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId must be provided");
        }
    }

    private static void requireFieldKey(String fieldKey) {
        if (fieldKey == null || fieldKey.isBlank()) {
            throw new IllegalArgumentException("fieldKey must be provided");
        }
        if (TOMBSTONE_FIELD_KEY.equals(fieldKey)) {
            throw new IllegalArgumentException("fieldKey is reserved");
        }
    }
}
