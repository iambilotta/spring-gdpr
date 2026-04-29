package com.iambilotta.gdpr.starter.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcAuditSinkTest {

    private DataSource dataSource;

    @BeforeEach
    void initDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:gdpr-audit-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        this.dataSource = ds;
    }

    @Test
    void writeAndQueryRoundTrip() {
        JdbcAuditSink sink = new JdbcAuditSink(dataSource, "gdpr_audit_access");

        Instant t0 = Instant.parse("2030-01-01T10:00:00Z");
        Instant t1 = Instant.parse("2030-01-01T10:05:00Z");
        sink.write(record("e1", t0, "alice-1", "com.x.Customer", "findById", "6(1)(b)", false));
        sink.write(record("e2", t1, "alice-1", "com.x.Customer", "findById", "6(1)(b)", true));
        sink.write(record("e3", t0, "bob-2", "com.x.Customer", "findById", "6(1)(b)", false));

        List<AuditAccessRecord> aliceRecords = sink.findBySubject(
                "alice-1",
                Instant.parse("2030-01-01T00:00:00Z"),
                Instant.parse("2030-01-02T00:00:00Z"));

        assertThat(aliceRecords).hasSize(2);
        assertThat(aliceRecords).extracting(AuditAccessRecord::eventId).containsExactly("e1", "e2");
        assertThat(aliceRecords.get(1).specialCategory()).isTrue();
    }

    @Test
    void filtersByTimeWindow() {
        JdbcAuditSink sink = new JdbcAuditSink(dataSource, "gdpr_audit_access");
        sink.write(record("old", Instant.parse("2025-01-01T00:00:00Z"), "x", "T", "m", "6(1)(a)", false));
        sink.write(record("new", Instant.parse("2030-01-01T00:00:00Z"), "x", "T", "m", "6(1)(a)", false));

        List<AuditAccessRecord> only2030 = sink.findBySubject(
                "x",
                Instant.parse("2029-12-01T00:00:00Z"),
                Instant.parse("2031-01-01T00:00:00Z"));

        assertThat(only2030).hasSize(1);
        assertThat(only2030.get(0).eventId()).isEqualTo("new");
    }

    @Test
    void rejectsTableNamesThatLookLikeSqlInjection() {
        assertThatThrownBy(() -> new JdbcAuditSink(dataSource, "audit; DROP TABLE users"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JdbcAuditSink(dataSource, "audit-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void schemaIsIdempotent() {
        JdbcAuditSink first = new JdbcAuditSink(dataSource, "gdpr_audit_access");
        JdbcAuditSink second = new JdbcAuditSink(dataSource, "gdpr_audit_access");

        second.write(record("after-second-bootstrap", Instant.now(), "x", "T", "m", "6(1)(a)", false));
        assertThat(first.findBySubject("x", null, null)).hasSize(1);
    }

    private static AuditAccessRecord record(
            String eventId, Instant at, String subjectId, String type, String member, String basis, boolean special) {
        return new AuditAccessRecord(eventId, at, "tester", subjectId, type, member, basis, special);
    }
}
