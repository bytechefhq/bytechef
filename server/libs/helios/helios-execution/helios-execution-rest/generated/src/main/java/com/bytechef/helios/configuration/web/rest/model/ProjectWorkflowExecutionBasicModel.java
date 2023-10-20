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

@Schema(name = "ProjectWorkflowExecutionBasic", description = "Contains information about execution of one of project workflows.")
@JsonTypeName("ProjectWorkflowExecutionBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-06-06T08:34:49.230359+02:00[Europe/Zagreb]")
public class ProjectWorkflowExecutionBasicModel {

  private Long id;

  private com.bytechef.helios.configuration.web.rest.model.ProjectInstanceBasicModel instance;

  private com.bytechef.hermes.execution.web.rest.model.JobBasicModel job;

  private com.bytechef.helios.configuration.web.rest.model.ProjectBasicModel project;

  private WorkflowModel workflow;

  public ProjectWorkflowExecutionBasicModel id(Long id) {
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

  public ProjectWorkflowExecutionBasicModel instance(com.bytechef.helios.configuration.web.rest.model.ProjectInstanceBasicModel instance) {
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
  public com.bytechef.helios.configuration.web.rest.model.ProjectInstanceBasicModel getInstance() {
    return instance;
  }

  public void setInstance(com.bytechef.helios.configuration.web.rest.model.ProjectInstanceBasicModel instance) {
    this.instance = instance;
  }

  public ProjectWorkflowExecutionBasicModel job(com.bytechef.hermes.execution.web.rest.model.JobBasicModel job) {
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
  public com.bytechef.hermes.execution.web.rest.model.JobBasicModel getJob() {
    return job;
  }

  public void setJob(com.bytechef.hermes.execution.web.rest.model.JobBasicModel job) {
    this.job = job;
  }

  public ProjectWorkflowExecutionBasicModel project(com.bytechef.helios.configuration.web.rest.model.ProjectBasicModel project) {
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
  public com.bytechef.helios.configuration.web.rest.model.ProjectBasicModel getProject() {
    return project;
  }

  public void setProject(com.bytechef.helios.configuration.web.rest.model.ProjectBasicModel project) {
    this.project = project;
  }

  public ProjectWorkflowExecutionBasicModel workflow(WorkflowModel workflow) {
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
    ProjectWorkflowExecutionBasicModel projectWorkflowExecutionBasic = (ProjectWorkflowExecutionBasicModel) o;
    return Objects.equals(this.id, projectWorkflowExecutionBasic.id) &&
        Objects.equals(this.instance, projectWorkflowExecutionBasic.instance) &&
        Objects.equals(this.job, projectWorkflowExecutionBasic.job) &&
        Objects.equals(this.project, projectWorkflowExecutionBasic.project) &&
        Objects.equals(this.workflow, projectWorkflowExecutionBasic.workflow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, instance, job, project, workflow);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectWorkflowExecutionBasicModel {\n");
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

