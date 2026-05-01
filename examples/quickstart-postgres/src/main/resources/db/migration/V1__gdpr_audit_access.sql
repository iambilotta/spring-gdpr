-- spring-gdpr v0.1 baseline schema
-- Flyway-compatible migration. Apply via Flyway, Liquibase, or your own runner.
-- Tested against PostgreSQL 14+, MariaDB 10.6+, MySQL 8.0+, H2.
-- Oracle: replace BOOLEAN with NUMBER(1) and remove IF NOT EXISTS.

CREATE TABLE IF NOT EXISTS gdpr_audit_access (
    event_id          VARCHAR(64)  PRIMARY KEY,
    at_ts             TIMESTAMP    NOT NULL,
    actor             VARCHAR(255),
    subject_id        VARCHAR(255),
    target_type       VARCHAR(512) NOT NULL,
    target_member     VARCHAR(255),
    legal_basis       VARCHAR(64),
    special_category  BOOLEAN      NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_gdpr_audit_access_subject
    ON gdpr_audit_access (subject_id, at_ts);
