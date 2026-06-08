package com.iambilotta.gdpr.starter.access;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.iambilotta.gdpr.starter.jdbc.SqlIdentifiers;

/**
 * Ready-made {@link SubjectDataProvider} for the common case: fetch the rows of one table whose
 * subject-id column matches and map each to a {@code @GdprPersonalData}-annotated object that the
 * {@link AccessExportService} reflects into the Article 15 dossier. Register it as a bean instead of
 * hand-writing the select:
 *
 * <pre>{@code
 * @Bean
 * SubjectDataProvider customerExport(DataSource ds) {
 *     return new JdbcSubjectDataProvider(ds, "customers", "id",
 *             (rs, n) -> new Customer(rs.getString("full_name"), rs.getString("email")));
 * }
 * }</pre>
 *
 * <p>The mapper returns whatever object carries the {@code @GdprPersonalData} fields you want in the
 * export: usually the domain type itself. The {@code table} and {@code subjectIdColumn} are validated
 * as bare SQL identifiers at construction; the subject id is always bound as a {@code ?} parameter.
 * For a multi-table or external-system lookup, implement the raw {@link SubjectDataProvider}.
 */
public class JdbcSubjectDataProvider implements SubjectDataProvider {

    private final JdbcTemplate jdbc;
    private final String table;
    private final String subjectIdColumn;
    private final RowMapper<?> rowMapper;

    public JdbcSubjectDataProvider(
            DataSource dataSource,
            String table,
            String subjectIdColumn,
            RowMapper<?> rowMapper) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.table = SqlIdentifiers.requireIdentifier(table, "table");
        this.subjectIdColumn = SqlIdentifiers.requireIdentifier(subjectIdColumn, "subjectIdColumn");
        this.rowMapper = rowMapper;
    }

    @Override
    public List<?> dataFor(String subjectId) {
        return jdbc.query(
                "SELECT * FROM " + table + " WHERE " + subjectIdColumn + " = ?",
                rowMapper,
                subjectId);
    }
}
