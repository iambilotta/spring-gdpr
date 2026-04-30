# Changelog

All notable changes to this project are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versioning follows
[SemVer](https://semver.org/spec/v2.0.0.html).

Each entry links to the commit that introduced it. Subsections group changes
by area: **Annotations**, **Runtime**, **Build-time**, **DX**, **Migrations**,
**Examples**, **CI/security**, **Docs**.

## [Unreleased]

### Added

#### Annotations
- `Art9Condition` and `Art10Basis` enums on `@GdprLegalBasis` for special-category
  (health, biometric, religion, ...) and criminal-convictions data. Audit log + ROPA
  + DPIA now record a composite article reference like `6(1)(b) + 9(2)(h)`. ([f476345])

#### Runtime
- `AsyncAuditSinkDecorator`: bounded-queue async wrapper around any `AuditSink`. ON by
  default. Drop-newest fallback under saturation, observable via `droppedCount()` +
  WARN logs. Default queue 1024, default 1 worker thread. ([f07053c])
- `AuditSinkMetrics`: optional Micrometer binder. Registers
  `spring.gdpr.audit.{submitted,dropped,failed}` gauges when Micrometer is on the
  classpath AND the sink is wrapped by `AsyncAuditSinkDecorator`. Alert recipe in
  Javadoc: page on `rate(spring_gdpr_audit_dropped_total[5m]) > 0`. ([527dd67])

#### Build-time
- `JdbcAuditSinkPostgresIT`: Testcontainers-backed Postgres integration test
  asserting the Flyway migration applies cleanly on PostgreSQL 16 and the JDBC sink
  reads/writes against the migrated schema. Gated on `-Dspring-gdpr.it=true`,
  requires Docker API >= 1.40. ([a688574])

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
- `.github/dependabot.yml`: weekly Maven scans grouped by family (spring-boot /
  testing / maven-plugins), monthly for the quickstart, weekly for github-actions.
  ([527dd67])
- `.github/workflows/codeql.yml`: GitHub CodeQL Java analyzer on push/PR + weekly cron.
  ([527dd67])
- `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md` (Contributor Covenant 2.1), `SECURITY.md`
  (90-day disclosure window), `.github/ISSUE_TEMPLATE/`,
  `.github/PULL_REQUEST_TEMPLATE.md`. ([0ecd9f5])

#### Docs
- README rewrite: hero now shows INPUT (annotated entity) AND OUTPUT (sample ROPA +
  DPIA). Decision-first navigation (Should I use this? + Common questions before
  setup), Mermaid architecture diagram (with ASCII fallback), merged "Limitations" +
  "Anti-patterns" into a single "Reality check" section. ([28029c9])

### Changed

#### Runtime
- `JdbcAuditSink` no longer creates the audit table on startup by default. Production
  path expects the schema to be applied via Flyway/Liquibase. Set
  `spring.gdpr.audit.auto-create-schema=true` to opt back in. ([a688574])
- `JdbcAuditSink` now fails fast at bean creation when the configured table is
  missing and auto-create is off, with an error message pointing to the bundled
  migration scripts. ([a688574])
- `PersonalDataAccessAdvisor.capture()` now wraps `sink.write()` in a try/catch.
  Sink failures are logged at ERROR with the event id and target member; the
  business method continues. Audit gap surfaces in logs, request thread is not
  blocked. ([f07053c])
- `pickPersonalData` selection bias replaced with `isAnyAnnotationSpecialCategory`
  aggregation (logical OR across method, type, parameters). A method touching one
  normal and one Art. 9 parameter is now flagged correctly regardless of declaration
  order. ([f476345])

#### Configuration
- `GdprAutoConfiguration`: collapsed two `@Bean AuditSink` methods (one per branch,
  both `@ConditionalOnMissingBean`) into a single factory that picks JDBC or Slf4j
  based on config + classpath + DataSource availability. Three explicit fallback
  paths, each with a WARN log so operator misconfig surfaces in the deploy summary
  instead of an opaque startup crash. ([bfdc216])

#### CI/security
- Maven Surefire pre-attaches `byte-buddy-agent` so Mockito does not self-attach at
  runtime. JDK 24+ disables dynamic agent loading by default; the pre-attach
  future-proofs the test suite and removes the JDK WARNING noise from build logs.
  ([527dd67])

#### Examples
- Demo `Customer` entity: `taxId` is no longer marked `specialCategory` (national
  tax IDs fall under Art. 87, not Art. 9). Added a genuine `healthCondition` field
  with `specialCategory = true` and updated the legal basis to include
  `Art9Condition.EXPLICIT_CONSENT`. Integration test seed updated. ([f476345])

### Deprecated

(none)

### Removed

(none)

### Fixed

(none, this is the pre-release hardening pass)

### Security

(no advisories at this time; vulnerability disclosure channel is open at
`francesco@iambilotta.com`, see [SECURITY.md](SECURITY.md))

## [0.1.0] - target 2026-Q3

Initial public release. Apache 2.0. Maven Central namespace `com.iambilotta.gdpr`
(claim pending). Highlights:

- Five-annotation API (`@GdprPersonalData`, `@GdprDataSubjects`, `@GdprLegalBasis`,
  `@GdprRetention`, `@GdprErasable`) covering Art. 5(1)(e), 6, 9, 10, 17, 30, 35.
- Runtime: AOP advisor, async audit sink (SLF4J + JDBC + custom), retention scheduler,
  REST endpoints for Art. 15 right-of-access and Art. 17 right-to-erasure.
- Build-time: APT processor producing deterministic `ropa.csv` + `dpia.md` artifacts
  under `target/generated-sources/`.
- Maven plugin: `gdpr:dpia`, `gdpr:ropa`, `gdpr:verify` goals.

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
