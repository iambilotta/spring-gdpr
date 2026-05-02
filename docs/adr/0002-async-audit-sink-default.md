# ADR-0002: Async AuditSink decorator on by default

- **Status:** accepted
- **Date:** 2026-04-30
- **Deciders:** Francesco Bilotta

## Context

The advisor `PersonalDataAccessAdvisor` fires on every method that reads or returns a `@GdprPersonalData` field. In a typical Spring Boot service that also serves traffic, this is the hot path of every CRUD endpoint. The audit write must not become the bottleneck.

Three sink shapes exist:

- synchronous: the advisor blocks until the sink (DB INSERT, log line) returns. Zero data loss. Worst-case latency tail equals the sink's worst-case.
- asynchronous, unbounded queue: events queue forever. Zero data loss until the JVM runs out of memory.
- asynchronous, bounded queue with drop-newest: events queue up to `queue-capacity`; beyond that, drop and increment a counter. Bounded memory, observable backpressure.

## Decision

Default is asynchronous, bounded queue with drop-newest, queue-capacity 1024, single-threaded worker. Synchronous mode opt-in via `spring.gdpr.audit.async.enabled=false`. The `dropped` counter is exposed as a Micrometer gauge so adopters can alert on `rate(dropped[5m]) > 0`.

The `AsyncAuditSinkDecorator` wraps the user-provided `AuditSink` bean transparently when the autoconfig path is taken.

## Consequences

- The request thread does not wait on the sink. Under sustained load below `queue-capacity` events/sec/pod the audit log is complete.
- Above that, events are dropped. The counter is observable, the WARN logs name the dropped event, but the audit log has gaps. Documented in `Reality check` of the README.
- Production deployments with hard zero-loss requirements can flip `async.enabled=false` and accept request-thread blocking. The library does not pretend you can have zero loss + zero blocking.
- The decision is reversible per-installation: it is a property, not a code path.

## Alternatives considered

**Synchronous by default.** Rejected because the typical Spring Boot service author who imports the starter does not want a 5x p99 latency increase as the welcome gift. They will switch async ON anyway, and the ones who genuinely need zero-loss are the minority.

**Async with unbounded queue.** Rejected because OOM under load is harder to debug than an observable drop counter.

**Drop-oldest instead of drop-newest.** A coin toss in theory. Drop-newest preserves the older audit chain (which is usually what an audit looks at: yesterday's events, not the events from the last second). Drop-oldest would silently rotate out historical evidence.
