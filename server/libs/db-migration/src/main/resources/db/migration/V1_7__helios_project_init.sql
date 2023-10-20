CREATE TABLE IF NOT EXISTS project (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    name                     VARCHAR(256) NOT NULL,
    description              TEXT         NULL,
    project_version          INT          NOT NULL,
    status                   INT          NOT NULL,
    published_date           TIMESTAMP    NULL,
    category_id              BIGINT       NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         VARCHAR(256) NOT NULL,
    version                  BIGINT       NOT NULL
);

CREATE TABLE IF NOT EXISTS project_instance (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    project_id               BIGINT       NULL,
    name                     VARCHAR(256) NOT NULL,
    description              TEXT         NULL,
    status                   INT          NOT NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         VARCHAR(256) NOT NULL,
    version                  BIGINT       NOT NULL
);

CREATE TABLE IF NOT EXISTS project_instance_tag (
   id                       BIGSERIAL    NOT NULL PRIMARY KEY,
   project_instance_id      BIGSERIAL    NOT NULL,
   tag_id                   BIGSERIAL    NOT NULL
);

CREATE TABLE IF NOT EXISTS project_instance_workflow (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    project_instance_id      BIGSERIAL    NOT NULL,
    workflow_id              VARCHAR(256) NOT NULL,
    inputs                   TEXT         NOT NULL,
    enabled                  BOOLEAN      NOT NULL,
    last_execution_date      TIMESTAMP    NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         VARCHAR(256) NOT NULL,
    version                  BIGINT       NOT NULL
);

CREATE TABLE IF NOT EXISTS project_instance_workflow_connection (
    id                                BIGSERIAL    NOT NULL PRIMARY KEY,
    project_instance_workflow_id      BIGSERIAL    NOT NULL,
    connection_id                     BIGSERIAL    NOT NULL,
    key                               VARCHAR(256) NOT NULL,
    task_name                         VARCHAR(256) NOT NULL
);

CREATE TABLE IF NOT EXISTS project_instance_workflow_job (
    id                                BIGSERIAL    NOT NULL PRIMARY KEY,
    project_instance_workflow_id      BIGSERIAL    NOT NULL,
    job_id                            BIGSERIAL    NOT NULL
);

CREATE TABLE IF NOT EXISTS project_tag (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    project_id               BIGSERIAL    NOT NULL,
    tag_id                   BIGSERIAL    NOT NULL
);

CREATE TABLE IF NOT EXISTS project_workflow (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    project_id               BIGSERIAL    NOT NULL,
    workflow_id              VARCHAR(256) NOT NULL
);

ALTER TABLE project ADD CONSTRAINT uk_project_name UNIQUE (name);
ALTER TABLE project_instance ADD CONSTRAINT uk_project_instance_name UNIQUE (name);
ALTER TABLE project ADD CONSTRAINT fk_project_category FOREIGN KEY (category_id) REFERENCES category (id);
ALTER TABLE project_instance ADD CONSTRAINT fk_project_instance_project FOREIGN KEY (project_id) REFERENCES project (id);
ALTER TABLE project_instance_tag ADD CONSTRAINT fk_project_instance_tag_project_instance FOREIGN KEY (project_instance_id) REFERENCES project_instance (id);
ALTER TABLE project_instance_tag ADD CONSTRAINT fk_project_instance_tag_tag FOREIGN KEY (tag_id) REFERENCES tag (id);
ALTER TABLE project_instance_workflow ADD CONSTRAINT fk_project_instance_workflow_project_instance FOREIGN KEY (project_instance_id) REFERENCES project_instance (id);
ALTER TABLE project_instance_workflow_connection ADD CONSTRAINT fk_project_instance_workflow_connection_piw FOREIGN KEY (project_instance_workflow_id) REFERENCES project_instance_workflow (id);
ALTER TABLE project_instance_workflow_connection ADD CONSTRAINT fk_project_instance_workflow_connection_connection FOREIGN KEY (connection_id) REFERENCES connection (id);
ALTER TABLE project_instance_workflow_job ADD CONSTRAINT fk_project_instance_workflow_job_project_instance_workflow FOREIGN KEY (project_instance_workflow_id) REFERENCES project_instance_workflow (id);
ALTER TABLE project_instance_workflow_job ADD CONSTRAINT fk_project_instance_workflow_job_job FOREIGN KEY (job_id) REFERENCES job (id);
ALTER TABLE project_tag ADD CONSTRAINT fk_project_tag_project FOREIGN KEY (project_id) REFERENCES project (id);
ALTER TABLE project_tag ADD CONSTRAINT fk_project_tag_tag FOREIGN KEY (tag_id) REFERENCES tag (id);
ALTER TABLE project_workflow ADD CONSTRAINT fk_project_workflow_project FOREIGN KEY (project_id) REFERENCES project (id);
