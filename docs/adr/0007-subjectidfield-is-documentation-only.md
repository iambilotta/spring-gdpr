# ADR-0007: @GdprErasable.subjectIdField is documentation, not a runtime lookup driver

- **Status:** accepted
- **Date:** 2026-05-02
- **Deciders:** Francesco Bilotta

## Context

The annotation `@GdprErasable(subjectIdField = "id")` reads naturally as "look up records by the field literally named `id` when erasing". In v0.1.x this misled both adopters and a senior code review pass: the property name suggests it drives the lookup, but the lookup is performed inside the user-provided `ErasureHandler.erase(subjectId)` body, with the `subjectId` resolved by the `SubjectIdResolver` SPI.

Three resolutions were available:

- rename the property (`documentSubjectIdField`, `subjectIdHint`) and break the annotation surface,
- make the property actually drive the lookup, by reflection on the entity, breaking the SPI,
- keep the property name and clarify the contract via Javadoc.

## Decision

Keep the property name `subjectIdField` because it is the term DPOs use in audits ("which column is the subject foreign key?"). Document explicitly in the Javadoc that the value is metadata surfaced in the DPIA, not a runtime lookup driver, and that subject-id resolution lives in `SubjectIdResolver`. The clarification shipped in v1.1.0.

A future v2 may sunset this property in favour of a typed `subjectId()` reference, but only if a real adopter request justifies the breaking change.

## Consequences

- Adopters reading the annotation in isolation can still misread it as a lookup directive. The Javadoc is the gating document.
- The DPIA reviewer (DPO) gets a meaningful "subject id field" column in the generated `dpia.md`, which matches the language used in audits.
- We accept that the contract relies on Javadoc rather than on the symbol name, and we document the gap in this ADR for future contributors.

## Alternatives considered

**Rename to `documentSubjectIdField`.** More accurate, but a `@Deprecated` migration cycle on a v1 breaking change for cosmetic clarity is too expensive against the marginal benefit.

**Remove the property, force adopters to declare the column elsewhere (XML, DPIA-yaml).** Reintroduces the drift problem the library exists to solve.

**Make the property drive the lookup.** A reflection-driven path forces the library to know the persistence shape (JPA entity, JDBC POJO, Mongo document), which contradicts ADR-0004's decision to keep `ErasureHandler` as a user-owned method.

## Why this matters

The principle is "annotation properties surface in audits, they do not drive runtime". `subjectIdField` exists because a DPO running an audit needs to know which column links a record to a subject; that information belongs in the DPIA. Driving the lookup off it would force the library into a persistence-aware shape (JPA reflection, etc) that ADR-0004 explicitly rejected. Keeping the property and clarifying its semantic is cheaper than renaming.

## References

- Annotation: `spring-gdpr-annotations/src/main/java/.../GdprErasable.java`.
- Resolver: `SubjectIdResolver` SPI in `spring-gdpr-starter/src/main/java/.../audit/`.
- Javadoc clarification shipped in the v1.1.0 release.
