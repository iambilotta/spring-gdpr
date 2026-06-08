# Requirements | spring-gdpr (the library's own GDPR contract)

> **Layer**: governance / **to-be** (hand-authored EARS). This file declares what the
> library *shall* do, in normative EARS, with MoSCoW priority and a trace to the test or
> class that proves it. The **as-is** view (what the code does *right now*) is the
> test-as-spec catalog generated from the `@Test` javadoc; until `tracegate` lands in this
> repo (see below) that generated catalog does not exist yet, so the `trace-to` of each
> `stato: implementato` requirement points at the **verified test/class** by hand. The
> **gap between this to-be and the implemented set is the backlog** (every `stato: proposto`).
>
> This is the requirements view *of the library itself*, complementary to the per-adopter
> requirements a consumer writes (e.g. the housetree `gest` GDPR requirements that dogfood
> this library). It does not replace the README (the marketing + usage surface) nor the
> ADRs (the decision records); it is the normative spec those two informally imply.

## Context

`spring-gdpr` turns `@Gdpr*` annotations on domain types into GDPR evidence: a build-time
**Article 30 ROPA** + **Article 35 DPIA** scaffold, a runtime **Article 15 access audit
log**, an **Article 17 erasure** orchestration shell, and **Article 5(1)(e) retention**
enforcement. Apache-2.0, JitPack-distributed, no SaaS, no data egress.

**Source-of-truth split** (governance layered, strongest first):
- the **annotations** are the source of truth for the *evidence* (ADR-0001);
- the **ADRs** (`docs/adr/`) are the source of truth for the *decisions*;
- **this file** is the source of truth for the *requirements* (the normative COSA);
- the **tests** are the source of truth for the *as-built reality* (each `@Test` is a requirement).

**Scope boundary**: a requirement here states the property the library must hold (the WHAT).
The *strategy* that satisfies an append-only-safe property (crypto-shredding vs
sentinel-at-projection) is a **design** decision and belongs in an ADR, not here (see
REQ-GDPR-016). The library deliberately does NOT cover Article 7 consent and Article 20
portability today (ADR-0008); those appear below as `proposto` so the gap is visible in the
same catalog, not hidden.

**As-is snapshot (2026-06-08, verified against the clone)**: the build-time generators, the
runtime audit advisor + async sink, the erasure orchestration shell, and the retention
scheduler are all implemented and tested. **Append-only-safe erasure** (crypto-shredding for
event-sourced stores, REQ-GDPR-016/017, ADR-0009) is now implemented (`erasure/crypto/`). The
two remaining to-be gaps the library itself flags are the
**IDENTITY/CONTACT/FINANCIAL category taxonomy** (today `@GdprPersonalData` carries only a
`specialCategory` boolean) and **structured-log PII redaction**. Consent (Art.7) and
portability (Art.20) are the two deferred-by-ADR gaps.

---

## Requirements

### REQ-GDPR-001 | Personal-data marked at the source, machine-readable
- categoria: funzionale
- priorità: M
- fonte: GDPR Art.4(1) + Art.30; ADR-0001 (annotations as source of truth)
- rationale: without a machine-readable mark on the data there is no inventory, no audit, no DPIA, no selective erasure; it is the precondition of every other requirement
- trace-to: `GdprPersonalData.java` (`@Target FIELD/PARAMETER/TYPE/METHOD`, `specialCategory`); surfaced by `GdprAnnotationProcessor`; access path proven by `PersonalDataAccessAdvisorTest#deliveriesReachTheSink`
- stato: implementato

WHEN a domain field, parameter, type, or method carries personal data, the system SHALL let
the developer mark it with `@GdprPersonalData` so the same symbol drives both the build-time
evidence and the runtime audit (single source of truth, never split).

### REQ-GDPR-002 | ROPA (Art.30) generated at build, never hand-maintained
- categoria: funzionale
- priorità: M
- fonte: GDPR Art.30; ADR-0001
- rationale: a hand-maintained inventory drifts the moment the next feature ships; it must be derived from the code at every compile (living documentation)
- trace-to: `GdprAnnotationProcessor.java` (writes `spring.gdpr/ropa.csv` to `SOURCE_OUTPUT`); CLI goal `RopaMojo`
- stato: implementato

WHEN the project is compiled, the system SHALL generate the Records of Processing Activities
(ROPA) by enumerating every annotated type, with no manual intervention.

### REQ-GDPR-003 | DPIA (Art.35) scaffold generated from the annotations
- categoria: funzionale
- priorità: S
- fonte: GDPR Art.35; ADR-0001
- rationale: the DPIA scaffold must follow the code, not be re-written by hand each audit; special-category data must raise its visibility automatically
- trace-to: `GdprAnnotationProcessor.java` (writes `spring.gdpr/dpia.md`); CLI goal `DpiaMojo`; `specialCategory` flag on `GdprPersonalData`
- stato: implementato

WHEN a type carries personal data (especially special-category data), the system SHALL emit a
DPIA scaffold listing the personal-data access points and flagging Article 9/10 data for the
necessity-and-proportionality assessment.

### REQ-GDPR-004 | Explicit lawful basis per processing (Art.6/9/10)
- categoria: technology-constraint
- priorità: S
- fonte: GDPR Art.6, Art.9, Art.10
- rationale: every processing record must map to a declared, auditable lawful basis; special and criminal-conviction data compose onto the ordinary basis
- trace-to: `GdprLegalBasis.java` (`LawfulBasis`/`Art9Condition`/`Art10Basis` enums, `article()` override); `LegalBasisMappingTest#explicitArticleOverrideWins`, `#specialCategoryWithArt9ProducesCompositeReference`, `#criminalConvictionsProducesArt10Reference`
- stato: implementato

WHEN a processing record is declared, the system SHALL associate the applicable GDPR lawful
basis (Art.6, optionally composed with Art.9 or Art.10) and surface it in the ROPA and in the
audit row.

### REQ-GDPR-005 | Missing lawful basis is surfaced, not silently dropped
- categoria: technology-constraint
- priorità: S
- fonte: GDPR Art.5(2) accountability; ADR-0001
- rationale: a ROPA record without a lawful basis is a compliance hole; the build must make it visible rather than emit a blank cell
- trace-to: `LegalBasisMappingTest#specialCategoryWithoutArt9OrArt10FlagsMissing`, `#nullAnnotationReturnsNull`
- stato: implementato

WHEN a processing record carries special-category data but no Article 9/10 condition, the
system SHALL flag the basis as missing rather than emit an empty value.

### REQ-GDPR-006 | Access to personal data is audited (Art.15 accountability)
- categoria: funzionale
- priorità: M
- fonte: GDPR Art.5(2) accountability + Art.15; ADR-0002
- rationale: the controller must be able to answer "who read what about whom, and when"; the audit must not be bypassable by the read path it observes
- trace-to: `PersonalDataAccessAdvisor.java` (Spring AOP); `PersonalDataAccessAdvisorTest#deliveriesReachTheSink`; integration `GdprIntegrationTest#advisorEmitsAuditRecordOnPersonalDataAccess`
- stato: implementato

WHEN code reads a member annotated `@GdprPersonalData`, the system SHALL record an audit entry
(subject, actor, timestamp, target) through the configured `AuditSink`.

### REQ-GDPR-007 | Auditing never breaks the audited business path (fail-open on the request thread)
- categoria: qos-constraint
- priorità: M
- fonte: ADR-0002 (async sink, drop-newest); reliability posture
- rationale: a compliance side-effect must never take down the business operation it observes; a sink failure or a slow downstream must not propagate to the request thread
- chi-paga-questo-nove: bounded async queue (default 1024, 1 worker); above ~1024 events/sec/pod the queue saturates and the `dropped` Micrometer counter increments. Zero-loss audit is available by setting `async.enabled=false`, trading request-thread blocking
- trace-to: `PersonalDataAccessAdvisorTest#sinkFailureIsAbsorbedNotPropagated`; `AsyncAuditSinkDecoratorTest#sinkFailureIsAbsorbedAndCounted`, `#saturatedQueueDropsNewestAndCounts`, `#writesAreDispatchedToWorkerThread`
- stato: implementato

WHEN the audit sink throws or its downstream blocks, the system SHALL absorb the failure off
the request thread and count it (`failed` / `dropped` meters), never propagating it to the
business operation being audited.

### REQ-GDPR-008 | Audit gaps are observable (Art.5(2) accountability)
- categoria: qos-constraint
- priorità: S
- fonte: GDPR Art.5(2); ADR-0002
- rationale: an async, drop-newest audit can lose entries under load; the loss must be observable so accountability gaps are not silent
- trace-to: `AuditSinkMetrics.java` (three gauges); `AuditSinkMetricsTest#registersThreeGauges`, `#submittedGaugeReflectsLiveCount`
- stato: implementato

WHEN Micrometer is on the classpath, the system SHALL register `submitted`, `dropped`, and
`failed` audit meters so a positive `dropped`/`failed` rate signals an accountability gap.

### REQ-GDPR-009 | Audit query by subject and time window (Art.15 access)
- categoria: funzionale
- priorità: M
- fonte: GDPR Art.15
- rationale: the Article 15 answer must be queryable by subject id and bounded to a time window for a defensible audit response
- trace-to: `GdprController.java` `GET /gdpr/audit/access`; `JdbcAuditSink.java`; `JdbcAuditSinkTest#writeAndQueryRoundTrip`, `#filtersByTimeWindow`; `JdbcAuditSinkPostgresIT#writesAndReadsAgainstMigrationApplied`
- stato: implementato

WHEN an authorised actor queries the access log for a subject over a time window, the system
SHALL return the matching audit rows.

### REQ-GDPR-010 | Erasure orchestration: ordered, audited, partial-failure honest (Art.17)
- categoria: funzionale
- priorità: M
- fonte: GDPR Art.17; ADR-0004, ADR-0006
- rationale: a real subject's data lives in many tables with FK dependency order; each erasure step must be ordered (child before parent), audited per handler, and report partial failure rather than claim success
- trace-to: `ErasureService.java`, `ErasureHandler.java`; `ErasureServiceTest#invokesHandlersInOrderAscending`, `#aggregatesAffectedCountsByType`, `#rejectsBlankSubjectId`; integration `GdprIntegrationTest#erasureRemovesCustomerAcrossAllHandlers`; `GdprController` `DELETE /gdpr/erasure/{subjectId}` (207 Multi-Status on partial failure)
- stato: implementato

WHEN an erasure is requested for a subject id, the system SHALL invoke the registered
`ErasureHandler` beans in ascending order, audit each call, and report the per-handler
outcome (207 Multi-Status if any handler partially failed).

```gherkin
Given a subject with personal data spread across several registered ErasureHandlers
When DELETE /gdpr/erasure/{subjectId} is called
Then each handler is invoked in ascending order on that subject id
And one audit row per handler records (entity type, subject id, strategy, affected count, outcome)
And the response reports the per-type affected counts (207 if a handler partially failed)
```

### REQ-GDPR-011 | Erasure completeness is the adopter's, and the library is honest about it
- categoria: technology-constraint
- priorità: S
- fonte: ADR-0004 ("the library knows orchestration; the application knows the data")
- rationale: the library cannot guarantee full erasure (that depends on the adopter writing a handler per table); it must NOT overclaim, the audit row says what the library did, not what the system did
- trace-to: `ADR-0004` (consequences section); README "How right-to-erasure actually works"; absence of any reflection-driven cascade in `ErasureService.java`
- stato: implementato

WHEN erasure runs, the system SHALL audit exactly the handlers it invoked and SHALL NOT claim
the subject is fully erased beyond the registered handlers' reach.

### REQ-GDPR-012 | Retention sweep enforces the declared window (Art.5(1)(e))
- categoria: funzionale
- priorità: S
- fonte: GDPR Art.5(1)(e); ADR-0005
- rationale: no zombie data kept forever; the declared retention period must actually be applied by a scheduled, deterministic sweep
- chi-paga-questo-nove: `@Scheduled` fires per pod; in a multi-pod deployment the sweep triple-calls `applyDue` without leader election (ShedLock recipe deferred, README Reality check)
- trace-to: `RetentionScheduler.java`, `RetentionTarget.java` (SPI); `RetentionSchedulerTest#sweepAppliesEachTargetWithCutoffDerivedFromInjectedClock`, `#runWithOffsetUsesCallerProvidedDuration`, `#targetCountReflectsRegisteredTargets`
- stato: implementato

WHEN the retention cron fires, the system SHALL apply each registered `RetentionTarget` with a
cutoff derived from an injectable clock (delete / anonymize / pseudonymize per the declared
strategy).

### REQ-GDPR-013 | The audit store is schema-managed, fail-fast, and injection-safe
- categoria: technology-constraint
- priorità: M
- fonte: GDPR Art.32 (integrity of processing); ADR-0001
- rationale: in production the audit table is applied via Flyway (privileged migrator), not auto-created at runtime; a missing table must fail fast; a configurable table name must not become an SQL-injection vector
- trace-to: `db/migration/V1__gdpr_audit_access.sql` + bundled Liquibase changelog; `JdbcAuditSinkTest#failsFastWhenTableMissingAndAutoCreateOff`, `#worksWhenTableExistsAndAutoCreateOff`, `#schemaIsIdempotent`, `#rejectsTableNamesThatLookLikeSqlInjection`; `JdbcAuditSinkPostgresIT#failsFastWhenTableMissingAndAutoCreateOff`
- stato: implementato

WHEN `auto-create-schema` is off and the audit table is absent, the system SHALL fail fast at
startup; AND WHEN a configured table name does not match a safe identifier pattern, the system
SHALL reject it rather than interpolate it into SQL.

### REQ-GDPR-014 | The `/gdpr/**` endpoints carry no authentication of their own (fail-secure handoff)
- categoria: technology-constraint
- priorità: M
- fonte: GDPR Art.32; threat model (default-open REST); README "Wiring with Spring Security"
- rationale: erasure deletes data and the access log reveals subjects; the starter deliberately does not add auth, it requires the adopter to wire Spring Security on `<base-path>/**` and documents this as a load-bearing obligation
- trace-to: README "Wiring with Spring Security" + Reality check; example `SecurityConfig.java`; `QuickstartE2ETest#unauthenticatedAccessIsRejected`, `#erasureEndpointForbiddenForNonDpoRole`, `#erasureEndpointAccessibleToDpoRole`
- stato: implementato

WHEN the `/gdpr/**` endpoints are exposed, the system SHALL require the adopter to enforce
authentication and authorisation on them (the library ships them auth-agnostic and documents
the obligation; the example proves the DPO-role gate).

### REQ-GDPR-015 | Personal-data category taxonomy (IDENTITY / CONTACT / FINANCIAL)
- categoria: technology-constraint
- priorità: M
- fonte: GDPR Art.5(1)(c) minimisation + Art.30; dogfooding gap (housetree gest REQ-GDPR-001)
- rationale: today `@GdprPersonalData` carries only `specialCategory` (a boolean); a single dimension cannot drive selective retention, selective redaction, or a category-grouped ROPA. A coarse category taxonomy is the missing axis
- trace-to: `proposto` — extend `@GdprPersonalData` with a `category` dimension; backstop = the generated ROPA grouping by category
- stato: proposto

WHEN a field is marked as personal data, the system SHALL let the developer classify it as
`IDENTITY` | `CONTACT` | `FINANCIAL` (alongside the existing `specialCategory` flag), so
retention, redaction, and the ROPA can group and act by category.

### REQ-GDPR-016 | Append-only-safe erasure for event-sourced stores (Art.17, by design)
- categoria: funzionale
- priorità: M
- fonte: GDPR Art.17; dogfooding gap (housetree gest, event-sourced Activity domain)
- rationale: in an append-only event store you cannot delete the event that carries the PII; erasure must be a by-design property, not a DELETE. The current `ErasureHandler` shell assumes mutable stores (it deletes/anonymises rows); it does not cover an immutable log
- trace-to: `implementato` — ADR-0009 (crypto-shredding); `CryptoShreddingErasureTest` (drop-the-key renders PII unrecoverable while the event stays byte-immutable; per-subject granularity; replay-idempotent with no PII; erasure recorded as an audit fact), `AesGcmCryptoShredderTest` (AEAD, fail-closed, unique IV), `JdbcSubjectKeyStoreTest` (key lifecycle + tombstone/no-resurrection); classes under `erasure/crypto/` (`SubjectKeyStore`, `CryptoShredder`/`AesGcmCryptoShredder`, `CryptoShreddingErasureHandler`); migration `V2__gdpr_subject_key.sql`
- depends-on: REQ-GDPR-017
- stato: implementato

WHEN a subject exercises erasure against an append-only event store, the system SHALL render
that subject's personal data **no longer recoverable** from any projection and from the raw
event payload, while **preserving** the immutable audit trail (the erasure is itself a recorded
fact, not a mutation of the log).

```gherkin
Given a subject whose personal data lives inside immutable events
When erasure is executed for that subject
Then no projection nor a replay of the events exposes the subject's personal data
And replaying the stream still yields an idempotent read model (no drift)
And the audit trail still proves the erasure happened (who, when, why)
```

### REQ-GDPR-017 | Erasure strategy for append-only stores is decided in an ADR
- categoria: technology-constraint
- priorità: M
- fonte: derived-from REQ-GDPR-016
- rationale: the strategy is a one-way door (it touches the append-only event format) and must be decided in an ADR with an adversarial pass, not fixed in a requirement
- trace-to: `implementato` — ADR-0009 `docs/adr/0009-append-only-erasure-crypto-shredding.md` (accepted): crypto-shredding chosen over sentinel-at-projection; adversarial pass turned key-backup retention, per-subject granularity and involuntary-erasure into design constraints
- stato: implementato

WHEN REQ-GDPR-016 is implemented, the system SHALL adopt the strategy decided in a dedicated
ADR between (a) **crypto-shredding** (per-subject encrypted PII, erasure = drop the key) and
(b) **sentinel-at-projection + upcaster** that strips the raw payload. ADR-0009 adopts (a):
crypto-shredding, the first event-sourcing module the library ships.

### REQ-GDPR-018 | Structured-log PII redaction (Art.5(1)(f))
- categoria: qos-constraint
- priorità: S
- fonte: GDPR Art.5(1)(f) integrity and confidentiality; security posture
- rationale: a PII value that lands in an application log is a silent breach; the library audits *deliberate* access but does nothing about *accidental* logging of an annotated object. Missing today
- trace-to: `proposto` — a Logback/logstash converter that masks `@GdprPersonalData` fields; test that a classified field never appears in clear text in log output
- stato: proposto

WHEN an object carrying `@GdprPersonalData` fields is logged, the system SHALL mask those
fields in the structured log output (never PII in clear text in the logs).

### REQ-GDPR-019 | Right of access export end-to-end (Art.15)
- categoria: funzionale
- priorità: C
- fonte: GDPR Art.15
- rationale: the library audits access today but does not assemble the subject-facing export of all their personal data; that is the adopter's to build on top of the annotations, and the library could offer a derived export
- trace-to: `proposto` — export derived from the `@GdprPersonalData` fields per subject; end-to-end test on a DSR endpoint
- stato: proposto

WHEN an authenticated subject requests access to their data, the system SHALL be able to return
an export of all their classified fields in a human-readable form.

### REQ-GDPR-020 | Consent management (Art.7) — deferred by ADR
- categoria: funzionale
- priorità: W
- fonte: GDPR Art.7; ADR-0008 (deferred)
- rationale: proof of explicit, freely-given, informed consent (wording, moment, channel, withdrawal). A half-built consent ledger that misses a withdrawal event creates legal exposure; deliberately deferred until a real adopter conversation (ADR-0008)
- trace-to: `proposto` — none (W = won't-this-time, declared in ADR-0008); buildable on top of `AuditSink` + `ErasureHandler`
- stato: proposto

WHEN consent is the lawful basis, the system SHALL record proof of explicit, informed consent
including its withdrawal — deferred to a future minor per ADR-0008.

### REQ-GDPR-021 | Data portability (Art.20) — deferred by ADR
- categoria: funzionale
- priorità: W
- fonte: GDPR Art.20; ADR-0008 (deferred)
- rationale: structured machine-readable export of all personal data; the export shape (JSON-LD / CSV-per-entity / dossier) is not yet decided, and shipping a wrong shape adopters depend on is harder to fix than not shipping. Deferred (ADR-0008)
- trace-to: `proposto` — none (W = won't-this-time, declared in ADR-0008)
- stato: proposto

WHEN a subject requests portability of the data they provided, the system SHALL be able to
export it in a structured, interoperable format — deferred to a future minor per ADR-0008.

---

## Where the as-built catalog will come from (tracegate)

This repo does not yet run a test-as-spec generator. When **`tracegate`** lands (the same
generator pattern used in the housetree monorepo: tree-sitter parse of `@Test` javadoc into
a `_generated/requirements.md`, with a CI `--check` drift gate), the `trace-to` of every
`stato: implementato` requirement above becomes a derived link rather than a hand-verified
one, and a drift gate fails the build if a requirement here loses its backing test. Until
then the `trace-to` references are **hand-verified against the clone** (every cited test
method and class was confirmed to exist on 2026-06-08). Do NOT wire tracegate here yet; it is
not ready in this repo.

## Changelog
- 2026-06-08 | REQ-GDPR-001..021 | created | first hand-authored EARS to-be spec of the library itself; 14 implementato (each with a hand-verified trace to an existing test/class), 7 proposto (category taxonomy, append-only erasure + its strategy ADR, log-redaction, Art.15 export, plus the ADR-0008 deferred consent/portability); strategy for append-only erasure deferred to a future ADR (REQ-GDPR-017)
- 2026-06-08 | REQ-GDPR-016, REQ-GDPR-017 | proposto -> implementato | crypto-shredding shipped under ADR-0009 (accepted): per-subject AES-256-GCM key store (`SubjectKeyStore` SPI + JDBC default, `gdpr_subject_key`/V2), `AesGcmCryptoShredder`, `CryptoShreddingErasureHandler` (drop-the-key + audit fact); `CryptoShreddingErasureTest` un-`@Disabled` (4 GREEN) plus `AesGcmCryptoShredderTest`/`JdbcSubjectKeyStoreTest`
