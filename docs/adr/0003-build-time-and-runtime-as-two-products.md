# ADR-0003: Build-time generator and runtime starter as two separately adoptable halves

- **Status:** accepted
- **Date:** 2026-05-02
- **Deciders:** Francesco Bilotta

## Context

The library does two distinct jobs: it generates DPIA + ROPA at build time (annotation processor), and it logs personal-data accesses + handles erasure at runtime (Spring Boot starter). Some teams need only the first half: their DPO wants the dossier, but the application is too small or too low-traffic to justify wiring a runtime audit sink and a `/gdpr/erasure` endpoint. Other teams have already audited their persistence layer and want only the runtime advisor.

A single uber-module forcing both halves together would push adopters who only need one half to drag in dependencies, REST endpoints, and Spring Security configuration they do not use.

## Decision

The two halves ship as separately consumable Maven artifacts:

- `spring-gdpr-annotations` (zero-deps): the five annotations.
- `spring-gdpr-processor` (build only, JSR 269 annotation processor): generates `ropa.csv` and `dpia.md`.
- `spring-gdpr-starter` (Spring Boot 3.5+): AOP advisor, REST endpoints, retention sweep, async sink.

Adopters wire only what they need. The README "Two products, one source of truth" section names the split explicitly.

## Consequences

- Smaller blast radius for users in regulated environments who already have an audit log they do not want to replace: take only the processor.
- More moving parts to maintain. `spring-gdpr-processor` and `spring-gdpr-starter` must agree on annotation semantics (handled by both depending on `spring-gdpr-annotations`).
- Documentation cost: the README has to explain the split or adopters mis-import.

## Alternatives considered

**Single monolithic dependency.** Rejected because forcing the runtime starter on adopters who only need DPIA generation is a friction point that costs more than the maintenance overhead of two artifacts.

**Single dependency with optional auto-configuration.** Rejected because Spring Boot's auto-config still pulls the runtime classes onto the classpath even when disabled by property; the dependency tree stays heavy.
