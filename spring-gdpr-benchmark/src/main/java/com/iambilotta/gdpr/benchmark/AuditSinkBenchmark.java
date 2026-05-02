/*
 * Copyright 2026 Francesco Bilotta
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package com.iambilotta.gdpr.benchmark;

import com.iambilotta.gdpr.starter.audit.AsyncAuditSinkDecorator;
import com.iambilotta.gdpr.starter.audit.AuditAccessRecord;
import com.iambilotta.gdpr.starter.audit.AuditSink;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Pinpoints the per-call cost of the audit sink in three configurations:
 *
 * <ul>
 *   <li>{@code sync-noop}: synchronous direct call to a no-op sink. Lower bound on the
 *       cost the advisor adds beyond the sink itself.</li>
 *   <li>{@code async-noop}: {@link AsyncAuditSinkDecorator} on top of a no-op sink, queue
 *       capacity 1024, single worker. The default v1.x configuration with the trivial
 *       downstream.</li>
 *   <li>{@code async-blocking}: {@code AsyncAuditSinkDecorator} on top of a sink that
 *       blocks 1ms per write (simulating a JDBC INSERT). Shows the request-thread
 *       latency stays bounded by the queue, not by the slow sink.</li>
 * </ul>
 *
 * <p>Run: {@code java -jar target/benchmarks.jar AuditSinkBenchmark}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class AuditSinkBenchmark {

    @Param({"sync-noop", "async-noop", "async-blocking"})
    public String mode;

    private AuditSink sink;
    private AsyncAuditSinkDecorator asyncDecorator;
    private AuditAccessRecord record;

    @Setup
    public void setup() {
        AuditSink downstream = switch (mode) {
            case "async-blocking" -> new BlockingNoopSink(1L);
            default -> new NoopSink();
        };
        switch (mode) {
            case "sync-noop" -> {
                this.sink = downstream;
                this.asyncDecorator = null;
            }
            case "async-noop", "async-blocking" -> {
                this.asyncDecorator = new AsyncAuditSinkDecorator(downstream, 1, 1024, 5_000L);
                this.sink = this.asyncDecorator;
            }
            default -> throw new IllegalArgumentException(mode);
        }
        this.record = new AuditAccessRecord(
                UUID.randomUUID().toString(),
                Instant.now(),
                "user-1",
                "subject-x",
                "com.example.Customer",
                "read",
                "6(1)(b)",
                false);
    }

    @TearDown
    public void tearDown() {
        if (asyncDecorator != null) {
            try { asyncDecorator.close(); } catch (Exception ignored) { }
        }
    }

    @Benchmark
    public void write() {
        sink.write(record);
    }

    private static final class NoopSink implements AuditSink {
        @Override public void write(AuditAccessRecord r) { /* noop */ }
        @Override public List<AuditAccessRecord> findBySubject(String s, Instant a, Instant b) { return List.of(); }
    }

    private static final class BlockingNoopSink implements AuditSink {
        private final long blockMillis;
        BlockingNoopSink(long blockMillis) { this.blockMillis = blockMillis; }
        @Override
        public void write(AuditAccessRecord r) {
            try { Thread.sleep(blockMillis); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        @Override public List<AuditAccessRecord> findBySubject(String s, Instant a, Instant b) { return List.of(); }
    }
}
