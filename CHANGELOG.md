# Changelog

All notable changes to this project are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versioning follows
[SemVer](https://semver.org/spec/v2.0.0.html).

Each entry links to the commit that introduced it. Subsections group changes
by area: **Annotations**, **Runtime**, **Build-time**, **DX**, **Migrations**,
**Examples**, **CI/security**, **Docs**.

## [Unreleased]

### Added
- **Annotations:** `@GdprPersonalData.storage` axis (`INLINE` default, `FORGETTABLE_PAYLOAD`),
  declaring a field's value as externalised. Backward-compatible (default `INLINE`).
- **Runtime:** the **forgettable-payload** erasure mechanism, now the **primary** personal-data
  erasure pattern (ADR-0010): `ForgettablePayloadStore` SPI + `JdbcForgettablePayloadStore` /
  `InMemoryForgettablePayloadStore`, `ForgettablePayloadReference` (URN-addressable),
  `ForgettablePayloadResolver` (fail-closed, `require` throws instead of faking a value),
  `ForgettablePayloadErasureHandler` (actual `DELETE` + audit fact, tombstone/no-resurrection), and
  `CompositeSubjectErasureHandler` to erase across both the forgettable store and the crypto key
  store as one unit.
- **Migrations:** `V3__gdpr_forgettable_payload.sql` (`gdpr_forgettable_payload` external PII store).
- **Runtime:** post-erasure SPI ([#37](https://github.com/iambilotta/spring-gdpr/issues/37)):
  `ErasureListener` (`onSubjectErased(ErasureReport)`) and a `SubjectErasedEvent` Spring
  `ApplicationEvent`, fired once by `ErasureService.eraseSubject` after the handlers commit. The hook
  for event-sourced / CQRS consumers to rebuild a projection or invalidate a cache that held a
  now-dangling forgettable-payload reference (ADR-0010). Auto-wired (every `ErasureListener` bean +
  the context `ApplicationEventPublisher`); a listener failure is surfaced as
  `ErasureListenerException` and never un-erases the subject. Backward compatible: no-op with no
  listener (new `ErasureService` constructors, the legacy one preserved).

### Changed
- **Docs:** crypto-shredding (ADR-0009) repositioned as the **secondary / exception** mechanism.
  README headline erasure pattern is now forgettable-payload; the legal nuance (pseudonymisation vs
  anonymisation, GDPR Recital 26, EDPB Guidelines 01/2025, contested total-key-destruction premise)
  is recorded honestly in ADR-0010. Requirements catalog now covers 107 requirements.

## [2.0.0] - 2026-05-02

First release of the **Spring Boot 4 line**. The library splits into two parallel
release lines from this point onward (closes
[#17](https://github.com/iambilotta/spring-gdpr/issues/17)):

- **v1.x** — Spring Boot 3.5+ LTS line, frozen at v1.1.x. No new features; bug
  fixes welcome via PR but no active maintenance commitment.
- **v2.x** — Spring Boot 4.0+ active line. All future development happens here.

### Changed (BREAKING for adopters)
- **Minimum Spring Boot version is now 4.0+.** Adopters on Spring Boot 3.5 must
  pin to v1.1.x; do not upgrade to v2.0.0.
- **Imports re-homed for Spring Boot 4 module split:**
  - `org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration` →
    `org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration`
- **Dependency rename:** `spring-boot-starter-aop` → `spring-boot-starter-aspectj`.

### Internal
- Test classpath additions for Spring Boot 4's split modules:
  `spring-boot-starter-jdbc` and `h2` declared explicitly in
  `spring-gdpr-starter-test/pom.xml` because the parent starter pom marks JDBC
  as `optional`. No effect on adopters.

### Docs
- README: "Status" line updated to "v1.x = SB 3.5 LTS line, v2.x = SB 4 active
  line", roadmap reflects the dual-line policy.

## [1.1.0] - 2026-05-02

API cleanup release. No new features. Sharpens the SPI surface that 1.0.0 froze
where a senior code review surfaced smells worth fixing before the first
external adopter.

### Changed (BREAKING for SPI implementers, not for application users)
- **`ErasureHandler.entityType()` now returns `Class<?>` instead of `String`.**
  Implementations change from `return Customer.class.getName();` to
  `return Customer.class;`. Wire format unchanged: the
  `DELETE /gdpr/erasure/{subjectId}` response still maps `affectedByType` keys
  to the type's fully qualified name (Jackson serialises `Class<?>` map keys
  as FQN strings).
- **`RetentionTarget.entityType()` now returns `Class<?>` instead of `String`.**
  Same migration as above. Used in retention sweep logs only, no wire impact.

Why: the previous String FQN coupling was stringly-typed. A renamed entity
slipped through to the audit row at runtime instead of failing at compile
time. Returning `Class<?>` puts the contract on the type system.

### Docs
- `@GdprErasable.subjectIdField` Javadoc now states explicitly that the value
  is documentation surfaced in the DPIA, not a runtime lookup driver. Adopters
  who wanted custom subject-id resolution were already supposed to override the
  `SubjectIdResolver` bean; this is now documented at the annotation level
  instead of being buried in the README's "Reality check" section.

## [1.0.0] - 2026-05-02

First stable release. API freeze for the five `@Gdpr*` annotations, the
`AuditSink` / `ErasureHandler` / `RetentionTarget` / `ActorResolver` SPIs,
the `spring.gdpr.*` configuration properties and the `/gdpr/**` REST shape.

JitPack coordinates: `com.github.iambilotta.spring-gdpr:spring-gdpr-starter:v1.0.0`.

### Docs
- README rewritten to open with a `Without spring-gdpr / With spring-gdpr`
  framing, to introduce the build-time generator and the runtime starter as
  two separately adoptable halves, and to put the right-to-erasure caveat
  upfront (the library does not magically purge tables; it orders
  `ErasureHandler` beans, audits each call, returns 207 Multi-Status).
- Roadmap collapsed: v1.0 is the current shipping surface; future scope
  reframed as "future minor" rather than fictional version numbers.

## [0.1.1] - 2026-05-01

### Fixed

- JitPack distribution build now succeeds: added Maven Wrapper 3.9.9 because
  the JitPack sandbox ships Maven 3.6.0, which trips the
  `maven-compiler-plugin 3.13.0` minimum-version check (3.6.3+ required).
  `jitpack.yml` now invokes `./mvnw` instead of the system `mvn`, downloading
  Maven 3.9.9 to JitPack's build cache on first run. Local CI on GitHub
  Actions unaffected (already on Maven 3.9.x). ([4d3b70e], [49ba0a0])
- Bump released because the JitPack v0.1.0 build failed and the JitPack
  cache holds the failed-build state for that tag. The v0.1.1 tag forces
  a clean rebuild against the wrapper-enabled commit.

## [0.1.0] - 2026-05-01

First public release. Apache 2.0. Distributed via [JitPack](https://jitpack.io/#iambilotta/spring-gdpr) (Maven Central namespace `com.iambilotta.gdpr` deferred to a later release).

### Added

#### Annotations
- `@GdprPersonalData`, `@GdprDataSubjects`, `@GdprLegalBasis`, `@GdprRetention`,
  `@GdprErasable`. Five-annotation API covering Art. 5(1)(e), 6, 9, 10, 17, 30, 35.
  ([526e0ba])
- `Art9Condition` and `Art10Basis` enums on `@GdprLegalBasis` for special-category
  (health, biometric, religion, ...) and criminal-convictions data. Audit log + ROPA
  + DPIA now record a composite article reference like `6(1)(b) + 9(2)(h)`. ([f476345])

#### Runtime
- `PersonalDataAccessAdvisor` (Spring AOP `@Before` advisor): captures every call to a
  `@GdprPersonalData`-annotated method or class, dispatches one `AuditAccessRecord`
  per access. ([526e0ba])
- `AsyncAuditSinkDecorator`: bounded-queue async wrapper around any `AuditSink`. ON by
  default. Drop-newest fallback under saturation, observable via `droppedCount()` +
  WARN logs. Default queue 1024, default 1 worker thread. ([f07053c])
- `AuditSinkMetrics`: optional Micrometer binder. Registers
  `spring.gdpr.audit.{submitted,dropped,failed}` gauges when Micrometer is on the
  classpath AND the sink is wrapped by `AsyncAuditSinkDecorator`. Alert recipe in
  Javadoc: page on `rate(spring_gdpr_audit_dropped_total[5m]) > 0`. ([527dd67])
- REST endpoints `DELETE /gdpr/erasure/{subjectId}` (Art. 17) and
  `GET /gdpr/audit/access` (Art. 15). Mounted at `${spring.gdpr.web.base-path}`,
  default `/gdpr`. No auth shipped, wire Spring Security around the base path. ([526e0ba])
- `RetentionScheduler` with `Clock` injection. `@Scheduled(cron = "${spring.gdpr.retention.cron}")`,
  default daily at 03:00 (Art. 5(1)(e)). ([526e0ba])

#### Build-time
- `GdprAnnotationProcessor` (APT). Walks every annotated type and emits two
  artifacts under `target/generated-sources/annotations/spring/gdpr/`:
  `ropa.csv` (Art. 30) + `dpia.md` (Art. 35 scaffold, sections 3-6 left for human
  judgment). Deterministic output, sorted by FQN, committed-friendly. Splits
  records-of-processing rows from access-points so a `@GdprPersonalData` method on
  a non-record type is not a false-positive ROPA row. ([526e0ba], [f476345])
- `JdbcAuditSinkPostgresIT`: Testcontainers-backed Postgres integration test
  asserting the Flyway migration applies cleanly on PostgreSQL 16 and the JDBC sink
  reads/writes against the migrated schema. Gated on `-Dspring-gdpr.it=true`,
  requires Docker API >= 1.40. ([a688574])

#### Maven plugin
- `spring-gdpr-maven-plugin` with three mojos: `gdpr:dpia` and `gdpr:ropa` copy the
  generated artifacts to `target/spring-gdpr/` for release pipelines, `gdpr:verify`
  fails the build at the `verify` phase if either is missing. ([526e0ba])

#### DX
- `additional-spring-configuration-metadata.json`: prose descriptions, defaults, and
  hints for every `spring.gdpr.*` property. IDE autocomplete (IntelliJ / VSCode /
  Eclipse) now surfaces tooltips, not just types. ([527dd67])

#### Migrations
- `db/migration/V1__gdpr_audit_access.sql` (Flyway) and
  `db/changelog/spring-gdpr-changelog.xml` (Liquibase) shipped inside the starter jar.
  Apply via your existing migration tool; `spring.gdpr.audit.auto-create-schema=true`
  for dev only. ([a688574])

#### Examples
- `examples/quickstart-postgres/`: runnable Spring Boot 3.5 demo with PostgreSQL via
  Docker Compose, Flyway-applied audit schema, Spring Security with `USER` + `DPO`
  roles, custom `ActorResolver` reading the security principal. End-to-end MockMvc
  test suite (4 tests) covering create, fetch, role-gated erasure. ([0ecd9f5])

#### CI/security
- `.github/workflows/ci.yml`: build + test + Postgres IT (`-Dspring-gdpr.it=true`)
  + quickstart compile + DPIA/ROPA presence assertion + artifact upload. ([0ecd9f5])
- `.github/workflows/codeql.yml`: GitHub CodeQL Java analyzer on push/PR + weekly cron.
  ([527dd67])
- `.github/dependabot.yml`: weekly Maven scans grouped by family (spring-boot /
  testing / maven-plugins), monthly for the quickstart, weekly for github-actions.
  ([527dd67])
- `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md` (Contributor Covenant 2.1), `SECURITY.md`
  (90-day disclosure window), `.github/ISSUE_TEMPLATE/`,
  `.github/PULL_REQUEST_TEMPLATE.md`. ([0ecd9f5])

#### Docs
- README with input/output hero pair, "Should I use this?" decision matrix, 5-minute
  setup, Mermaid architecture diagram (ASCII fallback), "Common questions",
  "Reality check" section consolidating limitations + anti-patterns. ([28029c9],
  [ba35eaf])
- Companion blog post draft `docs/articles/01-gdpr-compliance-by-annotation.md`.
  ([0ecd9f5])

### Changed

(none, this is the first release)

### Deprecated

(none)

### Removed

(none)

### Fixed

(none)

### Security

(no advisories at this time; vulnerability disclosure channel is open at
`francesco@iambilotta.com`, see [SECURITY.md](SECURITY.md))

[Unreleased]: https://github.com/iambilotta/spring-gdpr/compare/v0.1.1...HEAD
[0.1.1]: https://github.com/iambilotta/spring-gdpr/releases/tag/v0.1.1
[0.1.0]: https://github.com/iambilotta/spring-gdpr/releases/tag/v0.1.0

[526e0ba]: https://github.com/iambilotta/spring-gdpr/commit/526e0ba
[bfdc216]: https://github.com/iambilotta/spring-gdpr/commit/bfdc216
[f07053c]: https://github.com/iambilotta/spring-gdpr/commit/f07053c
[f476345]: https://github.com/iambilotta/spring-gdpr/commit/f476345
[a688574]: https://github.com/iambilotta/spring-gdpr/commit/a688574
[0ecd9f5]: https://github.com/iambilotta/spring-gdpr/commit/0ecd9f5
[527dd67]: https://github.com/iambilotta/spring-gdpr/commit/527dd67
[28029c9]: https://github.com/iambilotta/spring-gdpr/commit/28029c9
[ba35eaf]: https://github.com/iambilotta/spring-gdpr/commit/ba35eaf
[4d3b70e]: https://github.com/iambilotta/spring-gdpr/commit/4d3b70e
[49ba0a0]: https://github.com/iambilotta/spring-gdpr/commit/49ba0a0
