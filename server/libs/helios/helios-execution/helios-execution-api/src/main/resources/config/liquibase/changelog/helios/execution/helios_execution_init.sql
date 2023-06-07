CREATE TABLE IF NOT EXISTS project_instance_workflow_job (
    id                                BIGSERIAL    NOT NULL PRIMARY KEY,
    project_instance_workflow_id      BIGSERIAL    NOT NULL,
    job_id                            BIGSERIAL    NOT NULL
);

ALTER TABLE project_instance_workflow_job ADD CONSTRAINT fk_project_instance_workflow_job_project_instance_workflow FOREIGN KEY (project_instance_workflow_id) REFERENCES project_instance_workflow (id);
ALTER TABLE project_instance_workflow_job ADD CONSTRAINT fk_project_instance_workflow_job_job FOREIGN KEY (job_id) REFERENCES job (id);
