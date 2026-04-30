package com.iambilotta.gdpr.starter.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncAuditSinkDecoratorTest {

    @Test
    void writesAreDispatchedToWorkerThread() throws Exception {
        CapturingSink delegate = new CapturingSink();
        AsyncAuditSinkDecorator async = new AsyncAuditSinkDecorator(delegate, 1, 16, 1000L);

        async.write(record("e1"));
        async.write(record("e2"));

        delegate.awaitCount(2, 1, TimeUnit.SECONDS);
        assertThat(delegate.records).extracting(AuditAccessRecord::eventId).containsExactly("e1", "e2");
        assertThat(async.submittedCount()).isEqualTo(2);
        assertThat(async.droppedCount()).isZero();
        assertThat(async.failedCount()).isZero();
        async.close();
    }

    @Test
    void sinkFailureIsAbsorbedAndCounted() throws Exception {
        FailingSink delegate = new FailingSink();
        AsyncAuditSinkDecorator async = new AsyncAuditSinkDecorator(delegate, 1, 16, 1000L);

        async.write(record("e1"));
        async.write(record("e2"));
        delegate.awaitInvocations(2, 1, TimeUnit.SECONDS);
        async.close();

        assertThat(async.failedCount()).isEqualTo(2);
        assertThat(async.droppedCount()).isZero();
    }

    @Test
    void saturatedQueueDropsNewestAndCounts() throws Exception {
        BlockingSink delegate = new BlockingSink();
        AsyncAuditSinkDecorator async = new AsyncAuditSinkDecorator(delegate, 1, 1, 1000L);

        async.write(record("e1"));
        delegate.awaitWorkerEntered(1, TimeUnit.SECONDS);
        async.write(record("e2"));
        async.write(record("e3"));
        async.write(record("e4"));
        delegate.release();
        async.close();

        assertThat(async.droppedCount()).isEqualTo(2L);
        assertThat(async.submittedCount()).isEqualTo(2L);
    }

    @Test
    void findBySubjectDelegatesSynchronously() {
        CapturingSink delegate = new CapturingSink();
        AsyncAuditSinkDecorator async = new AsyncAuditSinkDecorator(delegate, 1, 16, 1000L);

        delegate.records.add(record("e1"));
        List<AuditAccessRecord> result = async.findBySubject("alice", null, null);

        assertThat(result).hasSize(1);
        async.close();
    }

    @Test
    void rejectsInvalidThreadCount() {
        CapturingSink delegate = new CapturingSink();
        try {
            new AsyncAuditSinkDecorator(delegate, 0, 16, 1000L);
            assertThat(false).as("should have thrown").isTrue();
        } catch (IllegalArgumentException expected) {
            assertThat(expected).hasMessageContaining("threadCount");
        }
        try {
            new AsyncAuditSinkDecorator(delegate, 1, 0, 1000L);
            assertThat(false).as("should have thrown").isTrue();
        } catch (IllegalArgumentException expected) {
            assertThat(expected).hasMessageContaining("queueCapacity");
        }
    }

    private static AuditAccessRecord record(String eventId) {
        return new AuditAccessRecord(
                eventId, Instant.now(), "tester", "subject", "T", "m", "6(1)(b)", false);
    }

    static class CapturingSink implements AuditSink {

        final List<AuditAccessRecord> records = new CopyOnWriteArrayList<>();

        @Override
        public void write(AuditAccessRecord record) {
            records.add(record);
        }

        @Override
        public List<AuditAccessRecord> findBySubject(String subjectId, Instant from, Instant to) {
            return List.copyOf(records);
        }

        void awaitCount(int expected, long timeout, TimeUnit unit) throws InterruptedException {
            long deadline = System.nanoTime() + unit.toNanos(timeout);
            while (records.size() < expected && System.nanoTime() < deadline) {
                Thread.sleep(5L);
            }
        }
    }

    static class FailingSink implements AuditSink {

        private final AtomicInteger invocations = new AtomicInteger();

        @Override
        public void write(AuditAccessRecord record) {
            invocations.incrementAndGet();
            throw new RuntimeException("boom: " + record.eventId());
        }

        @Override
        public List<AuditAccessRecord> findBySubject(String subjectId, Instant from, Instant to) {
            return List.of();
        }

        void awaitInvocations(int expected, long timeout, TimeUnit unit) throws InterruptedException {
            long deadline = System.nanoTime() + unit.toNanos(timeout);
            while (invocations.get() < expected && System.nanoTime() < deadline) {
                Thread.sleep(5L);
            }
        }
    }

    static class BlockingSink implements AuditSink {

        private final CountDownLatch gate = new CountDownLatch(1);
        private final CountDownLatch entered = new CountDownLatch(1);

        @Override
        public void write(AuditAccessRecord record) {
            entered.countDown();
            try {
                gate.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public List<AuditAccessRecord> findBySubject(String subjectId, Instant from, Instant to) {
            return List.of();
        }

        void release() {
            gate.countDown();
        }

        void awaitWorkerEntered(long timeout, TimeUnit unit) throws InterruptedException {
            if (!entered.await(timeout, unit)) {
                throw new IllegalStateException("worker did not enter blocking sink within timeout");
            }
        }
    }
}
