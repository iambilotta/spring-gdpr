package com.iambilotta.gdpr.starter.retention;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.iambilotta.gdpr.annotations.GdprRetention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract for the ready-made JDBC retention target: sweep rows whose timestamp column is older than
 * the cutoff, either deleting them or nulling the personal-data columns (anonymize). One constructor
 * call instead of hand-written sweep SQL; the raw {@link RetentionTarget} stays as the escape hatch.
 */
class JdbcRetentionTargetTest {

    static class Customer {}

    private DataSource dataSource;
    private JdbcTemplate jdbc;

    @BeforeEach
    void initDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:gdpr-retention-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        this.dataSource = ds;
        this.jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE customers ("
                + "id VARCHAR(64) PRIMARY KEY, full_name VARCHAR(255), created_at TIMESTAMP)");
    }

    private Instant cutoff() {
        return Instant.parse("2026-01-01T00:00:00Z");
    }

    private void seed() {
        jdbc.update("INSERT INTO customers VALUES ('old-1', 'Old One', ?)",
                Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")));
        jdbc.update("INSERT INTO customers VALUES ('old-2', 'Old Two', ?)",
                Timestamp.from(Instant.parse("2021-06-01T00:00:00Z")));
        jdbc.update("INSERT INTO customers VALUES ('fresh-1', 'Fresh', ?)",
                Timestamp.from(Instant.parse("2030-01-01T00:00:00Z")));
    }

    /**
     * @spec.given two rows older than the cutoff and one fresh, with a DELETE strategy
     * @spec.when  the JDBC retention target counts then applies the due sweep
     * @spec.then  countDue and applyDue both report two and only the fresh row survives
     * @spec.us    US-DX-003-jdbc-spi-base-classes
     */
    @Test
    void deletesRowsOlderThanCutoffAndCountsThem() {
        seed();
        JdbcRetentionTarget target = new JdbcRetentionTarget(
                dataSource, Customer.class, "customers", "created_at",
                Duration.ofDays(365 * 5), GdprRetention.Strategy.DELETE);

        assertThat(target.countDue(cutoff())).isEqualTo(2);
        assertThat(target.applyDue(cutoff())).isEqualTo(2);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM customers", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT id FROM customers", String.class)).isEqualTo("fresh-1");
    }

    /**
     * @spec.given two rows older than the cutoff, with an ANONYMIZE strategy nulling full_name
     * @spec.when  the JDBC retention target applies the due sweep
     * @spec.then  the old rows stay but their personal-data column is nulled; the fresh row is untouched
     * @spec.us    US-DX-003-jdbc-spi-base-classes
     */
    @Test
    void anonymizesPersonalColumnsOnDueRowsInsteadOfDeleting() {
        seed();
        JdbcRetentionTarget target = new JdbcRetentionTarget(
                dataSource, Customer.class, "customers", "created_at",
                Duration.ofDays(365 * 5), GdprRetention.Strategy.ANONYMIZE, "full_name");

        assertThat(target.applyDue(cutoff())).isEqualTo(2);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM customers", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM customers WHERE full_name IS NULL", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT full_name FROM customers WHERE id = 'fresh-1'", String.class)).isEqualTo("Fresh");
    }

    /**
     * @spec.given a SQL-injection payload in the table, timestamp, or personal-data column name
     * @spec.when  a JDBC retention target is constructed
     * @spec.then  construction fails: every identifier is whitelist-validated
     * @spec.us    US-DX-003-jdbc-spi-base-classes
     */
    @Test
    void rejectsNonIdentifierNames() {
        assertThatThrownBy(() -> new JdbcRetentionTarget(
                dataSource, Customer.class, "customers", "created_at; DROP TABLE customers",
                Duration.ofDays(1), GdprRetention.Strategy.DELETE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JdbcRetentionTarget(
                dataSource, Customer.class, "customers", "created_at",
                Duration.ofDays(1), GdprRetention.Strategy.ANONYMIZE, "full_name) = (1--"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
