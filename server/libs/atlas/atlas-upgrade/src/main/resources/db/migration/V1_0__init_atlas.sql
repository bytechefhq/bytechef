CREATE TABLE IF NOT EXISTS context (
    id                       VARCHAR(256) NOT NULL PRIMARY KEY,
    stack_id                 VARCHAR(256) NOT NULL,
    value                    TEXT         NOT NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_context_stack_id ON context (stack_id);

CREATE TABLE IF NOT EXISTS counter (
    id                       VARCHAR(256) NOT NULL PRIMARY KEY,
    value                    BIGINT       NOT NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL
    );

CREATE TABLE IF NOT EXISTS job (
    id                       VARCHAR(256) NOT NULL PRIMARY KEY,
    workflow_id              VARCHAR(256) NOT NULL,
    parent_task_execution_id VARCHAR(256)     NULL,
    status                   VARCHAR(256) NOT NULL,
    label                    VARCHAR(256)     NULL,
    current_task             INT          NOT NULL,
    start_time               TIMESTAMP        NULL,
    end_time                 TIMESTAMP        NULL,
    priority                 INT          NOT NULL,
    inputs                   TEXT             NULL,
    outputs                  TEXT             NULL,
    webhooks                 TEXT             NULL,
    error                    TEXT             NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         VARCHAR(256) NOT NULL,
    version                  BIGINT       NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_job_create_time ON job (created_date);
CREATE INDEX IF NOT EXISTS idx_job_status ON job (status);

CREATE TABLE IF NOT EXISTS task_execution (
    id                       VARCHAR(256) NOT NULL PRIMARY KEY,
    job_id                   VARCHAR(256) NOT NULL,
    parent_id                VARCHAR(256)     NULL,
    status                   VARCHAR(256) NOT NULL,
    progress                 INT not          NULL,
    priority                 INT          NOT NULL,
    retry                    INT          NOT NULL,
    retry_attempts           INT          NOT NULL,
    retry_delay              VARCHAR(256) NOT NULL,
    retry_delay_factor       INT          NOT NULL,
    start_time               TIMESTAMP        NULL,
    end_time                 TIMESTAMP        NULL,
    execution_time           INT          NOT NULL,
    error                    TEXT             NULL,
    workflow_task            TEXT         NOT NULL,
    task_number              INT          NOT NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         VARCHAR(256) NOT NULL,
    version                  BIGINT       NOT NULL
    );

CREATE TABLE IF NOT EXISTS workflow (
    id					     VARCHAR(256) NOT NULL PRIMARY KEY,
    definition    			     TEXT         NOT NULL,
    format     			     VARCHAR(256) NOT NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         VARCHAR(256) NOT NULL,
    version                  BIGINT       NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_task_execution_job_id ON task_execution (job_id);

ALTER TABLE job ADD CONSTRAINT fk_job_task_execution FOREIGN KEY (parent_task_execution_id) REFERENCES task_execution (id);
ALTER TABLE task_execution ADD CONSTRAINT fk_task_execution_job FOREIGN KEY (job_id) REFERENCES job (id);
ALTER TABLE task_execution ADD CONSTRAINT fk_task_execution_task_execution FOREIGN KEY (parent_id) REFERENCES task_execution (id);
