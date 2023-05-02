CREATE TABLE IF NOT EXISTS trigger_lifecycle (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    instance_id              VARCHAR(256) NULL,
    workflowExecutionId      VARCHAR(256) NOT NULL,
    value                    TEXT         NOT NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         VARCHAR(256) NOT NULL,
    version                  BIGINT       NOT NULL
);
