package com.bytechef.helios.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.helios.configuration.web.rest.model.WorkflowModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Contains information about execution of one of project workflows.
 */

@Schema(name = "ProjectWorkflowExecution", description = "Contains information about execution of one of project workflows.")
@JsonTypeName("ProjectWorkflowExecution")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-06-16T09:53:37.768123+02:00[Europe/Zagreb]")
public class ProjectWorkflowExecutionModel {

  private Long id;

  private com.bytechef.helios.configuration.web.rest.model.ProjectInstanceModel instance;

  private com.bytechef.hermes.execution.web.rest.model.JobModel job;

  private com.bytechef.helios.configuration.web.rest.model.ProjectModel project;

  private WorkflowModel workflow;

  public ProjectWorkflowExecutionModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a workflow execution.
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a workflow execution.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ProjectWorkflowExecutionModel instance(com.bytechef.helios.configuration.web.rest.model.ProjectInstanceModel instance) {
    this.instance = instance;
    return this;
  }

  /**
   * Get instance
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

  public ProjectWorkflowExecutionModel job(com.bytechef.hermes.execution.web.rest.model.JobModel job) {
    this.job = job;
    return this;
  }

  /**
   * Get job
   * @return job
  */
  @Valid 
  @Schema(name = "job", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("job")
  public com.bytechef.hermes.execution.web.rest.model.JobModel getJob() {
    return job;
  }

  public void setJob(com.bytechef.hermes.execution.web.rest.model.JobModel job) {
    this.job = job;
  }

  public ProjectWorkflowExecutionModel project(com.bytechef.helios.configuration.web.rest.model.ProjectModel project) {
    this.project = project;
    return this;
  }

  /**
   * Get project
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

  public ProjectWorkflowExecutionModel workflow(WorkflowModel workflow) {
    this.workflow = workflow;
    return this;
  }

  /**
   * Get workflow
   * @return workflow
  */
  @Valid 
  @Schema(name = "workflow", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflow")
  public WorkflowModel getWorkflow() {
    return workflow;
  }

  public void setWorkflow(WorkflowModel workflow) {
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
    ProjectWorkflowExecutionModel projectWorkflowExecution = (ProjectWorkflowExecutionModel) o;
    return Objects.equals(this.id, projectWorkflowExecution.id) &&
        Objects.equals(this.instance, projectWorkflowExecution.instance) &&
        Objects.equals(this.job, projectWorkflowExecution.job) &&
        Objects.equals(this.project, projectWorkflowExecution.project) &&
        Objects.equals(this.workflow, projectWorkflowExecution.workflow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, instance, job, project, workflow);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectWorkflowExecutionModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    instance: ").append(toIndentedString(instance)).append("\n");
    sb.append("    job: ").append(toIndentedString(job)).append("\n");
    sb.append("    project: ").append(toIndentedString(project)).append("\n");
    sb.append("    workflow: ").append(toIndentedString(workflow)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

