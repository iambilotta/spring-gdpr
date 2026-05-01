package com.iambilotta.gdpr.demo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.iambilotta.gdpr.starter.audit.AuditAccessRecord;
import com.iambilotta.gdpr.starter.audit.AuditSink;
import com.iambilotta.gdpr.starter.erasure.ErasureReport;
import com.iambilotta.gdpr.starter.erasure.ErasureService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GdprIntegrationTest {

    @Autowired
    CustomerRepository repository;

    @Autowired
    ErasureService erasureService;

    @Autowired
    RecordingAuditSink auditSink;

    @BeforeEach
    void seed() {
        auditSink.clear();
        repository.save(buildCustomer("alice-1", "Alice", "alice@example.com", "AT-12345"));
        repository.save(buildCustomer("bob-2", "Bob", "bob@example.com", "BT-67890"));
    }

    @Test
    void advisorEmitsAuditRecordOnPersonalDataAccess() {
        repository.findBySubjectId("alice-1");

        assertThat(auditSink.records).hasSize(1);
        AuditAccessRecord record = auditSink.records.get(0);
        assertThat(record.targetType()).isEqualTo(CustomerRepository.class.getName());
        assertThat(record.targetMember()).isEqualTo("findBySubjectId");
        assertThat(record.actor()).isEqualTo("system");
    }

    @Test
    void erasureRemovesCustomerAcrossAllHandlers() {
        ErasureReport report = erasureService.eraseSubject("alice-1");

        assertThat(report.totalAffected()).isEqualTo(1);
        assertThat(report.affectedByType()).containsKey(Customer.class.getName());
        assertThat(repository.findBySubjectId("alice-1")).isEmpty();
        assertThat(repository.findBySubjectId("bob-2")).isPresent();
    }

    private static Customer buildCustomer(String id, String name, String email, String taxId) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setFullName(name);
        customer.setEmail(email);
        customer.setTaxId(taxId);
        customer.setCreatedAt(Instant.now());
        return customer;
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        RecordingAuditSink recordingAuditSink() {
            return new RecordingAuditSink();
        }
    }

    static class RecordingAuditSink implements AuditSink {

        final List<AuditAccessRecord> records = new ArrayList<>();

        @Override
        public void write(AuditAccessRecord record) {
            records.add(record);
        }

        @Override
        public List<AuditAccessRecord> findBySubject(String subjectId, Instant from, Instant to) {
            return records.stream().filter(r -> subjectId.equals(r.subjectId())).toList();
        }

        void clear() {
            records.clear();
        }
    }
}
