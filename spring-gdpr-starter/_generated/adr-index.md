# ADR index — spring-gdpr-starter (as-is)

Auto-generated from `apps/gest/decisions/NNNN-*.md`. Title comes from the H1; status from a `Status: ...` line in the body; supported user stories from `US-NNN-slug` citations anywhere in the file. The 'Candidates' section is a heuristic scan of the codebase for load-bearing decisions (feature flags, invariants, contracts, projections) that have NO matching ADR yet — surface, don't autofix.

**Total ADRs**: 0

_No ADR files found under `apps/gest/decisions/`._

## Candidates (auto-detected, 1 signals)

Heuristic. Each signal usually warrants an ADR (a feature flag, an invariant, a contract, a projection design choice are all decisions you'll want to defend in 6 months). Open the file, decide if it's already covered by an existing ADR, otherwise consider writing a new one.

### conditional bean wiring (1)

- `spring-gdpr-starter/src/main/java/com/iambilotta/gdpr/starter/autoconfig/GdprAutoConfiguration.java` — @Conditional* present: feature flag / runtime branch — usually load-bearing
