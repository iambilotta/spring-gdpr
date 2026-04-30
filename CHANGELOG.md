# Changelog

All notable changes to this project are documented here. Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versioning follows [SemVer](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `examples/quickstart-postgres/`: runnable Spring Boot 3.5 demo with PostgreSQL via Docker Compose, Flyway-applied audit schema, Spring Security with `USER` + `DPO` roles, custom `ActorResolver` reading the security principal. End-to-end `MockMvc` test suite (4 tests) covering create, fetch, role-gated erasure.
- `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md` (Contributor Covenant 2.1), `SECURITY.md` (90-day disclosure window), `.github/ISSUE_TEMPLATE/`, `.github/PULL_REQUEST_TEMPLATE.md`.
- `db/migration/V1__gdpr_audit_access.sql` (Flyway) and `db/changelog/spring-gdpr-changelog.xml` (Liquibase) shipped inside the starter jar. Apply via your existing migration tool; `spring.gdpr.audit.auto-create-schema=true` for dev only.
- `JdbcAuditSinkPostgresIT`: Testcontainers-backed Postgres integration test asserting the migration applies cleanly on PostgreSQL 16 and the JDBC sink reads/writes against the migrated schema. Gated on `-Dspring-gdpr.it=true`.
- `AsyncAuditSinkDecorator`: bounded-queue async wrapper around any `AuditSink`. ON by default. Drop-newest under saturation, observable via `droppedCount()` + WARN logs.
- `Art9Condition` and `Art10Basis` enums on `@GdprLegalBasis` for special-category and criminal-convictions data. Composite article reference (`6(1)(b) + 9(2)(h)`) in audit log + ROPA + DPIA.

### Changed

- `JdbcAuditSink` no longer creates the audit table on startup by default. Production path expects the schema to be applied via Flyway/Liquibase. Set `spring.gdpr.audit.auto-create-schema=true` to opt back in.
- `JdbcAuditSink` now fails fast at bean creation when the configured table is missing and auto-create is off, with an error message pointing to the bundled migration scripts.
- `PersonalDataAccessAdvisor.capture()` now wraps `sink.write()` in a try/catch. Sink failures are logged at ERROR with the event id and target member; the business method continues. Audit gap surfaces in logs, request thread is not blocked.
- `GdprAutoConfiguration`: collapsed two `@Bean AuditSink` methods (one per branch, both `@ConditionalOnMissingBean`) into a single factory that picks JDBC or Slf4j based on config + classpath + DataSource availability. Three explicit fallback paths, each with a WARN log.
- `pickPersonalData` selection bias replaced with `isAnyAnnotationSpecialCategory` aggregation (logical OR across method, type, parameters). A method touching one normal and one Art. 9 parameter is now flagged correctly regardless of declaration order.
- Demo `Customer` entity: `taxId` is no longer marked `specialCategory` (national tax IDs fall under Art. 87, not Art. 9). Added a genuine `healthCondition` field with `specialCategory = true` and updated the legal basis to include `Art9Condition.EXPLICIT_CONSENT`.

### Test surface

- Starter unit tests: 31 (was 12).
- Integration tests: 2 in starter-test (full Spring context) + 4 in quickstart example.
- Optional Postgres IT: 2 (gated, requires Docker API >= 1.40).

## [0.1.0] - target 2026-Q3

Initial public release. Apache 2.0. Maven Central namespace `com.iambilotta.gdpr` (claim pending).

[Unreleased]: https://github.com/iambilotta/spring-gdpr/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/iambilotta/spring-gdpr/releases/tag/v0.1.0
