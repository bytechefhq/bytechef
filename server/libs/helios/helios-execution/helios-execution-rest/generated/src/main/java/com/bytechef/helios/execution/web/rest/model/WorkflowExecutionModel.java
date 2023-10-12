
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.helios.execution.web.rest.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.*;
import jakarta.annotation.Generated;

/**
 * Contains information about execution of a project workflow.
 */

@Schema(name = "WorkflowExecution", description = "Contains information about execution of a project workflow.")
@JsonTypeName("WorkflowExecution")
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-10-06T20:36:47.577089+02:00[Europe/Zagreb]")
public class WorkflowExecutionModel {

    private Long id;

    private com.bytechef.helios.configuration.web.rest.model.ProjectInstanceModel instance;

    private JobModel job;

    private com.bytechef.helios.configuration.web.rest.model.ProjectModel project;

    private WorkflowBasicModel workflow;

    public WorkflowExecutionModel id(Long id) {
        this.id = id;
        return this;
    }

    /**
     * The id of a workflow execution.
     * 
     * @return id
     */

    @Schema(
        name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a workflow execution.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkflowExecutionModel
        instance(com.bytechef.helios.configuration.web.rest.model.ProjectInstanceModel instance) {
        this.instance = instance;
        return this;
    }

    /**
     * Get instance
     * 
     * @return instance
     */
    @Valid
    @Schema(name = "instance", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("instance")
    public com.bytechef.helios.configuration.web.rest.model.ProjectInstanceModel getInstance() {
        return instance;
    }

    public void setInstance(com.bytechef.helios.configuration.web.rest.model.ProjectInstanceModel instance) {
        this.instance = instance;
    }

    public WorkflowExecutionModel job(JobModel job) {
        this.job = job;
        return this;
    }

    /**
     * Get job
     * 
     * @return job
     */
    @Valid
    @Schema(name = "job", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("job")
    public JobModel getJob() {
        return job;
    }

    public void setJob(JobModel job) {
        this.job = job;
    }

    public WorkflowExecutionModel project(com.bytechef.helios.configuration.web.rest.model.ProjectModel project) {
        this.project = project;
        return this;
    }

    /**
     * Get project
     * 
     * @return project
     */
    @Valid
    @Schema(name = "project", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("project")
    public com.bytechef.helios.configuration.web.rest.model.ProjectModel getProject() {
        return project;
    }

    public void setProject(com.bytechef.helios.configuration.web.rest.model.ProjectModel project) {
        this.project = project;
    }

    public WorkflowExecutionModel workflow(WorkflowBasicModel workflow) {
        this.workflow = workflow;
        return this;
    }

    /**
     * Get workflow
     * 
     * @return workflow
     */
    @Valid
    @Schema(name = "workflow", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("workflow")
    public WorkflowBasicModel getWorkflow() {
        return workflow;
    }

    public void setWorkflow(WorkflowBasicModel workflow) {
        this.workflow = workflow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkflowExecutionModel workflowExecution = (WorkflowExecutionModel) o;
        return Objects.equals(this.id, workflowExecution.id) &&
            Objects.equals(this.instance, workflowExecution.instance) &&
            Objects.equals(this.job, workflowExecution.job) &&
            Objects.equals(this.project, workflowExecution.project) &&
            Objects.equals(this.workflow, workflowExecution.workflow);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, instance, job, project, workflow);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class WorkflowExecutionModel {\n");
        sb.append("    id: ")
            .append(toIndentedString(id))
            .append("\n");
        sb.append("    instance: ")
            .append(toIndentedString(instance))
            .append("\n");
        sb.append("    job: ")
            .append(toIndentedString(job))
            .append("\n");
        sb.append("    project: ")
            .append(toIndentedString(project))
            .append("\n");
        sb.append("    workflow: ")
            .append(toIndentedString(workflow))
            .append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString()
            .replace("\n", "\n    ");
    }
}
