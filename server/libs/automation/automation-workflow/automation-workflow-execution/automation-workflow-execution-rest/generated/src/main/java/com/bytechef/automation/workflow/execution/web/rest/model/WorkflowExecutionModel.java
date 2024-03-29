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
 * Contains information about execution of a project workflow.
 */

@Schema(name = "WorkflowExecution", description = "Contains information about execution of a project workflow.")
@JsonTypeName("WorkflowExecution")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-03-29T14:33:24.765638+01:00[Europe/Zagreb]", comments = "Generator version: 7.4.0")
public class WorkflowExecutionModel {

  private Long id;

  private com.bytechef.platform.workflow.execution.web.rest.model.JobModel job;

  private com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel project;

  private com.bytechef.automation.configuration.web.rest.model.ProjectInstanceBasicModel projectInstance;

  private com.bytechef.platform.workflow.execution.web.rest.model.TriggerExecutionModel triggerExecution;

  private com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel workflow;

  public WorkflowExecutionModel id(Long id) {
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

  public WorkflowExecutionModel job(com.bytechef.platform.workflow.execution.web.rest.model.JobModel job) {
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
  public com.bytechef.platform.workflow.execution.web.rest.model.JobModel getJob() {
    return job;
  }

  public void setJob(com.bytechef.platform.workflow.execution.web.rest.model.JobModel job) {
    this.job = job;
  }

  public WorkflowExecutionModel project(com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel project) {
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

  public WorkflowExecutionModel projectInstance(com.bytechef.automation.configuration.web.rest.model.ProjectInstanceBasicModel projectInstance) {
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

  public WorkflowExecutionModel triggerExecution(com.bytechef.platform.workflow.execution.web.rest.model.TriggerExecutionModel triggerExecution) {
    this.triggerExecution = triggerExecution;
    return this;
  }

  /**
   * Get triggerExecution
   * @return triggerExecution
  */
  @Valid 
  @Schema(name = "triggerExecution", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("triggerExecution")
  public com.bytechef.platform.workflow.execution.web.rest.model.TriggerExecutionModel getTriggerExecution() {
    return triggerExecution;
  }

  public void setTriggerExecution(com.bytechef.platform.workflow.execution.web.rest.model.TriggerExecutionModel triggerExecution) {
    this.triggerExecution = triggerExecution;
  }

  public WorkflowExecutionModel workflow(com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel workflow) {
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
    WorkflowExecutionModel workflowExecution = (WorkflowExecutionModel) o;
    return Objects.equals(this.id, workflowExecution.id) &&
        Objects.equals(this.job, workflowExecution.job) &&
        Objects.equals(this.project, workflowExecution.project) &&
        Objects.equals(this.projectInstance, workflowExecution.projectInstance) &&
        Objects.equals(this.triggerExecution, workflowExecution.triggerExecution) &&
        Objects.equals(this.workflow, workflowExecution.workflow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, job, project, projectInstance, triggerExecution, workflow);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowExecutionModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    job: ").append(toIndentedString(job)).append("\n");
    sb.append("    project: ").append(toIndentedString(project)).append("\n");
    sb.append("    projectInstance: ").append(toIndentedString(projectInstance)).append("\n");
    sb.append("    triggerExecution: ").append(toIndentedString(triggerExecution)).append("\n");
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

