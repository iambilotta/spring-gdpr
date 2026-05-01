package com.iambilotta.gdpr.starter.audit;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Persists audit records to a JDBC table. Schema is created on first call to {@link #ensureSchema()}.
 * Override-able: clients can supply their own {@link AuditSink} bean to bypass this implementation.
 */
public class JdbcAuditSink implements AuditSink {

    private final JdbcTemplate jdbc;
    private final String table;

    public JdbcAuditSink(DataSource dataSource, String table) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.table = sanitize(table);
        ensureSchema();
    }

    public final void ensureSchema() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS " + table + " ("
                + "event_id VARCHAR(64) PRIMARY KEY, "
                + "at_ts TIMESTAMP NOT NULL, "
                + "actor VARCHAR(255), "
                + "subject_id VARCHAR(255), "
                + "target_type VARCHAR(512) NOT NULL, "
                + "target_member VARCHAR(255), "
                + "legal_basis VARCHAR(64), "
                + "special_category BOOLEAN NOT NULL"
                + ")");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_" + table + "_subject ON " + table + " (subject_id, at_ts)");
    }

    @Override
    public void write(AuditAccessRecord record) {
        jdbc.update(
                "INSERT INTO " + table + " (event_id, at_ts, actor, subject_id, target_type, target_member, legal_basis, special_category) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                record.eventId(),
                Timestamp.from(record.at()),
                record.actor(),
                record.subjectId(),
                record.targetType(),
                record.targetMember(),
                record.legalBasis(),
                record.specialCategory()
        );
    }

    @Override
    public List<AuditAccessRecord> findBySubject(String subjectId, Instant from, Instant to) {
        Instant rangeFrom = from != null ? from : Instant.EPOCH;
        Instant rangeTo = to != null ? to : Instant.now();
        return jdbc.query(
                "SELECT event_id, at_ts, actor, subject_id, target_type, target_member, legal_basis, special_category "
                        + "FROM " + table + " WHERE subject_id = ? AND at_ts BETWEEN ? AND ? ORDER BY at_ts ASC",
                ROW_MAPPER,
                subjectId,
                Timestamp.from(rangeFrom),
                Timestamp.from(rangeTo)
        );
    }

    private static final RowMapper<AuditAccessRecord> ROW_MAPPER = (rs, rowNum) -> new AuditAccessRecord(
            rs.getString("event_id"),
            rs.getTimestamp("at_ts").toInstant(),
            rs.getString("actor"),
            rs.getString("subject_id"),
            rs.getString("target_type"),
            rs.getString("target_member"),
            rs.getString("legal_basis"),
            rs.getBoolean("special_category")
    );

    /**
     * Defensive whitelist on the configurable table name. Only allow letters, digits, underscore.
     * Callers cannot inject SQL via {@code spring.gdpr.audit.table}.
     */
    private static String sanitize(String identifier) {
        if (identifier == null || !identifier.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException(
                    "spring.gdpr.audit.table must match [A-Za-z_][A-Za-z0-9_]* (got: " + identifier + ")");
        }
        return identifier;
    }
}
