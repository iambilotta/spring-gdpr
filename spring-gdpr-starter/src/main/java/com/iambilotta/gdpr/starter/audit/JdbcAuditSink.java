package com.iambilotta.gdpr.starter.audit;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Persists audit records to a JDBC table.
 *
 * <p>The schema is NOT created automatically by default. Apply the migration shipped at
 * {@code db/migration/V1__gdpr_audit_access.sql} (Flyway) or
 * {@code db/changelog/spring-gdpr-changelog.xml} (Liquibase) via your existing
 * migration tool. This is the production-correct path: migration history, rollback,
 * coordination across replicas.
 *
 * <p>For local dev / tests / one-shot demos that do not run a migration tool, set
 * {@code spring.gdpr.audit.auto-create-schema=true}. The constructor will then issue
 * a {@code CREATE TABLE IF NOT EXISTS} on first use.
 *
 * <p>Override-able: clients can supply their own {@link AuditSink} bean to bypass this
 * implementation entirely.
 */
public class JdbcAuditSink implements AuditSink {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcAuditSink.class);

    private final JdbcTemplate jdbc;
    private final String table;

    public JdbcAuditSink(DataSource dataSource, String table) {
        this(dataSource, table, false);
    }

    public JdbcAuditSink(DataSource dataSource, String table, boolean autoCreateSchema) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.table = sanitize(table);
        if (autoCreateSchema) {
            bootstrapSchema();
        } else {
            verifyTableExists();
        }
    }

    /**
     * Idempotent best-effort schema creation. NOT used in production; supports the
     * {@code auto-create-schema} dev shortcut and the unit tests in this module.
     *
     * <p>Note: {@code CREATE INDEX IF NOT EXISTS} is not supported by Oracle and DB2.
     * If you target those databases, use the bundled migration scripts and leave
     * auto-create off.
     */
    public final void bootstrapSchema() {
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
        try {
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_" + table + "_subject ON " + table + " (subject_id, at_ts)");
        } catch (DataAccessException ex) {
            LOG.warn(
                    "spring-gdpr could not create idx_{}_subject (likely Oracle/DB2 syntax mismatch). "
                            + "Apply the bundled migration script instead. Cause: {}",
                    table, ex.getMessage());
        }
    }

    /**
     * Fails fast at startup if the table is missing. The downside of fail-fast here
     * is acceptable: the JDBC sink is opt-in, the user explicitly asked for it,
     * and starting an app with a misconfigured audit-write target would silently
     * lose events.
     */
    private void verifyTableExists() {
        try {
            jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE 1=0", Integer.class);
        } catch (DataAccessException ex) {
            throw new IllegalStateException(
                    "spring-gdpr audit table " + table + " is not present. Apply the migration shipped "
                            + "at db/migration/V1__gdpr_audit_access.sql (Flyway) or db/changelog/spring-gdpr-changelog.xml "
                            + "(Liquibase), or set spring.gdpr.audit.auto-create-schema=true for dev.",
                    ex);
        }
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
