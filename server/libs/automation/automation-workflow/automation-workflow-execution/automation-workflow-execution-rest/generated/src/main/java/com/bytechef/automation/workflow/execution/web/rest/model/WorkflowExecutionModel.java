package com.bytechef.automation.workflow.execution.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-22T09:01:00.827116+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class WorkflowExecutionModel {

  private List<com.bytechef.platform.workflow.execution.web.rest.model.@Valid FocusPathElementModel> focusPath = new ArrayList<>();

  private Long id;

  private @Nullable com.bytechef.platform.workflow.execution.web.rest.model.JobModel job;

  private com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel project;

  private com.bytechef.automation.configuration.web.rest.model.ProjectDeploymentBasicModel projectDeployment;

  private @Nullable Integer projectVersion;

  private @Nullable Long taskExecutionCount;

  private @Nullable com.bytechef.platform.workflow.execution.web.rest.model.TriggerExecutionModel triggerExecution;

  private com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel workflow;

  public WorkflowExecutionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WorkflowExecutionModel(Long id, com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel project, com.bytechef.automation.configuration.web.rest.model.ProjectDeploymentBasicModel projectDeployment, com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel workflow) {
    this.id = id;
    this.project = project;
    this.projectDeployment = projectDeployment;
    this.workflow = workflow;
  }

  public WorkflowExecutionModel focusPath(List<com.bytechef.platform.workflow.execution.web.rest.model.@Valid FocusPathElementModel> focusPath) {
    this.focusPath = focusPath;
    return this;
  }

  public WorkflowExecutionModel addFocusPathItem(com.bytechef.platform.workflow.execution.web.rest.model.FocusPathElementModel focusPathItem) {
    if (this.focusPath == null) {
      this.focusPath = new ArrayList<>();
    }
    this.focusPath.add(focusPathItem);
    return this;
  }

  /**
   * The path from the root task executions to the task execution to focus, set when the execution is loaded lazily.
   * @return focusPath
   */
  
  @Schema(name = "focusPath", description = "The path from the root task executions to the task execution to focus, set when the execution is loaded lazily.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("focusPath")
  public List<com.bytechef.platform.workflow.execution.web.rest.model.@Valid FocusPathElementModel> getFocusPath() {
    return focusPath;
  }

  @JsonProperty("focusPath")
  public void setFocusPath(List<com.bytechef.platform.workflow.execution.web.rest.model.@Valid FocusPathElementModel> focusPath) {
    this.focusPath = focusPath;
  }

  public WorkflowExecutionModel id(Long id) {
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

  public WorkflowExecutionModel job(@Nullable com.bytechef.platform.workflow.execution.web.rest.model.JobModel job) {
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
  public @Nullable com.bytechef.platform.workflow.execution.web.rest.model.JobModel getJob() {
    return job;
  }

  @JsonProperty("job")
  public void setJob(@Nullable com.bytechef.platform.workflow.execution.web.rest.model.JobModel job) {
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

  public WorkflowExecutionModel projectDeployment(com.bytechef.automation.configuration.web.rest.model.ProjectDeploymentBasicModel projectDeployment) {
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

  public WorkflowExecutionModel projectVersion(@Nullable Integer projectVersion) {
    this.projectVersion = projectVersion;
    return this;
  }

  /**
   * The version of the project the workflow execution ran.
   * @return projectVersion
   */
  
  @Schema(name = "projectVersion", accessMode = Schema.AccessMode.READ_ONLY, description = "The version of the project the workflow execution ran.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectVersion")
  public @Nullable Integer getProjectVersion() {
    return projectVersion;
  }

  @JsonProperty("projectVersion")
  public void setProjectVersion(@Nullable Integer projectVersion) {
    this.projectVersion = projectVersion;
  }

  public WorkflowExecutionModel taskExecutionCount(@Nullable Long taskExecutionCount) {
    this.taskExecutionCount = taskExecutionCount;
    return this;
  }

  /**
   * The total number of task executions of the job and its subflow jobs, set when the execution is loaded lazily.
   * @return taskExecutionCount
   */
  
  @Schema(name = "taskExecutionCount", accessMode = Schema.AccessMode.READ_ONLY, description = "The total number of task executions of the job and its subflow jobs, set when the execution is loaded lazily.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("taskExecutionCount")
  public @Nullable Long getTaskExecutionCount() {
    return taskExecutionCount;
  }

  @JsonProperty("taskExecutionCount")
  public void setTaskExecutionCount(@Nullable Long taskExecutionCount) {
    this.taskExecutionCount = taskExecutionCount;
  }

  public WorkflowExecutionModel triggerExecution(@Nullable com.bytechef.platform.workflow.execution.web.rest.model.TriggerExecutionModel triggerExecution) {
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

  public WorkflowExecutionModel workflow(com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel workflow) {
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
    WorkflowExecutionModel workflowExecution = (WorkflowExecutionModel) o;
    return Objects.equals(this.focusPath, workflowExecution.focusPath) &&
        Objects.equals(this.id, workflowExecution.id) &&
        Objects.equals(this.job, workflowExecution.job) &&
        Objects.equals(this.project, workflowExecution.project) &&
        Objects.equals(this.projectDeployment, workflowExecution.projectDeployment) &&
        Objects.equals(this.projectVersion, workflowExecution.projectVersion) &&
        Objects.equals(this.taskExecutionCount, workflowExecution.taskExecutionCount) &&
        Objects.equals(this.triggerExecution, workflowExecution.triggerExecution) &&
        Objects.equals(this.workflow, workflowExecution.workflow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(focusPath, id, job, project, projectDeployment, projectVersion, taskExecutionCount, triggerExecution, workflow);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowExecutionModel {\n");
    sb.append("    focusPath: ").append(toIndentedString(focusPath)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    job: ").append(toIndentedString(job)).append("\n");
    sb.append("    project: ").append(toIndentedString(project)).append("\n");
    sb.append("    projectDeployment: ").append(toIndentedString(projectDeployment)).append("\n");
    sb.append("    projectVersion: ").append(toIndentedString(projectVersion)).append("\n");
    sb.append("    taskExecutionCount: ").append(toIndentedString(taskExecutionCount)).append("\n");
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

