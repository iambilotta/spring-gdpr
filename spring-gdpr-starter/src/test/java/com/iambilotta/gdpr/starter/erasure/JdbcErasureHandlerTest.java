package com.iambilotta.gdpr.starter.erasure;

import java.util.UUID;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.iambilotta.gdpr.annotations.GdprErasable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract for the ready-made JDBC erasure handler: the 80% case (delete rows of one table whose
 * subject-id column matches) becomes a constructor call, not hand-written SQL. The raw
 * {@link ErasureHandler} interface stays as the escape hatch for non-JDBC stores.
 */
class JdbcErasureHandlerTest {

    static class Customer {}

    private DataSource dataSource;
    private JdbcTemplate jdbc;

    @BeforeEach
    void initDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:gdpr-erasure-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        this.dataSource = ds;
        this.jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE customers ("
                + "pk VARCHAR(64) PRIMARY KEY, subject_id VARCHAR(64), full_name VARCHAR(255))");
    }

    /**
     * @spec.given a customers table with three rows, two owned by the subject
     * @spec.when  the JDBC erasure handler deletes by the subject-id column
     * @spec.then  it returns the affected-row count and only the subject's rows are gone
     * @spec.us    US-DX-003-jdbc-spi-base-classes
     */
    @Test
    void deletesRowsMatchingTheSubjectIdColumnAndReturnsTheCount() {
        // two rows share the same subject id via the subject column (not the PK):
        jdbc.update("INSERT INTO customers VALUES ('row-1', 'alice-1', 'Alice')");
        jdbc.update("INSERT INTO customers VALUES ('row-2', 'alice-1', 'Alice copy')");
        jdbc.update("INSERT INTO customers VALUES ('row-3', 'bob-2', 'Bob')");

        JdbcErasureHandler handler = new JdbcErasureHandler(
                dataSource, Customer.class, "customers", "subject_id", GdprErasable.Strategy.DELETE);

        int affected = handler.erase("alice-1");

        assertThat(affected).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customers", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT subject_id FROM customers", String.class)).isEqualTo("bob-2");
    }

    /**
     * @spec.given a JDBC erasure handler built for a domain type
     * @spec.when  the orchestrator reads its metadata
     * @spec.then  it reports the typed entity class and configured strategy (used in the audit row)
     * @spec.us    US-DX-003-jdbc-spi-base-classes
     */
    @Test
    void exposesTheTypedEntityClassAndStrategy() {
        JdbcErasureHandler handler = new JdbcErasureHandler(
                dataSource, Customer.class, "customers", "id", GdprErasable.Strategy.DELETE);

        assertThat(handler.entityType()).isEqualTo(Customer.class);
        assertThat(handler.strategy()).isEqualTo(GdprErasable.Strategy.DELETE);
    }

    /**
     * @spec.given a table or column name carrying a SQL-injection payload
     * @spec.when  a JDBC erasure handler is constructed
     * @spec.then  construction fails: identifiers are whitelist-validated, not interpolated blindly
     * @spec.us    US-DX-003-jdbc-spi-base-classes
     */
    @Test
    void rejectsNonIdentifierTableOrColumnNames() {
        assertThatThrownBy(() -> new JdbcErasureHandler(
                dataSource, Customer.class, "customers; DROP TABLE customers", "id",
                GdprErasable.Strategy.DELETE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JdbcErasureHandler(
                dataSource, Customer.class, "customers", "id = id OR 1=1",
                GdprErasable.Strategy.DELETE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
