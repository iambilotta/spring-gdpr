# ADR-0001: Annotations as the source of truth for GDPR evidence

- **Status:** accepted
- **Date:** 2026-04-30
- **Deciders:** Francesco Bilotta

## Context

A DPO walking into a GDPR audit needs three artefacts: an Article 30 ROPA, an Article 35 DPIA scaffold, and a defensible answer to "where in your stack does each item live". The first two are documents. The third is the gating question, and where every implementation we have seen falls down: the library that wins is the one whose answer to "where does each ROPA row come from" is "this Java class, that field, this commit".

Concretely the data the DPO needs (data-subject categories, lawful basis under Article 6/9/10, retention period, erasure strategy, the FK column that links a record to a subject) has to live somewhere the engineering team already maintains. The realistic candidates are a YAML config under `resources/`, a SaaS governance dashboard, or annotations on the JPA entity itself.

## Decision

The annotations on the domain types are the single source of truth for the GDPR evidence pack. The build-time annotation processor reads them and writes `ropa.csv` + `dpia.md`; the runtime AOP advisor reads the same annotations to decide what to audit and how. Both halves never disagree because they read the same Java symbols.

Concretely the five annotations (`@GdprPersonalData`, `@GdprDataSubjects`, `@GdprLegalBasis`, `@GdprRetention`, `@GdprErasable`) live in a zero-runtime-dependency module `spring-gdpr-annotations`, importable by any layer.

## Consequences

- Refactoring a `Customer` class moves the DPIA entry with it. A DPO who reviews the regenerated DPIA against last quarter's version sees the architecture changes for free.
- Removing a field annotated `@GdprPersonalData` deletes its row from `dpia.md` at the next `mvn compile`. If the deletion was unintended, code review catches it.
- The annotations live with the code, which means they live in the version-controlled history that the company already audits. No drift between "the doc" and "the running system" is possible.
- Deserves repeating: this is the source of truth for the **dossier**, not for the **claim**. If `@GdprRetention(period = "P5Y")` says five years but the actual retention sweep is configured to one year, the dossier and the runtime are still consistent because both read the same annotation; the *retention sweep* is the side that has to be wired to actually purge. Documented in ADR-0005.

## Alternatives considered

**YAML config under `src/main/resources/gdpr.yaml`.** Detached from the Java class it describes. A rename of `Customer` does not move the YAML entry; the file is updated by hand or it drifts. Same failure mode as the Confluence page we are replacing.

**SaaS dashboard (OneTrust, Privitar) with the schema kept in their UI.** Adopters pay 25k EUR/year and onboarding cycles, audit data leaves the company, and the dashboard inevitably drifts from what the application actually does. Net negative for our target.

**XML descriptors on the Spring bean side.** Spring has moved past XML config for ten years; there is no audience for "let me write a `<gdpr-personal-data field='email' />`".

## Why this matters

The principle is "the code is the audit-evidence baseline". Every later decision (where to log access, how erasure is orchestrated, when retention runs) defers to it. If a future feature would force the DPO to maintain a separate file, that feature is wrong-shaped. The DPO's hour-long review against the regenerated DPIA is the load-bearing UX of the entire library.

## References

- The five `@Gdpr*` annotations live in `spring-gdpr-annotations/` (zero runtime deps so the module is safe to import from any layer).
- The annotation processor that turns them into ROPA + DPIA is `spring-gdpr-processor/`. Output goes to `target/generated-sources/annotations/spring/gdpr/`.
- See `git log --oneline -- spring-gdpr-annotations/src/main/java` for the iteration on the annotation surface before v0.1.0.
