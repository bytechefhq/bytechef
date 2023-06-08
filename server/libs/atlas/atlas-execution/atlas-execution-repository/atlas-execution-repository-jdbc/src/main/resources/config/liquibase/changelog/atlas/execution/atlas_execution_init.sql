CREATE TABLE IF NOT EXISTS context (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    stack_id                 BIGINT       NOT NULL,
    sub_stack_id             BIGINT           NULL,
    classname_id             INT          NOT NULL,
    value                    TEXT         NOT NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL
    );

CREATE TABLE IF NOT EXISTS counter (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    value                    BIGINT       NOT NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL
    );

CREATE TABLE IF NOT EXISTS job (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    workflow_id              VARCHAR(256) NOT NULL,
    parent_task_execution_id BIGINT           NULL,
    status                   INT          NOT NULL,
    label                    VARCHAR(256)     NULL,
    current_task             INT          NOT NULL,
    start_date               TIMESTAMP        NULL,
    end_date                 TIMESTAMP        NULL,
    priority                 INT          NOT NULL,
    inputs                   TEXT             NULL,
    outputs                  TEXT             NULL,
    webhooks                 TEXT             NULL,
    error                    TEXT             NULL,
    metadata                 TEXT             NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         VARCHAR(256) NOT NULL,
    version                  BIGINT       NOT NULL
    );

CREATE TABLE IF NOT EXISTS task_execution (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    job_id                   BIGSERIAL    NOT NULL,
    parent_id                BIGINT           NULL,
    status                   VARCHAR(256) NOT NULL,
    progress                 INT          NOT NULL,
    priority                 INT          NOT NULL,
    max_retries              INT          NOT NULL,
    retry_attempts           INT          NOT NULL,
    retry_delay              VARCHAR(256) NOT NULL,
    retry_delay_factor       INT          NOT NULL,
    start_date               TIMESTAMP        NULL,
    end_date                 TIMESTAMP        NULL,
    execution_time           INT          NOT NULL,
    workflow_task            TEXT         NOT NULL,
    task_number              INT          NOT NULL,
    output                   TEXT             NULL,
    error                    TEXT             NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         VARCHAR(256) NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_context_stack_id ON context (stack_id, classname_id);
CREATE INDEX IF NOT EXISTS idx_context_stack_id_sub_stack_id ON context (stack_id, sub_stack_id, classname_id);
CREATE INDEX IF NOT EXISTS idx_job_created_date ON job (created_date);
CREATE INDEX IF NOT EXISTS idx_job_status ON job (status);
CREATE INDEX IF NOT EXISTS idx_task_execution_job_id ON task_execution (job_id);

ALTER TABLE job ADD CONSTRAINT fk_job_task_execution FOREIGN KEY (parent_task_execution_id) REFERENCES task_execution (id);
ALTER TABLE task_execution ADD CONSTRAINT fk_task_execution_job FOREIGN KEY (job_id) REFERENCES job (id);
ALTER TABLE task_execution ADD CONSTRAINT fk_task_execution_task_execution FOREIGN KEY (parent_id) REFERENCES task_execution (id);
ALTER TABLE counter ADD CONSTRAINT fk_counter_task_execution FOREIGN KEY (id) REFERENCES task_execution (id);
