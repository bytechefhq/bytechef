CREATE TABLE IF NOT EXISTS integration (
    id                       VARCHAR(256) NOT NULL PRIMARY KEY,
    name                     VARCHAR(256) NOT NULL,
    description              TEXT         NOT NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               TEXT         NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         TEXT         NOT NULL,
    version                  BIGINT       NOT NULL
);

CREATE TABLE IF NOT EXISTS integration_workflow (
    id                       VARCHAR(256) NOT NULL PRIMARY KEY,
    integration_id           VARCHAR(256) NOT NULL,
    workflow_id              VARCHAR(256) NOT NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         VARCHAR(256) NOT NULL,
    version                  BIGINT       NOT NULL
);

ALTER TABLE integration_workflow ADD CONSTRAINT fk_integration_workflow_integration FOREIGN KEY (integration_id) REFERENCES integration (id);
