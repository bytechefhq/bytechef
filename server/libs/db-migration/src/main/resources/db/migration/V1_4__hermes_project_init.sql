CREATE TABLE IF NOT EXISTS project (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    name                     VARCHAR(256) NOT NULL,
    description              TEXT         NULL,
    project_version          INT          NOT NULL,
    status                   VARCHAR(256) NOT NULL,
    published_date           TIMESTAMP    NULL,
    category_id              BIGINT       NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               TEXT         NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         TEXT         NOT NULL,
    version                  BIGINT       NOT NULL
);

ALTER TABLE project ADD CONSTRAINT uk_project_name UNIQUE (name);

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

ALTER TABLE project ADD CONSTRAINT fk_project_category FOREIGN KEY (category_id) REFERENCES category (id);
ALTER TABLE project_tag ADD CONSTRAINT fk_project_tag_project FOREIGN KEY (project_id) REFERENCES project (id);
ALTER TABLE project_tag ADD CONSTRAINT fk_project_tag_tag FOREIGN KEY (tag_id) REFERENCES tag (id);
ALTER TABLE project_workflow ADD CONSTRAINT fk_project_workflow_project FOREIGN KEY (project_id) REFERENCES project (id);
