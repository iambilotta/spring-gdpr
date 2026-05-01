# Contributing to spring-gdpr

Thanks for considering a contribution. The repo is at v0.1, the surface is small, the rules are short.

## What is in scope

- Bug fixes and concrete production hardening (failure modes, performance, schema portability across mainstream RDBMS).
- New annotations or fields ONLY when they directly enable a regulator-required artifact (Art. 30 row, Art. 35 DPIA section, Art. 15 right-of-access, Art. 17 right-to-erasure, Art. 20 portability).
- Documentation that adds an example or fills a real gap. README diffs that say "this is great" without code go to issues, not PRs.
- Test coverage on production paths that the existing suite leaves uncovered.

## What is out of scope

- A SaaS dashboard, web UI, or no-code admin panel. Contradicts the evidence-as-code thesis.
- Wrappers for non-Spring stacks. The annotation surface can be ported, send a sister project link.
- Features that require pretending the tool certifies compliance. It does not, the README is explicit.
- Sweeping refactors without a paired bug or feature ticket.

## Workflow

1. Open an issue first for anything beyond a typo or a one-line fix. We agree on shape before code.
2. Fork, branch, code, push.
3. PR against `main` with:
   - A description that says WHAT changed and WHY (not just what files moved).
   - A test for the change. If a test is impossible, explain why in the PR.
   - All existing tests still green: `mvn -B clean verify`.
   - Optional but appreciated: run the Postgres IT (`mvn -Dspring-gdpr.it=true verify`) if you touched the JDBC sink.

## Commit messages

Write commit messages future-you would want to read at 2am during an incident:

- A 50-72 char subject line in imperative mood.
- A blank line, then a body that explains the WHY, the alternatives you considered, and the trade-off.
- Reference the issue or PR.

The existing commit log on `main` is the style guide. Read three recent commits before writing yours.

## Coding style

- Java 21 baseline. Use records, sealed types, switch expressions where they help.
- No em dashes (`-`) in comments or docs. Use commas, colons, periods, or `|`.
- Comments only when the WHY is non-obvious. Identifier names carry the WHAT.
- No new third-party runtime deps without discussion. Test deps are flexible.
- Spotless / Checkstyle: not yet wired. Match the existing style by reading what is around.

## Tests

- Unit tests on starter logic, no Spring context where possible.
- `ApplicationContextRunner` for autoconfig wiring tests.
- `@SpringBootTest` for full-stack integration where the wiring matters.
- Testcontainers for engine-specific behavior, gated on `-Dspring-gdpr.it=true`.

## Local build

```bash
mvn -B clean verify
```

Add `-Dspring-gdpr.it=true` to run the Postgres integration tests (needs Docker API >= 1.40).

## Code of conduct

Read [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md). Short version: be a normal adult.

## License + DCO

By submitting code, you agree to license it under Apache 2.0. Sign your commits with `-s` to certify the [Developer Certificate of Origin](https://developercertificate.org). PRs without DCO sign-off are not merged.

## Reporting security issues

Do NOT open a public issue for vulnerabilities. See [`SECURITY.md`](SECURITY.md).
