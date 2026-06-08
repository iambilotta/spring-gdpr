# Architecture Decision Records

Each file under `docs/adr/` records a non-trivial design decision in a fixed format ([Michael Nygard's ADR template](https://www.cognitect.com/blog/2011/11/15/documenting-architecture-decisions)). Status, date, context, decision, consequences, alternatives. Short.

The point is honesty: a future contributor (or future me) can re-evaluate a decision with the full context that produced it, instead of guessing from the code.

## Index

- [ADR-0001](0001-annotations-as-source-of-truth.md): Annotations as the source of truth for GDPR evidence.
- [ADR-0002](0002-async-audit-sink-default.md): Async `AuditSink` decorator on by default.
- [ADR-0003](0003-build-time-and-runtime-as-two-products.md): Build-time generator and runtime starter as two separately adoptable halves.
- [ADR-0004](0004-erasure-handler-orchestration.md): `ErasureHandler` beans, ordered, audited.
- [ADR-0005](0005-retention-via-spring-scheduled.md): Retention sweep via `@Scheduled`, configurable cron.
- [ADR-0006](0006-typed-class-on-spi.md): `ErasureHandler.entityType()` returns `Class<?>`.
- [ADR-0007](0007-subjectidfield-is-documentation-only.md): `@GdprErasable.subjectIdField` is documentation, not a runtime lookup driver.
- [ADR-0008](0008-consent-and-portability-deferred.md) [proposed]: Article 7 consent management and Article 20 portability deferred to a future minor.
- [ADR-0009](0009-append-only-erasure-crypto-shredding.md) [proposed]: Append-only-safe erasure via crypto-shredding (per-subject key, erasure = key drop). One-way door; RED tests committed `@Disabled` pending human GREEN approval.

## How to add an ADR

1. Copy `0000-template.md` to the next available number, kebab-case the title.
2. Set status to `proposed`. Open a PR. After review, flip to `accepted`.
3. If a later ADR supersedes an earlier one, reference both ways (`Superseded by`, `Supersedes`).
4. Never delete an old ADR. Mark it `superseded` or `deprecated` and link the replacement.
