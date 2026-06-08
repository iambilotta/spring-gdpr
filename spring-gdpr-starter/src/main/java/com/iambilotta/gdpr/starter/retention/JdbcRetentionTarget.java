package com.iambilotta.gdpr.starter.retention;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.StringJoiner;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import com.iambilotta.gdpr.annotations.GdprRetention;
import com.iambilotta.gdpr.starter.jdbc.SqlIdentifiers;

/**
 * Ready-made {@link RetentionTarget} for the common case: enforce a retention policy on one table by
 * its timestamp column. Register it as a bean instead of hand-writing the sweep SQL:
 *
 * <pre>{@code
 * // delete rows older than the cutoff
 * new JdbcRetentionTarget(ds, Customer.class, "customers", "created_at",
 *         Duration.ofDays(365 * 5), Strategy.DELETE);
 *
 * // or null the personal-data columns in place (anonymize), keeping the row
 * new JdbcRetentionTarget(ds, Customer.class, "customers", "created_at",
 *         Duration.ofDays(365 * 5), Strategy.ANONYMIZE, "full_name", "email");
 * }</pre>
 *
 * <p>{@code DELETE} removes the due rows; {@code ANONYMIZE} sets the listed personal-data columns to
 * {@code NULL} on the due rows. {@code PSEUDONYMIZE} is not a one-line JDBC operation (it needs a
 * deterministic token), so it is rejected here: implement the raw {@link RetentionTarget} for it.
 *
 * <p>All identifiers (table, timestamp column, personal-data columns) are validated as bare SQL
 * identifiers at construction; the cutoff is always bound as a {@code ?} parameter.
 */
public class JdbcRetentionTarget implements RetentionTarget {

    private final JdbcTemplate jdbc;
    private final Class<?> entityType;
    private final String table;
    private final String timestampColumn;
    private final Duration retentionPeriod;
    private final GdprRetention.Strategy strategy;
    private final List<String> personalDataColumns;

    public JdbcRetentionTarget(
            DataSource dataSource,
            Class<?> entityType,
            String table,
            String timestampColumn,
            Duration retentionPeriod,
            GdprRetention.Strategy strategy,
            String... personalDataColumns) {
        if (strategy == GdprRetention.Strategy.ANONYMIZE
                && (personalDataColumns == null || personalDataColumns.length == 0)) {
            throw new IllegalArgumentException(
                    "ANONYMIZE strategy needs at least one personal-data column to null");
        }
        if (strategy == GdprRetention.Strategy.PSEUDONYMIZE) {
            throw new IllegalArgumentException(
                    "PSEUDONYMIZE is not a one-line JDBC sweep: implement RetentionTarget directly");
        }
        this.jdbc = new JdbcTemplate(dataSource);
        this.entityType = entityType;
        this.table = SqlIdentifiers.requireIdentifier(table, "table");
        this.timestampColumn = SqlIdentifiers.requireIdentifier(timestampColumn, "timestampColumn");
        this.retentionPeriod = retentionPeriod;
        this.strategy = strategy;
        this.personalDataColumns = personalDataColumns == null
                ? List.of()
                : List.of(personalDataColumns).stream()
                        .map(c -> SqlIdentifiers.requireIdentifier(c, "personalDataColumn"))
                        .toList();
    }

    @Override
    public Class<?> entityType() {
        return entityType;
    }

    @Override
    public Duration retentionPeriod() {
        return retentionPeriod;
    }

    @Override
    public GdprRetention.Strategy strategy() {
        return strategy;
    }

    @Override
    public long countDue(Instant cutoff) {
        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE " + timestampColumn + " < ?",
                Long.class,
                Timestamp.from(cutoff));
        return count != null ? count : 0L;
    }

    @Override
    public long applyDue(Instant cutoff) {
        String sql = switch (strategy) {
            case DELETE -> "DELETE FROM " + table + " WHERE " + timestampColumn + " < ?";
            case ANONYMIZE -> "UPDATE " + table + " SET " + nullAssignments()
                    + " WHERE " + timestampColumn + " < ?";
            case PSEUDONYMIZE -> throw new IllegalStateException("unreachable: rejected in constructor");
        };
        return jdbc.update(sql, Timestamp.from(cutoff));
    }

    private String nullAssignments() {
        StringJoiner sets = new StringJoiner(", ");
        for (String column : personalDataColumns) {
            sets.add(column + " = NULL");
        }
        return sets.toString();
    }
}
