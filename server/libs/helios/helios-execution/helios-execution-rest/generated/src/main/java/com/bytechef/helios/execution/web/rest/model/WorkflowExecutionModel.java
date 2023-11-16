package com.bytechef.helios.execution.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.helios.execution.web.rest.model.JobModel;
import com.bytechef.helios.execution.web.rest.model.TriggerExecutionModel;
import com.bytechef.helios.execution.web.rest.model.WorkflowBasicModel;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-11-21T21:27:08.772308+01:00[Europe/Zagreb]")
public class WorkflowExecutionModel {

  private Long id;

  private JobModel job;

  private com.bytechef.helios.configuration.web.rest.model.ProjectModel project;

  private com.bytechef.helios.configuration.web.rest.model.ProjectInstanceModel projectInstance;

  private TriggerExecutionModel triggerExecution;

  private WorkflowBasicModel workflow;

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

  public WorkflowExecutionModel job(JobModel job) {
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

  public WorkflowExecutionModel projectInstance(com.bytechef.helios.configuration.web.rest.model.ProjectInstanceModel projectInstance) {
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
  public com.bytechef.helios.configuration.web.rest.model.ProjectInstanceModel getProjectInstance() {
    return projectInstance;
  }

  public void setProjectInstance(com.bytechef.helios.configuration.web.rest.model.ProjectInstanceModel projectInstance) {
    this.projectInstance = projectInstance;
  }

  public WorkflowExecutionModel triggerExecution(TriggerExecutionModel triggerExecution) {
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
  public TriggerExecutionModel getTriggerExecution() {
    return triggerExecution;
  }

  public void setTriggerExecution(TriggerExecutionModel triggerExecution) {
    this.triggerExecution = triggerExecution;
  }

  public WorkflowExecutionModel workflow(WorkflowBasicModel workflow) {
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

