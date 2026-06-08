package com.iambilotta.gdpr.starter.access;

import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract for the ready-made JDBC subject-data provider: select the rows of one table whose
 * subject-id column matches and map each row to a {@code @GdprPersonalData}-annotated object the
 * {@link AccessExportService} can reflect. A constructor + a RowMapper instead of hand-written SQL;
 * the raw {@link SubjectDataProvider} stays as the escape hatch.
 */
class JdbcSubjectDataProviderTest {

    private DataSource dataSource;
    private JdbcTemplate jdbc;

    @BeforeEach
    void initDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:gdpr-access-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        this.dataSource = ds;
        this.jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE customers (id VARCHAR(64) PRIMARY KEY, full_name VARCHAR(255))");
        jdbc.update("INSERT INTO customers VALUES ('alice-1', 'Alice')");
        jdbc.update("INSERT INTO customers VALUES ('bob-2', 'Bob')");
    }

    private static final RowMapper<Object> MAPPER = (rs, rowNum) -> new ProfileRow(rs.getString("full_name"));

    /**
     * @spec.given a customers table with the subject's row
     * @spec.when  the JDBC subject-data provider fetches by the subject-id column
     * @spec.then  it returns the mapped object(s) for that subject only
     * @spec.us    US-DX-003-jdbc-spi-base-classes
     */
    @Test
    void returnsTheSubjectsRowsMappedToPersonalDataObjects() {
        JdbcSubjectDataProvider provider = new JdbcSubjectDataProvider(
                dataSource, "customers", "id", MAPPER);

        List<?> rows = provider.dataFor("alice-1");

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).isInstanceOf(ProfileRow.class);
        assertThat(((ProfileRow) rows.get(0)).fullName).isEqualTo("Alice");
    }

    /**
     * @spec.given the JDBC subject-data provider behind the AccessExportService
     * @spec.when  an export is assembled for the subject
     * @spec.then  the dossier carries the classified field value from the mapped row
     * @spec.us    US-DX-002-art15-export-endpoint
     */
    @Test
    void feedsTheAccessExportWithClassifiedFields() {
        JdbcSubjectDataProvider provider = new JdbcSubjectDataProvider(
                dataSource, "customers", "id", MAPPER);
        AccessExportService export = new AccessExportService(List.of(provider));

        SubjectAccessExport dossier = export.exportSubject("alice-1");

        assertThat(dossier.fields()).hasSize(1);
        assertThat(dossier.fields().get(0).field()).isEqualTo("fullName");
        assertThat(dossier.fields().get(0).value()).isEqualTo("Alice");
    }

    /**
     * @spec.given a SQL-injection payload in the table or subject-id column name
     * @spec.when  a JDBC subject-data provider is constructed
     * @spec.then  construction fails: identifiers are whitelist-validated
     * @spec.us    US-DX-003-jdbc-spi-base-classes
     */
    @Test
    void rejectsNonIdentifierNames() {
        assertThatThrownBy(() -> new JdbcSubjectDataProvider(
                dataSource, "customers WHERE 1=1; DROP TABLE customers", "id", MAPPER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static class ProfileRow {
        @com.iambilotta.gdpr.annotations.GdprPersonalData(description = "full legal name")
        final String fullName;

        ProfileRow(String fullName) {
            this.fullName = fullName;
        }
    }
}
