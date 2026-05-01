## What this PR does

(One paragraph. WHAT changed and WHY.)

## Linked issue

(Closes #N, refs #N, or "no issue, this is a one-line fix".)

## Trade-offs and considered alternatives

(Even one bullet is fine. The point is to show you weighed the choice.)

## Tests

- [ ] Unit tests added for the changed logic
- [ ] Existing tests still green: `mvn -B clean verify`
- [ ] Postgres IT run if the JDBC sink was touched: `mvn -Dspring-gdpr.it=true verify`
- [ ] Manual smoke test on `examples/quickstart-postgres/` if the runtime path was touched

## Documentation

- [ ] README updated if the public API or the wiring changed
- [ ] CHANGELOG.md `[Unreleased]` section updated
- [ ] Javadoc on new public types

## Checklist

- [ ] Commit messages follow the repo style (subject in imperative mood, body explains WHY + alternatives)
- [ ] DCO sign-off (`-s`) on every commit
- [ ] No em dashes in the diff
- [ ] No new runtime dependencies without prior discussion in an issue
