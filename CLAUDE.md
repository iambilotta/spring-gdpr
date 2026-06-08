# CLAUDE.md — `spring-gdpr` (repo working contract)

The technical working contract for this repo. The README is the **usage + pitch** surface;
the ADRs are the **decisions**; `docs/requirements/` is the **normative spec**; the tests are
the **as-built reality**. This file says how to work here without duplicating any of them.

## What this is

Apache-2.0 Spring Boot library that turns `@Gdpr*` annotations on domain types into GDPR
evidence: build-time **Art.30 ROPA + Art.35 DPIA** (annotation processor, JSR-269), runtime
**Art.15 access audit log** (Spring AOP + async sink), **Art.17 erasure** orchestration shell
(`ErasureHandler` SPI), **Art.5(1)(e) retention** sweep. JitPack-distributed, no SaaS, no data
egress. A reference / portfolio asset, not a commercial product (Maven Central deliberately not
planned). Dual line: `v1.x` (Spring Boot 3.5+, frozen) and `v2.x` (Spring Boot 4.0+, active).

## Architecture (the one-line map; the README has the diagrams)

Two separately consumable halves (ADR-0003): a **build-time** annotation processor
(`spring-gdpr-processor` + `-maven-plugin`) and a **runtime** starter (`spring-gdpr-starter`).
Both read the *same* annotations (`spring-gdpr-annotations`, zero runtime deps) so the dossier
and the running system can never disagree (ADR-0001). Modules: `annotations` (the 5 `@Gdpr*`),
`starter` (AOP advisor + async sink + erasure orchestration + retention scheduler + REST),
`processor` (APT → `ropa.csv` + `dpia.md`), `maven-plugin` (CI goals), `starter-test` (demo +
integration suite), `benchmark` (JMH, not distributed).

## The canon governance (where each kind of fact lives)

The strongest-deterministic layer that can hold a rule owns it; prose is the last resort.

| Artifact | Holds | Discipline |
|---|---|---|
| `@Gdpr*` annotations | the GDPR **evidence** source of truth (ADR-0001) | the code is the baseline; remove the annotation, remove the claim |
| `docs/adr/*.md` | locked **design decisions** (MADR-lite: status/context/decision/consequences/alternatives) | never delete; supersede with a new ADR and cross-link |
| `docs/requirements/*.requirements.md` | the **to-be** normative spec (hand-authored EARS, MoSCoW, trace-to) | append-only changelog; the gap to the implemented set is the backlog |
| tests (`@Test`) | the **as-built reality** (each test is a requirement) | every test pins one property; `trace-to` in the requirements points here |
| code comments | non-obvious **invariants** that survive refactors | no session chronicle, no decision narrative |
| `CHANGELOG.md` | the released delta per version | Keep-a-Changelog shape |

**tracegate (future, not wired here)**: a test-as-spec generator (tree-sitter over `@Test`
javadoc → `_generated/requirements.md` + a CI `--check` drift gate, the pattern used in the
housetree monorepo) will later make the `trace-to` of every implemented requirement a derived
link and fail the build on drift. It is **not ready in this repo**: do not wire it. Until then
the requirements `trace-to` are hand-verified against the code.

## Hard rules

- **No production code without a test that demands it.** Unit tests on starter logic boot no
  Spring context where possible; `ApplicationContextRunner` for autoconfig wiring; `*IT` +
  Testcontainers for the JDBC sink against real Postgres.
- **The annotations are the single source of truth.** Never add a second place (YAML, SaaS, XML)
  the DPO would have to maintain (ADR-0001). A feature that forces a separate file is wrong-shaped.
- **Do not overclaim erasure.** The library audits what its handlers did, never claims the
  subject is fully erased beyond the registered handlers' reach (ADR-0004). No reflection-driven
  cascade.
- **The `/gdpr/**` endpoints ship auth-agnostic on purpose.** Erasure deletes and access export
  reveals subjects; the adopter MUST wire Spring Security. Never bolt half-auth into the starter.
- **Audit must never break the audited path.** Sink failures and slow downstreams are absorbed
  off the request thread and counted (`dropped`/`failed` meters), never propagated (ADR-0002).
- **Schema is Flyway/Liquibase-owned, not runtime-created** in production (`auto-create-schema`
  off → fail fast if the table is missing). A configurable table name is validated, never
  interpolated raw into SQL.
- **English** for code, comments, commits, ADRs, requirements. **No em-dashes.**
- **No breaking change to the annotation surface** without an ADR and a deprecation cycle
  (see ADR-0007 on why `subjectIdField` was kept and clarified rather than renamed).

## Build entry points

```
mvn -B clean verify                      # full gate: unit + autoconfig wiring + Spotless
mvn -Dspring-gdpr.it=true verify         # + Postgres Testcontainers IT (needs Docker API >= 1.40)
mvn compile                              # regenerates ropa.csv + dpia.md under target/generated-sources/
./mvnw -DskipTests -pl spring-gdpr-benchmark -am package   # JMH harness
```

## Adding an ADR

Copy `docs/adr/0000-template.md` to the next number, kebab-case the title, status `proposed`,
flip to `accepted` after review. Never delete an old ADR; mark `superseded`/`deprecated` and
link both ways. Keep it short and honest (forces at play, what was chosen, what got harder,
what else was considered).

## Scope discipline

Article 7 consent and Article 20 portability are **deliberately deferred** (ADR-0008) and appear
as `proposto` in `docs/requirements/gdpr.requirements.md` so the gap is visible, not hidden. The
three to-be engineering gaps the library flags about itself — the IDENTITY/CONTACT/FINANCIAL
category taxonomy, append-only-safe erasure (crypto-shredding for event-sourced stores), and
structured-log PII redaction — live as `proposto` requirements with a strategy ADR pending
(REQ-GDPR-017). Declare them; do not half-build them.
