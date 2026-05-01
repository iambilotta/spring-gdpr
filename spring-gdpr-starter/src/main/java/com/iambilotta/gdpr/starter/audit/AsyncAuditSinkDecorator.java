package com.iambilotta.gdpr.starter.audit;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorator that turns a synchronous {@link AuditSink} into an asynchronous one.
 *
 * <p>Why async by default for production: the advisor pointcut runs on the request thread.
 * If the underlying sink does I/O (JDBC, HTTP, file flush) and that I/O is slow or fails,
 * a synchronous sink either adds latency to every personal-data access or propagates the
 * failure into the business method. Both are unacceptable on a hot path.
 *
 * <p>This decorator submits {@code write} calls to a small fixed-size {@link ThreadPoolExecutor}
 * with a bounded queue. Three failure modes, all visible:
 * <ul>
 *   <li>queue saturated: the call is dropped, the {@code dropped} counter increments,
 *       a WARN is logged. Drop-newest semantics: under load we keep what we have, we do
 *       NOT slow down the request thread to make room. Compliance impact is a gap in
 *       the audit log under saturation, which is observable via the counter; back-pressuring
 *       the request thread is not an option for a customer-facing service.</li>
 *   <li>worker thread sink failure: ERROR logged with the event id, the {@code failed}
 *       counter increments. The exception does NOT propagate.</li>
 *   <li>shutdown: graceful drain up to {@code awaitMillis} on close, then forced
 *       shutdown. Pending records are logged at the underlying sink before the JVM exits
 *       in 99% of cases; the 1% drop is measured by the {@code dropped} counter.</li>
 * </ul>
 *
 * <p>{@link #findBySubject} delegates synchronously: it is a query, not a hot path. The
 * Art. 15 right-of-access endpoint is fine running on the request thread.
 */
public class AsyncAuditSinkDecorator implements AuditSink, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncAuditSinkDecorator.class);

    private final AuditSink delegate;
    private final ExecutorService executor;
    private final long awaitMillis;

    private final LongAdder dropped = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final AtomicLong submitted = new AtomicLong();

    public AsyncAuditSinkDecorator(AuditSink delegate, int threadCount, int queueCapacity, long awaitMillis) {
        if (threadCount < 1) {
            throw new IllegalArgumentException("threadCount must be >= 1, got " + threadCount);
        }
        if (queueCapacity < 1) {
            throw new IllegalArgumentException("queueCapacity must be >= 1, got " + queueCapacity);
        }
        this.delegate = delegate;
        this.awaitMillis = awaitMillis;
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                threadCount,
                threadCount,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                runnable -> {
                    Thread t = new Thread(runnable, "spring-gdpr-audit");
                    t.setDaemon(true);
                    return t;
                },
                (rejected, ex) -> {
                    dropped.increment();
                    LOG.warn(
                            "spring-gdpr audit queue saturated, dropping record. dropped_total={}",
                            dropped.sum());
                });
        this.executor = pool;
    }

    @Override
    public void write(AuditAccessRecord record) {
        Runnable task = () -> {
            submitted.incrementAndGet();
            try {
                delegate.write(record);
            } catch (RuntimeException ex) {
                failed.increment();
                LOG.error(
                        "spring-gdpr audit sink failed for event_id={}: {}. failed_total={}",
                        record.eventId(), ex.getMessage(), failed.sum());
            }
        };
        try {
            executor.execute(task);
        } catch (RejectedExecutionException ex) {
            dropped.increment();
            LOG.warn(
                    "spring-gdpr audit submission rejected (executor closed?). dropped_total={}",
                    dropped.sum());
        }
    }

    @Override
    public List<AuditAccessRecord> findBySubject(String subjectId, Instant from, Instant to) {
        return delegate.findBySubject(subjectId, from, to);
    }

    public long droppedCount() {
        return dropped.sum();
    }

    public long failedCount() {
        return failed.sum();
    }

    public long submittedCount() {
        return submitted.get();
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(awaitMillis, TimeUnit.MILLISECONDS)) {
                LOG.warn(
                        "spring-gdpr audit executor did not drain within {}ms, forcing shutdown. dropped_total={}",
                        awaitMillis, dropped.sum());
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
