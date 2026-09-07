package com.bytechef.automation.workflow.execution.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.lang.Nullable;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-04T22:06:08.943670+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class WorkflowExecutionBasicModel {

  private Long id;

  private com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel project;

  private com.bytechef.automation.configuration.web.rest.model.ProjectDeploymentBasicModel projectDeployment;

  private @Nullable com.bytechef.platform.workflow.execution.web.rest.model.JobBasicModel job;

  private @Nullable com.bytechef.platform.workflow.execution.web.rest.model.TriggerExecutionModel triggerExecution;

  private com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel workflow;

  public WorkflowExecutionBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WorkflowExecutionBasicModel(Long id, com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel project, com.bytechef.automation.configuration.web.rest.model.ProjectDeploymentBasicModel projectDeployment, com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel workflow) {
    this.id = id;
    this.project = project;
    this.projectDeployment = projectDeployment;
    this.workflow = workflow;
  }

  public WorkflowExecutionBasicModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a workflow execution.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a workflow execution.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  @JsonProperty("id")
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
  @NotNull @Valid 
  @Schema(name = "project", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("project")
  public com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel getProject() {
    return project;
  }

  @JsonProperty("project")
  public void setProject(com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel project) {
    this.project = project;
  }

  public WorkflowExecutionBasicModel projectDeployment(com.bytechef.automation.configuration.web.rest.model.ProjectDeploymentBasicModel projectDeployment) {
    this.projectDeployment = projectDeployment;
    return this;
  }

  /**
   * Get projectDeployment
   * @return projectDeployment
   */
  @NotNull @Valid 
  @Schema(name = "projectDeployment", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("projectDeployment")
  public com.bytechef.automation.configuration.web.rest.model.ProjectDeploymentBasicModel getProjectDeployment() {
    return projectDeployment;
  }

  @JsonProperty("projectDeployment")
  public void setProjectDeployment(com.bytechef.automation.configuration.web.rest.model.ProjectDeploymentBasicModel projectDeployment) {
    this.projectDeployment = projectDeployment;
  }

  public WorkflowExecutionBasicModel job(@Nullable com.bytechef.platform.workflow.execution.web.rest.model.JobBasicModel job) {
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
  public @Nullable com.bytechef.platform.workflow.execution.web.rest.model.JobBasicModel getJob() {
    return job;
  }

  @JsonProperty("job")
  public void setJob(@Nullable com.bytechef.platform.workflow.execution.web.rest.model.JobBasicModel job) {
    this.job = job;
  }

  public WorkflowExecutionBasicModel triggerExecution(@Nullable com.bytechef.platform.workflow.execution.web.rest.model.TriggerExecutionModel triggerExecution) {
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
  public @Nullable com.bytechef.platform.workflow.execution.web.rest.model.TriggerExecutionModel getTriggerExecution() {
    return triggerExecution;
  }

  @JsonProperty("triggerExecution")
  public void setTriggerExecution(@Nullable com.bytechef.platform.workflow.execution.web.rest.model.TriggerExecutionModel triggerExecution) {
    this.triggerExecution = triggerExecution;
  }

  public WorkflowExecutionBasicModel workflow(com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel workflow) {
    this.workflow = workflow;
    return this;
  }

  /**
   * Get workflow
   * @return workflow
   */
  @NotNull @Valid 
  @Schema(name = "workflow", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workflow")
  public com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel getWorkflow() {
    return workflow;
  }

  @JsonProperty("workflow")
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
        Objects.equals(this.projectDeployment, workflowExecutionBasic.projectDeployment) &&
        Objects.equals(this.job, workflowExecutionBasic.job) &&
        Objects.equals(this.triggerExecution, workflowExecutionBasic.triggerExecution) &&
        Objects.equals(this.workflow, workflowExecutionBasic.workflow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, project, projectDeployment, job, triggerExecution, workflow);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowExecutionBasicModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    project: ").append(toIndentedString(project)).append("\n");
    sb.append("    projectDeployment: ").append(toIndentedString(projectDeployment)).append("\n");
    sb.append("    job: ").append(toIndentedString(job)).append("\n");
    sb.append("    triggerExecution: ").append(toIndentedString(triggerExecution)).append("\n");
    sb.append("    workflow: ").append(toIndentedString(workflow)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

