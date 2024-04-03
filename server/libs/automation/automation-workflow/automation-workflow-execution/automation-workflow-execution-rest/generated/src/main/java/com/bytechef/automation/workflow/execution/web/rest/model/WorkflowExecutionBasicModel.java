package com.bytechef.automation.workflow.execution.web.rest.model;

import java.net.URI;
import java.util.Objects;
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
 * Contains information about execution of a Integration workflow.
 */

@Schema(name = "WorkflowExecutionBasic", description = "Contains information about execution of a Integration workflow.")
@JsonTypeName("WorkflowExecutionBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-04-03T19:49:17.559872+02:00[Europe/Zagreb]", comments = "Generator version: 7.4.0")
public class WorkflowExecutionBasicModel {

  private Long id;

  private com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel project;

  private com.bytechef.automation.configuration.web.rest.model.ProjectInstanceBasicModel projectInstance;

  private com.bytechef.platform.workflow.execution.web.rest.model.JobBasicModel job;

  private com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel workflow;

  public WorkflowExecutionBasicModel id(Long id) {
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

  public WorkflowExecutionBasicModel project(com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel project) {
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
  public com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel getProject() {
    return project;
  }

  public void setProject(com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel project) {
    this.project = project;
  }

  public WorkflowExecutionBasicModel projectInstance(com.bytechef.automation.configuration.web.rest.model.ProjectInstanceBasicModel projectInstance) {
    this.projectInstance = projectInstance;
    return this;
  }

  /**
   * Get projectInstance
   * @return projectInstance
  */
  @Valid 
  @Schema(name = "projectInstance", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectInstance")
  public com.bytechef.automation.configuration.web.rest.model.ProjectInstanceBasicModel getProjectInstance() {
    return projectInstance;
  }

  public void setProjectInstance(com.bytechef.automation.configuration.web.rest.model.ProjectInstanceBasicModel projectInstance) {
    this.projectInstance = projectInstance;
  }

  public WorkflowExecutionBasicModel job(com.bytechef.platform.workflow.execution.web.rest.model.JobBasicModel job) {
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
  public com.bytechef.platform.workflow.execution.web.rest.model.JobBasicModel getJob() {
    return job;
  }

  public void setJob(com.bytechef.platform.workflow.execution.web.rest.model.JobBasicModel job) {
    this.job = job;
  }

  public WorkflowExecutionBasicModel workflow(com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel workflow) {
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
  public com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel getWorkflow() {
    return workflow;
  }

  public void setWorkflow(com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel workflow) {
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
    WorkflowExecutionBasicModel workflowExecutionBasic = (WorkflowExecutionBasicModel) o;
    return Objects.equals(this.id, workflowExecutionBasic.id) &&
        Objects.equals(this.project, workflowExecutionBasic.project) &&
        Objects.equals(this.projectInstance, workflowExecutionBasic.projectInstance) &&
        Objects.equals(this.job, workflowExecutionBasic.job) &&
        Objects.equals(this.workflow, workflowExecutionBasic.workflow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, project, projectInstance, job, workflow);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowExecutionBasicModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    project: ").append(toIndentedString(project)).append("\n");
    sb.append("    projectInstance: ").append(toIndentedString(projectInstance)).append("\n");
    sb.append("    job: ").append(toIndentedString(job)).append("\n");
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

