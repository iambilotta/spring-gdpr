# ADR-0009: Append-only-safe erasure via crypto-shredding

- **Status:** accepted
- **Date:** 2026-06-08
- **Deciders:** Francesco Bilotta

## Context

GDPR Article 17 requires the controller to render a subject's personal data no longer
recoverable on request. The existing erasure orchestration (ADR-0004) assumes **mutable**
stores: an `ErasureHandler` deletes, anonymizes, or pseudonymizes rows. That shell does not
cover an **append-only event store**, where the golden rule is "never edit or delete an
event": the very event that carries the PII cannot be `DELETE`d. Erasure there must be a
property **by design**, not a mutation of the log.

This is the gap REQ-GDPR-016 states (erasure against an append-only store, PII no longer
recoverable from any projection nor the raw event payload, audit trail preserved) and
REQ-GDPR-017 defers the strategy to this ADR, because the choice touches the event format
and is therefore a **one-way door** (changing it after the store is populated means
re-encrypting or migrating historical events).

The library ships **no** event-sourcing module today; this is the first one. The decision is
the same one already locked for the first real adopter (housetree `gest`, ADR-0007
"Erasure in an append-only event store", crypto-shredding, accepted 2026-06-08): this ADR is
the upstream, library-level record of that design so the mechanism can be contributed back as
a reusable `spring-gdpr` module rather than re-derived per adopter.

## Decision

Adopt **crypto-shredding**. PII inside events is encrypted with a **per-subject key**; the key
lives in a separate key table. Erasure is the **drop of that subject's key**: the ciphertext in
every event becomes irreversibly unreadable while the event stays byte-immutable. The event
store remains pure append-only; no event is ever touched.

Sentinel-at-projection (the projection refuses to surface the data) is kept as a
**complementary** technique, never as the erasure mechanism: on its own it leaves the raw PII in
the store, so "really erasing" it would require rewriting historical events, the exact
append-only violation we forbid.

The adversarial pass turned the following into **design constraints** (to satisfy before any
store is populated, because the reversal cost is high), not reasons to defer:

- **Per-subject key granularity** (not per-event): erasing a subject is then a single key drop.
- **Key loss = involuntary erasure**: the key table is a new critical asset. It is protected and
  backed up separately and reliably; losing a key is an unintended Article 17 erasure.
- **Backup of the event store must not resurrect a dropped key**: the drop propagates to backups,
  or the key backup carries the same retention as the erasure, so restoring the store never
  un-erases a subject.
- **Performance is measured, not assumed**: encryption on the write path and decryption on the
  replay path are benchmarked (the library already ships a JMH harness); the cost is reported,
  not hand-waved.
- **Upcasters evolve the schema around the opaque encrypted fields** without decrypting them.
- **Audit trail is preserved**: the erasure is itself a recorded fact (who, when, why), never a
  mutation of the log; the audit row proves the erasure happened even though no event changed.

## Consequences

- The event schema is constrained: PII fields are encrypted (or encryptable) from the **first**
  event of an event-sourced domain. Retrofitting encryption after events exist means migrating
  them, which is why the choice is made before adoption.
- A new critical asset appears: the key table, with its own lifecycle and a backup discipline
  **separate** from the event-store backup.
- Projections decrypt during replay only while the key exists; after a key drop, a replay yields
  the read model with that subject's PII fields blank/ciphertext, and the replay stays idempotent.
- Reversal cost is high (changing strategy after populating the store = re-encrypt/migrate every
  event). This is the reason REQ-GDPR-017 forced the decision into an ADR with an adversarial pass
  rather than a requirement.
- Implementation was gated on **human GREEN approval**: the RED tests that specify this behavior
  (`docs/requirements` REQ-GDPR-016) were committed `@Disabled("pending human GREEN approval,
  ADR-0009")`. With this ADR `accepted`, the encryption-at-write + key-lifecycle + replay code is
  written and the tests are enabled.

## Implementation (what shipped)

The reusable, store-agnostic mechanism lives in
`spring-gdpr-starter/.../erasure/crypto/`:

- `SubjectKeyStore` (SPI) — per-subject key lifecycle: `keyFor` / `getOrCreate` / `drop` /
  `exists`. Default `JdbcSubjectKeyStore` over a `gdpr_subject_key` table (migration
  `V2__gdpr_subject_key.sql`); `InMemorySubjectKeyStore` for tests. The SPI lets an adopter back
  it with a KMS / Secret Manager (the column is `wrapped_key`, ready for KEK-wrapping).
- `CryptoShredder` (SPI) + `AesGcmCryptoShredder` — field-level encrypt-on-write /
  decrypt-on-read. **Crypto choices**: AES-256-GCM (AEAD), a fresh 96-bit IV per record from
  `SecureRandom`, 128-bit auth tag, wire format `[version][IV][ciphertext||tag]`. Decrypt is
  **fail-closed**: a dropped key, a tampered ciphertext (failed GCM tag), a wrong-subject key, or
  malformed bytes all return `Optional.empty()`, never a partial plaintext, never a leaked cause.
  Plaintext and keys are never logged; key byte arrays are zeroed after use.
- `CryptoShreddingErasureHandler` — an `ErasureHandler` whose `erase(subjectId)` **drops the key**
  (the Art. 17 act) and writes an `AuditAccessRecord` (action `ERASURE`, legal basis `17`) to the
  `AuditSink`. The erasure is a recorded fact; no event is touched.

**Tombstone / no-resurrection**: `drop` records a tombstone (`erased_at`); a later `getOrCreate`
for an erased subject throws rather than minting a fresh key, so an erasure cannot be silently
undone.

**Key-backup constraint (re-stated as a hard rule)**: backups of `gdpr_subject_key` MUST honour
the erasure retention. A drop must propagate to every backup, or the key backup must carry the
same retention as the erasure SLA, so restoring the store can never un-erase a subject. The
default `JdbcSubjectKeyStore` stores the raw 256-bit key; protect the table at rest (DB-level
encryption, least-privilege grants) or wrap the key under a KMS-held KEK via the SPI.

## Alternatives considered

**Sentinel-at-projection + an upcaster that strips the payload.** The projection marks the subject
and shows a sentinel; an upcaster removes the PII from the payload on read. Rejected as the erasure
mechanism: if the upcaster acts only on read, the raw PII stays in the store, so it is not real
erasure; truly removing it would require rewriting historical events (a compaction), which violates
append-only and the golden rule "never alter past events". Useful only as a complement (the
projection should not expose the data anyway).

**Physical deletion of events.** Forbidden by the append-only invariant (a tampered, compacted log
is no longer a trustworthy audit source, and breaks every downstream token/replay assumption).

**No erasure, retention only.** Retention (Art. 5(1)(e), ADR-0005) expires data on a schedule; it
does not satisfy an Article 17 request to erase a specific subject on demand.

## References

- REQ-GDPR-016 / REQ-GDPR-017 in `docs/requirements/gdpr.requirements.md`.
- Adopter decision this mirrors: housetree `gest` ADR-0007 (crypto-shredding, accepted 2026-06-08).
- Event-sourcing golden rules in the consuming monorepo (event store is the only source of truth,
  append-only, projections rebuilt by replay).
- RED specification: `spring-gdpr-starter/src/test/java/.../erasure/CryptoShreddingErasureTest.java`
  (all `@Disabled` pending human GREEN approval).
