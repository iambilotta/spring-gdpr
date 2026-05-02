<!-- Thanks for the PR. Keep this template short. -->

## Summary

What does this change. One paragraph.

## Motivation

Linked issue (or link to a GDPR article / DPO request). If there is no issue, write here why this is a good idea.

## Test plan

- [ ] `./mvnw -B verify` green on the full reactor.
- [ ] New tests cover the behaviour change (or: existing tests cover it, link to which).
- [ ] `examples/quickstart-postgres` still boots (if the change touches public API).

## Breaking change

- [ ] Yes, documented in `CHANGELOG.md` under `[Unreleased]` with a migration block.
- [ ] No.

## Sign-off

By submitting this PR, I confirm:

- [ ] My contribution is licensed under Apache 2.0, the same licence as the project.
- [ ] I have signed off the commit (`git commit -s`). DCO is required.
