# MANIFEST — spring-gdpr-example-quickstart-postgres auto-generated docs

Read this **first** in a fresh session. Every doc below is regenerated from the code (or tests / migrations / properties / pom) on every commit; the markdown is never the source of truth. Path-only links so any tool that opens the manifest can lazy-load.

## Map

- [`structure.md`](./structure.md) — convention-driven repository tree (git-tracked skeleton) — read first to orient

## Scope & roadmap

- [`requirements.md`](./requirements.md) — tests-as-requirements catalog (FR/NFR/INV/CON/E2E), 100% spec javadoc
- [`requirements-by-us.md`](./requirements-by-us.md) — tests grouped per User Story with AC coverage gate

## Architecture

- [`modules.md`](./modules.md) — Modulith canvas + cross-module dependency graph + cycle detection
- [`modules-graph.md`](./modules-graph.md) — Mermaid module-dependency graph (cycles highlighted), renders inline on GitHub/intranet
- [`domain-model.md`](./domain-model.md) — Mermaid class diagram of the domain: records, sealed hierarchies, enums
- [`ports.md`](./ports.md) — hexagonal ports → adapters matrix
- [`templates.md`](./templates.md) — JTE template tree (params + include graph)

## Behavior

- [`http-endpoints.md`](./http-endpoints.md) — every HTTP route with javadoc + contract reference
- [`state-machine.md`](./state-machine.md) — Mermaid stateDiagram-v2 from a declared transition table (when present)

## State

- [`config.md`](./config.md) — application properties per profile + Java usages
- [`dependencies.md`](./dependencies.md) — Maven + npm + Python dependency tree

## Quality & debt

- [`coverage.md`](./coverage.md) — JaCoCo per-file coverage with full file list (incl. 0% files)
- [`todo.md`](./todo.md) — TODO/FIXME/HACK/@Deprecated inventory with git blame
- [`adr-index.md`](./adr-index.md) — Architecture Decision Record index + auto-detected ADR candidates

---

**Convention**: `_generated/` is gitignored at the repo root (pattern `**/_generated/`) but the tracked `_generated/` of a documented app IS the docs. Pre-commit regenerates everything; never hand-edit a file in this directory.
