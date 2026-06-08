-- spring-gdpr crypto-shredding key store (ADR-0009, REQ-GDPR-016/017).
-- Flyway-compatible migration. Apply via Flyway, Liquibase, or your own runner.
-- Tested against PostgreSQL 14+, MariaDB 10.6+, MySQL 8.0+, H2.
-- Oracle: replace VARBINARY with RAW/BLOB and remove IF NOT EXISTS.

-- Per-subject AES-256 key. Erasure (GDPR Art.17) = drop the key: NULL wrapped_key + stamp
-- erased_at. The row is kept as a tombstone so the erasure is a recorded fact and a re-mint
-- cannot resurrect an erased subject.
--
-- KEY-BACKUP CONSTRAINT (ADR-0009, hard): backups of THIS table must honour the erasure
-- retention. A drop must propagate to every backup (or the backup must carry the same retention
-- as the erasure), so restoring it can never un-erase a subject. Do NOT back this table up with a
-- longer retention than the erasure SLA. wrapped_key may itself be KEK-wrapped by a KMS-backed
-- SubjectKeyStore implementation; this default stores the raw key, so protect the table at rest.
CREATE TABLE IF NOT EXISTS gdpr_subject_key (
    subject_id   VARCHAR(255)  PRIMARY KEY,
    wrapped_key  VARBINARY(255),               -- NULL once the subject is erased
    created_at   TIMESTAMP     NOT NULL,
    erased_at    TIMESTAMP                      -- non-NULL = erased (tombstone)
);
