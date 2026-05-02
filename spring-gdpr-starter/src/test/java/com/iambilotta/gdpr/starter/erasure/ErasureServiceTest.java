package com.iambilotta.gdpr.starter.erasure;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.iambilotta.gdpr.annotations.GdprErasable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ErasureServiceTest {

    static class Address {}
    static class Customer {}
    static class Invoice {}

    @Test
    void invokesHandlersInOrderAscending() {
        List<String> invocationOrder = new ArrayList<>();
        FakeHandler addresses = new FakeHandler(Address.class, 50, invocationOrder, 0);
        FakeHandler customers = new FakeHandler(Customer.class, 100, invocationOrder, 0);
        FakeHandler invoices = new FakeHandler(Invoice.class, 25, invocationOrder, 0);

        ErasureService service = new ErasureService(List.of(addresses, customers, invoices));
        service.eraseSubject("subject-x");

        assertThat(invocationOrder).containsExactly(
                Invoice.class.getName(),
                Address.class.getName(),
                Customer.class.getName());
    }

    @Test
    void aggregatesAffectedCountsByType() {
        FakeHandler customers = new FakeHandler(Customer.class, 100, new ArrayList<>(), 1);
        FakeHandler invoices = new FakeHandler(Invoice.class, 50, new ArrayList<>(), 3);

        ErasureService service = new ErasureService(List.of(customers, invoices));
        ErasureReport report = service.eraseSubject("subject-x");

        assertThat(report.subjectId()).isEqualTo("subject-x");
        assertThat(report.totalAffected()).isEqualTo(4);
        assertThat(report.affectedByType())
                .containsEntry(Customer.class.getName(), 1)
                .containsEntry(Invoice.class.getName(), 3);
    }

    @Test
    void rejectsBlankSubjectId() {
        ErasureService service = new ErasureService(List.of());

        assertThatThrownBy(() -> service.eraseSubject(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.eraseSubject(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class FakeHandler implements ErasureHandler {

        private final Class<?> type;
        private final int order;
        private final List<String> log;
        private final int affected;

        FakeHandler(Class<?> type, int order, List<String> log, int affected) {
            this.type = type;
            this.order = order;
            this.log = log;
            this.affected = affected;
        }

        @Override
        public Class<?> entityType() {
            return type;
        }

        @Override
        public GdprErasable.Strategy strategy() {
            return GdprErasable.Strategy.DELETE;
        }

        @Override
        public int erase(String subjectId) {
            log.add(type.getName());
            return affected;
        }

        @Override
        public int order() {
            return order;
        }
    }
}
