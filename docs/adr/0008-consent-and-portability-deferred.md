# ADR-0008: Article 7 consent management and Article 20 portability deferred to a future minor

- **Status:** proposed
- **Date:** 2026-05-02
- **Deciders:** Francesco Bilotta

## Context

The library covers Articles 5(1)(e), 6, 9, 10, 15, 17, 30 and 35. It does not cover:

- **Article 7** (consent): proof of explicit, freely-given, informed consent including the wording, the moment, the channel, the withdrawal flow.
- **Article 20** (data portability): structured machine-readable export of all personal data on request.

Both are real GDPR obligations. Both are out of scope today.

## Decision

Stay deferred. Track each as a future minor (v1.2 or later) under a single ADR rather than letting them creep into v1.1 piecemeal. Document the gap in the README "What this is not" section so adopters in scope-sensitive scenarios know to combine the library with another tool (or build the missing piece on top of `AuditSink` + `ErasureHandler`).

## Why this matters

The principle is "do one thing well in v1, ship the next one when there is an adopter who needs it". Article 7 done badly is worse than not done: a half-built consent ledger that misses a withdrawal event creates legal exposure, not relieves it. We refuse to ship that. Article 20 is a smaller scope than 7, but the export shape (JSON-LD? CSV per entity? PDF dossier?) is something we do not yet have a strong opinion on, and shipping a wrong shape that adopters depend on is harder to fix than not shipping yet.

## Consequences

- Adopters whose primary GDPR risk is consent management (typical for B2C with marketing) will need a separate tool until v1.2+. The README names this clearly.
- The library is honest about its scope; this is what `Reality check` covers in the README.
- The maintainer's bandwidth stays focused on hardening the existing surface (audit, erasure, retention, dossier) before broadening.

## Alternatives considered

**Ship a minimum-viable Article 7 in v1.2.** Plausible but requires a real adopter conversation about consent UX, which has not happened.

**Ship Article 20 first because it is smaller.** Considered. Deferred because the export shape decision is not obvious enough to ship without an adopter input.

## References

- Article 7 GDPR: https://gdpr-info.eu/art-7-gdpr/
- Article 20 GDPR: https://gdpr-info.eu/art-20-gdpr/
- Roadmap line in `README.md`.
