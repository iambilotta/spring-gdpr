package com.iambilotta.gdpr.starter.erasure;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import com.iambilotta.gdpr.annotations.GdprErasable;
import com.iambilotta.gdpr.starter.jdbc.SqlIdentifiers;

/**
 * Ready-made {@link ErasureHandler} for the common case: erase the rows of one table whose
 * subject-id column matches. Register it as a bean instead of hand-writing
 * {@code repo.deleteBySubjectId} for every table:
 *
 * <pre>{@code
 * @Bean
 * ErasureHandler customerErasure(DataSource ds) {
 *     return new JdbcErasureHandler(ds, Customer.class, "customers", "id", Strategy.DELETE);
 * }
 * }</pre>
 *
 * <p>This is a battery, not a cage: when the lookup is anything other than "delete one table by one
 * column" (FK cascades that need a transaction, anonymize that nulls specific columns, a non-JDBC
 * store), implement the raw {@link ErasureHandler} interface directly. Declare {@link #order()} so
 * child rows are erased before parents (FK-safe ordering).
 *
 * <p>The {@code table} and {@code subjectIdColumn} are validated as bare SQL identifiers at
 * construction (they are interpolated, not bindable); the subject id itself is always bound as a
 * {@code ?} parameter.
 */
public class JdbcErasureHandler implements ErasureHandler {

    private final JdbcTemplate jdbc;
    private final Class<?> entityType;
    private final String table;
    private final String subjectIdColumn;
    private final GdprErasable.Strategy strategy;
    private final int order;

    public JdbcErasureHandler(
            DataSource dataSource,
            Class<?> entityType,
            String table,
            String subjectIdColumn,
            GdprErasable.Strategy strategy) {
        this(dataSource, entityType, table, subjectIdColumn, strategy, 100);
    }

    public JdbcErasureHandler(
            DataSource dataSource,
            Class<?> entityType,
            String table,
            String subjectIdColumn,
            GdprErasable.Strategy strategy,
            int order) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.entityType = entityType;
        this.table = SqlIdentifiers.requireIdentifier(table, "table");
        this.subjectIdColumn = SqlIdentifiers.requireIdentifier(subjectIdColumn, "subjectIdColumn");
        this.strategy = strategy;
        this.order = order;
    }

    @Override
    public Class<?> entityType() {
        return entityType;
    }

    @Override
    public GdprErasable.Strategy strategy() {
        return strategy;
    }

    @Override
    public int erase(String subjectId) {
        return jdbc.update(
                "DELETE FROM " + table + " WHERE " + subjectIdColumn + " = ?",
                subjectId);
    }

    @Override
    public int order() {
        return order;
    }
}
