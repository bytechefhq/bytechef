CREATE TABLE IF NOT EXISTS tag (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    name                     VARCHAR(256) NOT NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               TEXT         NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         TEXT         NOT NULL,
    version                  BIGINT       NOT NULL
);

ALTER TABLE tag ADD CONSTRAINT uk_tag_name UNIQUE (name);
