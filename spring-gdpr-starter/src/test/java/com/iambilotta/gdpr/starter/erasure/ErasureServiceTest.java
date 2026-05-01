package com.iambilotta.gdpr.starter.erasure;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.iambilotta.gdpr.annotations.GdprErasable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ErasureServiceTest {

    @Test
    void invokesHandlersInOrderAscending() {
        List<String> invocationOrder = new ArrayList<>();
        FakeHandler addresses = new FakeHandler("Address", 50, invocationOrder, 0);
        FakeHandler customers = new FakeHandler("Customer", 100, invocationOrder, 0);
        FakeHandler invoices = new FakeHandler("Invoice", 25, invocationOrder, 0);

        ErasureService service = new ErasureService(List.of(addresses, customers, invoices));
        service.eraseSubject("subject-x");

        assertThat(invocationOrder).containsExactly("Invoice", "Address", "Customer");
    }

    @Test
    void aggregatesAffectedCountsByType() {
        FakeHandler customers = new FakeHandler("Customer", 100, new ArrayList<>(), 1);
        FakeHandler invoices = new FakeHandler("Invoice", 50, new ArrayList<>(), 3);

        ErasureService service = new ErasureService(List.of(customers, invoices));
        ErasureReport report = service.eraseSubject("subject-x");

        assertThat(report.subjectId()).isEqualTo("subject-x");
        assertThat(report.totalAffected()).isEqualTo(4);
        assertThat(report.affectedByType())
                .containsEntry("Customer", 1)
                .containsEntry("Invoice", 3);
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

        private final String name;
        private final int order;
        private final List<String> log;
        private final int affected;

        FakeHandler(String name, int order, List<String> log, int affected) {
            this.name = name;
            this.order = order;
            this.log = log;
            this.affected = affected;
        }

        @Override
        public String entityType() {
            return name;
        }

        @Override
        public GdprErasable.Strategy strategy() {
            return GdprErasable.Strategy.DELETE;
        }

        @Override
        public int erase(String subjectId) {
            log.add(name);
            return affected;
        }

        @Override
        public int order() {
            return order;
        }
    }
}
