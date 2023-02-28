CREATE TABLE IF NOT EXISTS category (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    name                     VARCHAR(256) NOT NULL,
    description              TEXT         NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               TEXT         NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         TEXT         NOT NULL,
    version                  BIGINT       NOT NULL
);

ALTER TABLE category ADD CONSTRAINT uk_category_name UNIQUE (name);
