-- spring-gdpr forgettable-payload external PII store (ADR-0010, REQ-GDPR-022).
-- Flyway-compatible migration. Apply via Flyway, Liquibase, or your own runner.
-- Tested against PostgreSQL 14+, MariaDB 10.6+, MySQL 8.0+, H2.
-- Oracle: replace IF NOT EXISTS and adjust VARCHAR sizes (4000-byte limit) as needed.

-- The mutable external store for personal-data fields marked storage = FORGETTABLE_PAYLOAD.
-- The domain object / event carries only a ForgettablePayloadReference (subject_id + field_key);
-- the value lives ONLY here. This is the library's PRIMARY personal-data erasure path.
--
-- Erasure (GDPR Art.17) = an actual DELETE of the subject's value rows + a tombstone (the row with
-- the reserved field_key and a non-NULL erased_at). Deleting the value is anonymisation; this is
-- preferred over crypto-shredding, whose key-drop leaves ciphertext that is, in law, pseudonymised
-- personal data (Recital 26, EDPB 01/2025). Crypto-shredding (V2) stays the narrow exception for an
-- immutable event that must legally carry the value inline.
--
-- TOMBSTONE: the per-subject erased state is a reserved row (field_key = ' __erased__',
-- erased_at non-NULL). It is what makes the erasure a recorded fact and stops a later write from
-- silently re-creating an erased subject's data (no resurrection).
--
-- payload_value is named with a prefix because "value" is a reserved word in several engines
-- (H2, MySQL); it holds the externalised PII in clear text at rest, so protect this table at rest
-- (DB-level encryption, least-privilege grants) exactly as you would the data it replaces inline.
CREATE TABLE IF NOT EXISTS gdpr_forgettable_payload (
    subject_id    VARCHAR(255)  NOT NULL,
    field_key     VARCHAR(255)  NOT NULL,
    payload_value VARCHAR(4096),                -- the externalised PII; NULL on the tombstone row
    updated_at    TIMESTAMP     NOT NULL,
    erased_at     TIMESTAMP,                    -- non-NULL on the reserved tombstone row = erased
    PRIMARY KEY (subject_id, field_key)
);
