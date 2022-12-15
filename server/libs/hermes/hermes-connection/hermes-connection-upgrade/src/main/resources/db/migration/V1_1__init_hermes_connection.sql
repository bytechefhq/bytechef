CREATE TABLE IF NOT EXISTS connection (
    id                       VARCHAR(256) NOT NULL PRIMARY KEY,
    name                     VARCHAR(256) NOT NULL,
    component_name           VARCHAR(256) NOT NULL,
    component_version        INT          NOT NULL,
    authorization_name       VARCHAR(256) NULL,
    parameters               TEXT         NOT NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         VARCHAR(256) NOT NULL,
    version                  BIGINT       NOT NULL
);
