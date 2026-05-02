# ADR-0005: Retention sweep via @Scheduled, configurable cron

- **Status:** accepted
- **Date:** 2026-04-30
- **Deciders:** Francesco Bilotta

## Context

GDPR Article 5(1)(e) requires the controller to keep personal data "in a form which permits identification of data subjects for no longer than is necessary". For routine processing this means a scheduled enforcement: a cron job that walks the records, finds those past their `@GdprRetention(period = "P5Y")` window, and applies the declared strategy (DELETE / ANONYMIZE / PSEUDONYMIZE). The library has to decide where this scheduler lives.

Three options:

- An external scheduler (k8s CronJob, systemd timer) calling a REST endpoint.
- A library-provided `@Scheduled` bean inside the application JVM.
- A user-provided callback that the adopter wires themselves.

## Decision

The library provides a `RetentionScheduler` bean annotated `@Scheduled(cron = "${spring.gdpr.retention.cron:0 0 3 * * *}")`. The default fires at 03:00 server-local. The cron expression is fully overridable via property.

Adopters who prefer external scheduling set `spring.gdpr.retention.enabled=false` and call `RetentionScheduler.runWithOffset(Duration)` from their own trigger.

## Consequences

- Out-of-the-box: an adopter who imports the starter and applies the migration gets retention enforcement at 03:00 daily. No additional infra wiring required.
- Single-pod deployments work straight; multi-pod deployments must configure leader election (e.g. via ShedLock) because three pods running the same scheduler triple-call `applyDue`. Documented in the README "Reality check" with a one-line ShedLock recipe deferred to a future minor.
- The cron expression is a property, so adopters in regulated environments who want all schedules in their own systemd or k8s setup flip the property and call `runWithOffset` from where they already centralise jobs.

## Alternatives considered

**No scheduler, REST endpoint only.** Rejected because the most common adopter (a small Spring Boot service with a part-time DPO) wants retention "to just happen" without standing up a separate cron infrastructure.

**Quartz integration.** Rejected as over-engineering for v1. Spring `@Scheduled` is built-in and good enough for the common case; adopters who already run Quartz configure their own trigger.

**ShedLock bundled.** Rejected for v1: it imposes a database table on every adopter even single-pod ones. Documented as the multi-pod recipe; adopters who need it pull it in themselves.

## Why this matters

The principle is "the smallest infra dependency that still gets the job done". Adding Quartz, ShedLock, or a dedicated cron container is reasonable for some adopters but not for the median Spring Boot service. By defaulting to `@Scheduled` we cover the 80% case with zero extra moving parts; multi-pod deployments still get a documented recipe instead of a hidden gotcha.

## References

- `RetentionScheduler` and `RetentionTarget` SPI in `spring-gdpr-starter/src/main/java/.../retention/`.
- Cron property: `spring.gdpr.retention.cron`. Tests in `RetentionSchedulerTest`.
- Multi-pod note: README "Reality check" + future ADR on ShedLock if a real adopter brings the requirement.
