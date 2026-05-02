# ADR-0004: ErasureHandler beans, ordered, audited

- **Status:** accepted
- **Date:** 2026-04-30
- **Deciders:** Francesco Bilotta

## Context

GDPR Article 17 requires the controller to erase a subject's personal data on request, "without undue delay". A real application keeps personal data in many tables, sometimes in many systems (CRM, mailer, search index). Erasure has dependency order: child rows go before parent rows, otherwise foreign-key constraints reject the delete. Some tables should anonymize rather than delete (statistics, audit history). Each step must be audited so the controller can demonstrate the request was honoured.

An "auto-erasure-by-reflection" mechanism that walks JPA entity graphs at runtime is appealing on the demo slide and broken in production: it cannot speak to non-JPA stores, it cannot anonymize, and it cannot decide cascade order without metadata the entity does not carry.

## Decision

Erasure is performed by user-provided `ErasureHandler` beans, one per type. The library does three things:

1. discovers all `ErasureHandler` beans and orders them by `int order()` ascending,
2. invokes each on the requested `subjectId`,
3. emits an audit row per handler (entity type, subject id, strategy, affected count, outcome).

The `DELETE /gdpr/erasure/{subjectId}` REST endpoint returns the per-handler aggregate as `{"subjectId": ..., "affectedByType": {"com.example.Customer": 1, ...}}`. If a handler partially fails the response surfaces it as a 207 Multi-Status with the per-handler outcome list.

`@GdprErasable.subjectIdField` is documentation surfaced in the DPIA, not a runtime lookup driver: see ADR-0007.

## Consequences

- Adopters write one method per table that holds personal data. The library handles ordering, audit and HTTP shape.
- The library refuses to be opinionated about persistence (JPA, JDBC, MongoDB, external API). The handler pattern works with whatever the adopter's stack already runs.
- The library cannot guarantee "this subject's data is fully erased" because that depends on whether the adopter wrote a handler for every table. We document this clearly: the audit row says what the library did, not what the system did.
- The cascade order is the adopter's responsibility, declared via `int order()`. Wrong order shows up as a database FK violation at the first call, which is the right time to fail.

## Alternatives considered

**Reflection-driven cascade walk** as suggested above. Rejected because non-JPA stores are out of reach and anonymize-vs-delete is a semantic decision the framework cannot make.

**Single user-provided "erase everything" bean.** Rejected because it gives up the per-type audit row, which is exactly what an Article 17 review wants to see.

**Auto-discovered Spring Data repositories.** Rejected for the same reason as reflection-driven: it traps the library in a JPA-only world and gives up anonymize semantics.

## Why this matters

The principle is "the library knows orchestration; the application knows the data". Reflection-driven cascades work on slide decks and break the moment a real adopter has Mongo, Redis, or an external CRM. By making `ErasureHandler` user-owned we trade some adopter typing for a library that survives every persistence shape. Article 17 audits care about evidence that the right tables were touched; they do not care that the library walked them automatically.

## References

- `ErasureHandler` SPI: `spring-gdpr-starter/src/main/java/.../erasure/`.
- `ErasureService` orchestrator: same package. Tests in `spring-gdpr-starter/src/test/java/.../erasure/ErasureServiceTest.java`.
- Demo handler (single-table delete): see `examples/quickstart-postgres/.../CustomerErasureHandler.java`.
