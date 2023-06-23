CREATE TABLE IF NOT EXISTS trigger_execution (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    status                   VARCHAR(256) NOT NULL,
    instance_id              BIGSERIAL    NOT NULL,
    instance_type            VARCHAR(256) NOT NULL,
    workflow_id              VARCHAR(256) NOT NULL,
    workflow_execution_id    VARCHAR(256) NOT NULL,
    priority                 INT          NOT NULL,
    start_date               TIMESTAMP        NULL,
    end_date                 TIMESTAMP        NULL,
    execution_time           INT          NOT NULL,
    workflow_trigger         TEXT         NOT NULL,
    output                   TEXT             NULL,
    error                    TEXT             NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         VARCHAR(256) NOT NULL
);

CREATE TABLE IF NOT EXISTS trigger_state (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    workflowExecutionId      VARCHAR(256) NOT NULL,
    value                    TEXT         NOT NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         VARCHAR(256) NOT NULL,
    version                  BIGINT       NOT NULL
);

