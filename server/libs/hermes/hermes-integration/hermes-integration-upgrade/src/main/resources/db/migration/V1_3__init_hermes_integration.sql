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

CREATE TABLE IF NOT EXISTS integration (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    name                     VARCHAR(256) NOT NULL,
    description              TEXT         NULL,
    category_id              BIGINT       NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               TEXT         NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         TEXT         NOT NULL,
    version                  BIGINT       NOT NULL
);

CREATE TABLE IF NOT EXISTS integration_tag (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    integration_id           BIGSERIAL    NOT NULL,
    tag_id                   BIGSERIAL    NOT NULL
);

CREATE TABLE IF NOT EXISTS integration_workflow (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    integration_id           BIGSERIAL    NOT NULL,
    workflow_id              VARCHAR(256) NOT NULL
);

ALTER TABLE integration ADD CONSTRAINT fk_integration_category FOREIGN KEY (category_id) REFERENCES category (id);
ALTER TABLE integration_tag ADD CONSTRAINT fk_integration_tag_integration FOREIGN KEY (integration_id) REFERENCES integration (id);
ALTER TABLE integration_tag ADD CONSTRAINT fk_integration_tag_tag FOREIGN KEY (tag_id) REFERENCES tag (id);
ALTER TABLE integration_workflow ADD CONSTRAINT fk_integration_workflow_integration FOREIGN KEY (integration_id) REFERENCES integration (id);
