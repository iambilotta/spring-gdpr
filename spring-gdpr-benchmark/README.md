# spring-gdpr-benchmark

JMH micro-benchmark for the audit sink hot path. **Not distributed**: this module is local-only, excluded from `mvn install`/`deploy`, and does not appear on JitPack.

## What we measure

`AuditSinkBenchmark.write` — per-call cost of the `AuditSink.write(record)` path the AOP advisor fires on every `@GdprPersonalData` access. Three modes:

| Mode | Configuration | What it pins |
|---|---|---|
| `sync-noop` | direct call to a no-op sink | lower-bound on the cost the advisor adds beyond the sink itself |
| `async-noop` | `AsyncAuditSinkDecorator` over a no-op sink, queue 1024, 1 worker | default v1.x configuration with a trivial downstream |
| `async-blocking` | `AsyncAuditSinkDecorator` over a sink that blocks 1 ms per write (simulating a JDBC INSERT) | request-thread cost stays bounded by the queue, not by the slow sink |

## How to run

From the reactor root:

```bash
./mvnw -B -DskipTests -pl spring-gdpr-benchmark -am package
java -jar spring-gdpr-benchmark/target/benchmarks.jar AuditSinkBenchmark
```

JSON output:

```bash
java -jar spring-gdpr-benchmark/target/benchmarks.jar -rf json -rff results/$(date -I)-$(uname -m).json
```

## Reference numbers

Captured 2026-05-02 on Corretto 25.0.3 (Linux x86_64), `-wi 2 -i 3 -f 1`. Take as ballpark.

| Benchmark | Mode | Score | Unit |
|---|---|---|---|
| `AuditSinkBenchmark.write` (sync-noop) | avg | 0.001 ± 0.001 | µs/op |
| `AuditSinkBenchmark.write` (async-noop) | avg | 0.26 ± 1.4 | µs/op |
| `AuditSinkBenchmark.write` (async-blocking, 1ms downstream) | avg | 2.08 ± 1.1 | µs/op |

Raw JSON: [`results/2026-05-02-jdk25-corretto.json`](results/2026-05-02-jdk25-corretto.json).

## What these numbers mean for an adopter

- The sink boundary itself adds a single nanosecond on a no-op sync path: the AOP advisor and the record allocation are not the bottleneck. Your slow sink is what slows you down.
- The async decorator adds ~250 ns per write to the request thread, regardless of how fast or slow the downstream sink is. **That is the load-bearing claim of ADR-0002**: the request thread does not wait on the sink. When the downstream blocks for 1 ms, the request thread still returns in ~2 µs because the work was offloaded to the worker.
- Above sustained 1024 events/sec/pod (the default queue capacity) the queue saturates and `dropped` increments. Bump `spring.gdpr.audit.async.queue-capacity` or shard your sink. Documented in the README "Reality check".

These are reference numbers; your hardware will produce different absolutes. Re-run on your target before sizing.

## Why this module exists

A library that says "async by default, the request thread is never blocked" without numbers is selling intuition. The async-blocking mode here is the proof: a 1 ms downstream does not appear on the request thread side. The cost of running the harness on your hardware is two minutes; the cost of trusting our blanket claim is finding out at production scale.
