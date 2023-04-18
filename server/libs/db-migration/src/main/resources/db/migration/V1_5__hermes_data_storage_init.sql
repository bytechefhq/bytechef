CREATE TABLE IF NOT EXISTS data_storage (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    workflow_id              VARCHAR(256) NULL,
    instance_id              BIGINT       NULL,
    job_id                   BIGINT       NULL,
    key                      VARCHAR(256) NOT NULL,
    scope                    INT          NOT NULL,
    value                    TEXT         NOT NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         VARCHAR(256) NOT NULL,
    version                  BIGINT       NOT NULL
);

ALTER TABLE data_storage ADD CONSTRAINT uk_data_storage_key UNIQUE (key, scope);
