# Changelog

All notable changes to this project are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versioning follows
[SemVer](https://semver.org/spec/v2.0.0.html).

Each entry links to the commit that introduced it. Subsections group changes
by area: **Annotations**, **Runtime**, **Build-time**, **DX**, **Migrations**,
**Examples**, **CI/security**, **Docs**.

## [Unreleased]

(no changes yet on top of v0.1.0)

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

[Unreleased]: https://github.com/iambilotta/spring-gdpr/compare/v0.1.0...HEAD
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
