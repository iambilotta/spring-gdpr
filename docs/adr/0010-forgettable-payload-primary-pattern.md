# ADR-0010: Forgettable payload is the primary personal-data erasure pattern (crypto-shredding the exception)

- **Status:** accepted
- **Date:** 2026-06-08
- **Deciders:** Francesco Bilotta

## Context

ADR-0009 shipped **crypto-shredding** as the library's append-only-safe erasure mechanism:
PII inside an immutable event is encrypted with a per-subject key, and erasure (GDPR Art. 17) is
the drop of that key, leaving byte-immutable ciphertext that can no longer be read.

Since then the industry consensus on *which* mechanism best satisfies the right to erasure for
**personal data** has sharpened, and it does not favour crypto-shredding as the default:

- **Encryption with a separately-held key is pseudonymisation, not anonymisation.** GDPR
  Recital 26 and the **EDPB Guidelines 01/2025 on pseudonymisation** treat data that *can* be
  re-associated with a subject by someone holding additional information (the key) as still
  **personal data**. Crypto-shredding does not delete the PII; it makes it conditionally
  unreadable. While the key exists the ciphertext is plainly personal data; the legal claim that
  it stops being personal data the instant the key is dropped rests entirely on **total,
  irreversible destruction of every copy of that key** (live store, every backup, every replica,
  any KMS cache). That total-destruction premise is **contested** and operationally hard: a single
  surviving key backup silently un-erases the subject.
- **The forgettable-payload / external-store pattern does actual deletion.** Greg Young's
  "forgettable payload" idea, popularised by Mathias Verraes ("Forgettable Payloads") and by
  Oskar Dudycz on event-driven.io, keeps the PII out of the immutable carrier entirely: the
  domain object or event stores only a **reference** (subjectId + fieldKey, or a URN); the value
  lives in a **mutable external store**. Erasure is a `DELETE` (or an anonymising overwrite to a
  "John Doe" placeholder) of that external row. The value is genuinely gone; the append-only log
  keeps only a dangling reference that resolves to nothing. This is anonymisation, the stronger
  Art. 17 guarantee, and it does not depend on the contested total-key-destruction premise.

The library's first event-sourcing module (ADR-0009) therefore optimised for the *narrow* case
(an immutable event that must legally carry the value) and presented it as *the* answer. For the
**common** case the more defensible pattern is forgettable payload.

## Decision

Adopt **forgettable payload (external PII store) as the PRIMARY personal-data erasure pattern**,
and demote **crypto-shredding to the secondary / exception mechanism**, used only where an
immutable event must legally carry the value inline and externalising it is not acceptable.

Concretely the library now ships, in `spring-gdpr-starter/.../erasure/forgettable/`:

- **`ForgettablePayloadStore` (SPI)** — the mutable external store keyed by `(subjectId, fieldKey)`:
  `put` / `resolve` / `erase`. Default JDBC implementation **`JdbcForgettablePayloadStore`** over a
  `gdpr_forgettable_payload` table (migration `V3__gdpr_forgettable_payload.sql`);
  `InMemoryForgettablePayloadStore` for tests and the in-test vault harness. The SPI lets an adopter
  back the store with a document DB, an object store, or a dedicated "PII vault" service while the
  domain stays event-sourced.
- **`ForgettablePayloadReference`** — the URN-addressable pointer (`urn:gdpr:fp:<subjectId>:<fieldKey>`)
  the carrier holds *instead* of the value, so the carrier itself holds no personal data.
- **`ForgettablePayloadResolver`** — the read side: resolves a reference to its value on read,
  **fail-closed** (an erased or never-written value resolves to empty; the `require` form throws a
  typed `PayloadNotAvailableException` rather than substituting a placeholder).
- **`ForgettablePayloadErasureHandler`** — an `ErasureHandler` whose `erase(subjectId)` **deletes**
  the external rows (the Art. 17 act) and writes an `AuditAccessRecord` (action `ERASURE`, basis
  `17`). Reported strategy is `DELETE`: the personal data is actually gone.
- **`CompositeSubjectErasureHandler`** — erases a subject across **both** mechanisms as one unit
  (forgettable-payload `DELETE` **and** the crypto-shredding key drop), summing affected counts, so
  a subject whose data is split across both paths is fully erased by one request and neither path is
  forgotten.
- A new **`@GdprPersonalData.storage`** axis (`INLINE` default, `FORGETTABLE_PAYLOAD`) so a field
  declares, at the source, that its value is externalised and routes to the primary erasure path.
  Backward-compatible: every existing annotation keeps `INLINE`.

**Tombstone / no-resurrection (preserved across both mechanisms).** `erase` records that the
subject was erased; a later `put` for that subject throws rather than re-creating their data. This
is the same guarantee `SubjectKeyStore` already gives, so the two mechanisms behave identically from
the adopter's side and an erased subject can never be silently re-created.

**Security posture (mirrored from crypto-shredding).** Fail-closed on a missing value, the value is
never logged nor placed in an exception message, SQL identifiers are validated as bare identifiers
at construction (the value and the subject id are always `?`-bound). The external store holds PII in
clear text at rest, so it carries the same at-rest protection obligation the inline columns it
replaces would: DB-level encryption, least-privilege grants.

## The legal nuance, stated honestly

This ADR does **not** claim crypto-shredding is non-compliant, nor that forgettable payload is a
silver bullet. The honest position:

- **Crypto-shredding = pseudonymisation while any key copy survives.** Its Art. 17 validity hinges
  on **total, irreversible destruction of every key copy**, a premise that is operationally fragile
  (key backups, replicas, KMS caches) and **legally contested**: regulators and commentators do not
  uniformly accept "the key is gone" as equivalent to erasure. Until the last key copy is provably
  destroyed, the ciphertext is, per Recital 26, still personal data.
- **Forgettable payload = anonymisation of the carrier by construction.** Because the carrier never
  held the value, deleting the external row leaves nothing to re-associate. This sidesteps the
  total-key-destruction problem. It is **not** free of duties: the external store itself must honour
  its own backups/retention (a stale backup of the *value* store re-creates the PII just as a stale
  key backup un-erases a crypto-shredded subject), and the references are not anonymous on their own
  if the subjectId is itself identifying.
- **Why crypto-shredding still earns a place.** When an immutable event must legally carry the value
  *inline* (an integrity requirement, a signed/notarised event, an external contract that forbids
  externalising the field), you cannot externalise it; crypto-shredding is then the least-bad
  append-only-safe mechanism, with its key-destruction obligations made explicit (ADR-0009). That is
  the exception, not the default.

References: GDPR Art. 17, Recital 26; **EDPB Guidelines 01/2025 on pseudonymisation**; Verraes,
"Forgettable Payloads"; Dudycz / event-driven.io on GDPR in event-sourced systems; Greg Young's
original "crypto-shredding vs. forgettable payload" framing.

## Consequences

- **README repositioning (breaking to the narrative, not the API).** Forgettable payload is the
  headline erasure pattern for personal data; crypto-shredding is presented as the narrow exception.
  No existing class is removed or changed in signature, so adopters of ADR-0009 keep working.
- **New table `gdpr_forgettable_payload` (V3).** An adopter using the primary path applies one more
  migration and registers a `ForgettablePayloadStore` bean + a `ForgettablePayloadErasureHandler`,
  exactly mirroring the crypto-shredding wiring ergonomics.
- **Two erasure mechanisms now coexist.** The `CompositeSubjectErasureHandler` makes "erase across
  both" first-class; `ErasureService` already runs every registered handler, so registering both a
  forgettable and a crypto handler also covers the subject without the composite.
- **The `storage` axis is additive.** Generators and adopters can route a field to the external
  store from the annotation; the default `INLINE` keeps every prior annotation valid.
- **Residual risk is shifted, not removed.** The external store is now a critical asset with its own
  backup/retention discipline (the same hard rule the key store carries). Externalising does not
  anonymise the *reference*: if the subjectId is identifying and is itself stored in the immutable
  log, that is a separate minimisation question the adopter still owns.

## Alternatives considered

**Keep crypto-shredding as the primary pattern (ADR-0009 unchanged).** Rejected: it presents a
pseudonymisation technique as the default answer to an erasure (anonymisation) requirement, and ties
the compliance claim to a contested, operationally fragile total-key-destruction premise. Acceptable
only where the value must stay inline.

**Anonymise-in-place the external value to a "John Doe" placeholder instead of `DELETE`.** Kept as a
*supported variant* (the SPI's `erase` may overwrite rather than physically delete, useful when a
row must remain for referential integrity), not as the sole mechanism: a hard `DELETE` is the
strongest default, the placeholder is the escape hatch when a row must survive.

**Sentinel-at-projection only (strip PII on read).** Already rejected in ADR-0009 for crypto: it
leaves the raw PII in the store, so it is not erasure. Same verdict here; resolving a dangling
reference to empty is the forgettable-payload analogue and *does* erase, because the value was never
in the carrier to begin with.
