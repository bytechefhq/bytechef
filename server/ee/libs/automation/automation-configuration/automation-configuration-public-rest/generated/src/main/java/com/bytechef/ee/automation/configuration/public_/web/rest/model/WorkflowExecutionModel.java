package com.bytechef.ee.automation.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.ExecutionErrorModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.ProjectDeploymentReferenceModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.ProjectReferenceModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.TaskExecutionModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.WorkflowExecutionStatusModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.WorkflowReferenceModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The full detail of a workflow execution, including inputs, outputs, error and task executions.
 */

@Schema(name = "WorkflowExecution", description = "The full detail of a workflow execution, including inputs, outputs, error and task executions.")
@JsonTypeName("WorkflowExecution")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-18T16:40:02.539074685Z[Etc/UTC]", comments = "Generator version: 7.22.0")
public class WorkflowExecutionModel {

  private Long id;

  private @Nullable WorkflowExecutionStatusModel status;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime startDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime endDate;

  private @Nullable ExecutionErrorModel error;

  @Valid
  private Map<String, Object> inputs = new HashMap<>();

  @Valid
  private Map<String, Object> outputs = new HashMap<>();

  private @Nullable ProjectReferenceModel project;

  private @Nullable ProjectDeploymentReferenceModel projectDeployment;

  private @Nullable WorkflowReferenceModel workflow;

  @Valid
  private List<@Valid TaskExecutionModel> taskExecutions = new ArrayList<>();

  public WorkflowExecutionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WorkflowExecutionModel(Long id) {
    this.id = id;
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

  public WorkflowExecutionModel status(@Nullable WorkflowExecutionStatusModel status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @Valid 
  @Schema(name = "status", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("status")
  public @Nullable WorkflowExecutionStatusModel getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable WorkflowExecutionStatusModel status) {
    this.status = status;
  }

  public WorkflowExecutionModel startDate(@Nullable OffsetDateTime startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * The instant an execution started.
   * @return startDate
   */
  @Valid 
  @Schema(name = "startDate", description = "The instant an execution started.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("startDate")
  public @Nullable OffsetDateTime getStartDate() {
    return startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(@Nullable OffsetDateTime startDate) {
    this.startDate = startDate;
  }

  public WorkflowExecutionModel endDate(@Nullable OffsetDateTime endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * The instant an execution ended. Null while the execution is running.
   * @return endDate
   */
  @Valid 
  @Schema(name = "endDate", description = "The instant an execution ended. Null while the execution is running.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("endDate")
  public @Nullable OffsetDateTime getEndDate() {
    return endDate;
  }

  @JsonProperty("endDate")
  public void setEndDate(@Nullable OffsetDateTime endDate) {
    this.endDate = endDate;
  }

  public WorkflowExecutionModel error(@Nullable ExecutionErrorModel error) {
    this.error = error;
    return this;
  }

  /**
   * Get error
   * @return error
   */
  @Valid 
  @Schema(name = "error", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("error")
  public @Nullable ExecutionErrorModel getError() {
    return error;
  }

  @JsonProperty("error")
  public void setError(@Nullable ExecutionErrorModel error) {
    this.error = error;
  }

  public WorkflowExecutionModel inputs(Map<String, Object> inputs) {
    this.inputs = inputs;
    return this;
  }

  public WorkflowExecutionModel putInputsItem(String key, Object inputsItem) {
    if (this.inputs == null) {
      this.inputs = new HashMap<>();
    }
    this.inputs.put(key, inputsItem);
    return this;
  }

  /**
   * The inputs the execution ran with.
   * @return inputs
   */
  
  @Schema(name = "inputs", description = "The inputs the execution ran with.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
  public Map<String, Object> getInputs() {
    return inputs;
  }

  @JsonProperty("inputs")
  public void setInputs(Map<String, Object> inputs) {
    this.inputs = inputs;
  }

  public WorkflowExecutionModel outputs(Map<String, Object> outputs) {
    this.outputs = outputs;
    return this;
  }

  public WorkflowExecutionModel putOutputsItem(String key, Object outputsItem) {
    if (this.outputs == null) {
      this.outputs = new HashMap<>();
    }
    this.outputs.put(key, outputsItem);
    return this;
  }

  /**
   * The outputs the execution produced.
   * @return outputs
   */
  
  @Schema(name = "outputs", description = "The outputs the execution produced.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("outputs")
  public Map<String, Object> getOutputs() {
    return outputs;
  }

  @JsonProperty("outputs")
  public void setOutputs(Map<String, Object> outputs) {
    this.outputs = outputs;
  }

  public WorkflowExecutionModel project(@Nullable ProjectReferenceModel project) {
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
  public @Nullable ProjectReferenceModel getProject() {
    return project;
  }

  @JsonProperty("project")
  public void setProject(@Nullable ProjectReferenceModel project) {
    this.project = project;
  }

  public WorkflowExecutionModel projectDeployment(@Nullable ProjectDeploymentReferenceModel projectDeployment) {
    this.projectDeployment = projectDeployment;
    return this;
  }

  /**
   * Get projectDeployment
   * @return projectDeployment
   */
  @Valid 
  @Schema(name = "projectDeployment", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectDeployment")
  public @Nullable ProjectDeploymentReferenceModel getProjectDeployment() {
    return projectDeployment;
  }

  @JsonProperty("projectDeployment")
  public void setProjectDeployment(@Nullable ProjectDeploymentReferenceModel projectDeployment) {
    this.projectDeployment = projectDeployment;
  }

  public WorkflowExecutionModel workflow(@Nullable WorkflowReferenceModel workflow) {
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
  public @Nullable WorkflowReferenceModel getWorkflow() {
    return workflow;
  }

  @JsonProperty("workflow")
  public void setWorkflow(@Nullable WorkflowReferenceModel workflow) {
    this.workflow = workflow;
  }

  public WorkflowExecutionModel taskExecutions(List<@Valid TaskExecutionModel> taskExecutions) {
    this.taskExecutions = taskExecutions;
    return this;
  }

  public WorkflowExecutionModel addTaskExecutionsItem(TaskExecutionModel taskExecutionsItem) {
    if (this.taskExecutions == null) {
      this.taskExecutions = new ArrayList<>();
    }
    this.taskExecutions.add(taskExecutionsItem);
    return this;
  }

  /**
   * The executions of the workflow's tasks.
   * @return taskExecutions
   */
  @Valid 
  @Schema(name = "taskExecutions", description = "The executions of the workflow's tasks.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("taskExecutions")
  public List<@Valid TaskExecutionModel> getTaskExecutions() {
    return taskExecutions;
  }

  @JsonProperty("taskExecutions")
  public void setTaskExecutions(List<@Valid TaskExecutionModel> taskExecutions) {
    this.taskExecutions = taskExecutions;
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
        Objects.equals(this.status, workflowExecution.status) &&
        Objects.equals(this.startDate, workflowExecution.startDate) &&
        Objects.equals(this.endDate, workflowExecution.endDate) &&
        Objects.equals(this.error, workflowExecution.error) &&
        Objects.equals(this.inputs, workflowExecution.inputs) &&
        Objects.equals(this.outputs, workflowExecution.outputs) &&
        Objects.equals(this.project, workflowExecution.project) &&
        Objects.equals(this.projectDeployment, workflowExecution.projectDeployment) &&
        Objects.equals(this.workflow, workflowExecution.workflow) &&
        Objects.equals(this.taskExecutions, workflowExecution.taskExecutions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, status, startDate, endDate, error, inputs, outputs, project, projectDeployment, workflow, taskExecutions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowExecutionModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    outputs: ").append(toIndentedString(outputs)).append("\n");
    sb.append("    project: ").append(toIndentedString(project)).append("\n");
    sb.append("    projectDeployment: ").append(toIndentedString(projectDeployment)).append("\n");
    sb.append("    workflow: ").append(toIndentedString(workflow)).append("\n");
    sb.append("    taskExecutions: ").append(toIndentedString(taskExecutions)).append("\n");
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

