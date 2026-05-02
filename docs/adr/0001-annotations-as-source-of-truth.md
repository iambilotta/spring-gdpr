# ADR-0001: Annotations as the source of truth for GDPR evidence

- **Status:** accepted
- **Date:** 2026-04-30
- **Deciders:** Francesco Bilotta

## Context

GDPR Articles 30 (ROPA) and 35 (DPIA) require the controller to keep an up-to-date description of every processing activity touching personal data: data subjects, lawful basis, retention, special category flag, related access points. In every audit I have walked into, the same failure mode shows up: a `DPIA-2024-Q3.docx` shared from a colleague who left, describing an architecture that no longer exists. The information was always already in the codebase. The tooling was missing.

Three places could conceivably hold this metadata: a YAML config file shipped with the app, a SaaS dashboard configured by humans, or annotations on the entity classes themselves.

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
