# ADR-0006: ErasureHandler.entityType() and RetentionTarget.entityType() return Class&lt;?&gt;

- **Status:** accepted
- **Date:** 2026-05-02
- **Deciders:** Francesco Bilotta
- **Supersedes:** the v0.1.x and v1.0.0 SPI shape that returned `String`

## Context

Up to v1.0.0 both SPIs returned the entity FQN as a `String`:

```java
@Override
public String entityType() {
    return Customer.class.getName();
}
```

The pattern is stringly-typed: a renamed entity slips through the SPI implementation to the audit row and the retention log, surfacing as a stale FQN in production logs months later. There is no compile-time check.

## Decision

Both `ErasureHandler.entityType()` and `RetentionTarget.entityType()` return `Class<?>`. Implementations:

```java
@Override
public Class<?> entityType() {
    return Customer.class;
}
```

Wire format is preserved: `ErasureService.eraseSubject` calls `entityType().getName()` when building the `affectedByType` map, so the JSON response keys still serialise as FQN strings. The `RetentionScheduler` log line emits `entityType().getName()` for the same reason.

## Consequences

- A renamed `Customer` class fails at compile time in every `ErasureHandler` and `RetentionTarget` that referenced it. The auditor never sees a stale FQN.
- Breaking change for SPI implementers. Migration is one line per implementation. Documented in CHANGELOG `[1.1.0]` with the diff.
- Wire shape unchanged. Application authors who consume the REST endpoint or grep the logs are unaffected.

## Alternatives considered

**Keep `String` and validate FQN against the runtime classpath at startup.** Rejected: validation runs once at boot but the class can still be missing at audit time (lazy classloading), and the error surface is far worse than a compile-time check.

**`TypeRef<T>` parameterised handler.** Rejected as over-engineering: the type parameter would not buy anything beyond `Class<?>`, and the SPI signature becomes harder to read.

**Defer the change to v2.0.** Rejected because the cost of carrying the stringly-typed signature into a v2 cycle is higher than fixing it in a v1.1 with a clean migration block.

## Why this matters

The principle is "let the type system catch what it can". Stringly-typed FQN coupling is acceptable in a v0.x prototype where adoption is none and the cost of a breaking change is zero. In v1.x with a published API, every refactor that the compiler can validate is worth a one-line migration block in the CHANGELOG. We pay the cost of the breaking change once, then never again.

## References

- Refactor commit: PR #9, merged on the v1.1.0 release.
- Wire-format compatibility preserved via `entityType().getName()` at the boundary (ErasureService, RetentionScheduler logs).
- Migration is one line per `ErasureHandler` / `RetentionTarget` implementation.
