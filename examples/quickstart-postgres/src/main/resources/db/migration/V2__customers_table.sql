CREATE TABLE IF NOT EXISTS customers (
    id                VARCHAR(64)  PRIMARY KEY,
    full_name         VARCHAR(255),
    email             VARCHAR(255),
    tax_id            VARCHAR(64),
    health_condition  VARCHAR(255),
    created_at        TIMESTAMP    NOT NULL
);
