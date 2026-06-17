package com.iambilotta.gdpr.starter.erasure;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.iambilotta.gdpr.annotations.GdprErasable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Specification for the post-erasure SPI (issue #37): after {@link ErasureService#eraseSubject}
 * assembles its report, every registered {@link ErasureListener} is invoked exactly once with that
 * report, so event-sourced / CQRS consumers can rebuild projections or invalidate caches that held
 * a now-dangling forgettable-payload reference (ADR-0010). Backward compatible: with no listener it
 * is a no-op.
 */
class ErasureListenerTest {

    static class Customer {}

    /**
     * @spec.given an ErasureService with one registered ErasureListener
     * @spec.when  a subject is erased
     * @spec.then  the listener is invoked exactly once with the assembled report
     * @spec.us    REQ-GDPR-023
     */
    @Test
    void invokesTheListenerOnceWithTheReportAfterErasure() {
        CapturingListener listener = new CapturingListener();
        ErasureService service = new ErasureService(
                List.of(new FakeHandler(Customer.class, 100, 2)),
                List.of(listener));

        ErasureReport report = service.eraseSubject("alice-1");

        assertThat(listener.received).hasSize(1);
        ErasureReport seen = listener.received.get(0);
        assertThat(seen).isSameAs(report);
        assertThat(seen.subjectId()).isEqualTo("alice-1");
        assertThat(seen.affectedByType()).containsEntry(Customer.class.getName(), 2);
    }

    /**
     * @spec.given an ErasureService with no listener registered (the legacy constructor)
     * @spec.when  a subject is erased
     * @spec.then  the erasure still succeeds and returns its report (backward compatible no-op)
     * @spec.us    REQ-GDPR-023
     */
    @Test
    void isANoOpAndBackwardCompatibleWhenNoListenerIsRegistered() {
        ErasureService service = new ErasureService(List.of(new FakeHandler(Customer.class, 100, 1)));

        ErasureReport report = service.eraseSubject("alice-1");

        assertThat(report.totalAffected()).isEqualTo(1);
    }

    /**
     * @spec.given an ErasureService with several listeners, one of which throws
     * @spec.when  a subject is erased
     * @spec.then  the failure is surfaced (not swallowed) and the other listeners still ran: the
     *             erasure itself already happened, a listener fault never silently un-erases it
     * @spec.us    REQ-GDPR-023
     */
    @Test
    void surfacesAListenerFailureWithoutSwallowingOrUnErasing() {
        CapturingListener before = new CapturingListener();
        ErasureListener boom = report -> {
            throw new IllegalStateException("downstream rebuild failed");
        };
        CapturingListener after = new CapturingListener();
        ErasureService service = new ErasureService(
                List.of(new FakeHandler(Customer.class, 100, 1)),
                List.of(before, boom, after));

        assertThatThrownBy(() -> service.eraseSubject("alice-1"))
                .isInstanceOf(ErasureListenerException.class)
                .hasMessageContaining("alice-1");

        assertThat(before.received).hasSize(1); // ran before the faulty one
        assertThat(after.received).hasSize(1); // a faulty listener does not block the others
    }

    private static final class CapturingListener implements ErasureListener {
        private final List<ErasureReport> received = new ArrayList<>();

        @Override
        public void onSubjectErased(ErasureReport report) {
            received.add(report);
        }
    }

    private static final class FakeHandler implements ErasureHandler {
        private final Class<?> type;
        private final int order;
        private final int affected;

        FakeHandler(Class<?> type, int order, int affected) {
            this.type = type;
            this.order = order;
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
            return affected;
        }

        @Override
        public int order() {
            return order;
        }
    }
}
