package com.bytechef.platform.workflow.execution.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.workflow.execution.web.rest.model.ExecutionErrorModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Adds execution semantics to a task.
 */

@Schema(name = "TaskExecution", description = "Adds execution semantics to a task.")
@JsonTypeName("TaskExecution")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-03-06T06:11:29.846980+01:00[Europe/Zagreb]")
public class TaskExecutionModel {

  private com.bytechef.platform.configuration.web.rest.model.ComponentDefinitionBasicModel component;

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime endDate;

  private ExecutionErrorModel error;

  private Long executionTime;

  private String id;

  @Valid
  private Map<String, Object> input = new HashMap<>();

  private String jobId;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  private Integer maxRetries;

  private Object output;

  private String parentId;

  private Integer priority;

  private Integer progress;

  private Integer retryAttempts;

  private String retryDelay;

  private Integer retryDelayFactor;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime startDate;

  /**
   * The current status of a task.
   */
  public enum StatusEnum {
    CREATED("CREATED"),
    
    STARTED("STARTED"),
    
    FAILED("FAILED"),
    
    CANCELLED("CANCELLED"),
    
    COMPLETED("COMPLETED");

    private String value;

    StatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private StatusEnum status;

  private Integer taskNumber;

  private Long retryDelayMillis;

  private com.bytechef.platform.configuration.web.rest.model.WorkflowTaskModel workflowTask;

  private String type;

  public TaskExecutionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TaskExecutionModel(String jobId, Integer priority, LocalDateTime startDate, StatusEnum status) {
    this.jobId = jobId;
    this.priority = priority;
    this.startDate = startDate;
    this.status = status;
  }

  public TaskExecutionModel component(com.bytechef.platform.configuration.web.rest.model.ComponentDefinitionBasicModel component) {
    this.component = component;
    return this;
  }

  /**
   * Get component
   * @return component
  */
  @Valid 
  @Schema(name = "component", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("component")
  public com.bytechef.platform.configuration.web.rest.model.ComponentDefinitionBasicModel getComponent() {
    return component;
  }

  public void setComponent(com.bytechef.platform.configuration.web.rest.model.ComponentDefinitionBasicModel component) {
    this.component = component;
  }

  public TaskExecutionModel createdBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * The created by.
   * @return createdBy
  */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The created by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdBy")
  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public TaskExecutionModel createdDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  /**
   * The created date.
   * @return createdDate
  */
  @Valid 
  @Schema(name = "createdDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The created date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdDate")
  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public TaskExecutionModel endDate(LocalDateTime endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * The time when a task instance ended (CANCELLED, FAILED, COMPLETED).
   * @return endDate
  */
  @Valid 
  @Schema(name = "endDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The time when a task instance ended (CANCELLED, FAILED, COMPLETED).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("endDate")
  public LocalDateTime getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDateTime endDate) {
    this.endDate = endDate;
  }

  public TaskExecutionModel error(ExecutionErrorModel error) {
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
  public ExecutionErrorModel getError() {
    return error;
  }

  public void setError(ExecutionErrorModel error) {
    this.error = error;
  }

  public TaskExecutionModel executionTime(Long executionTime) {
    this.executionTime = executionTime;
    return this;
  }

  /**
   * The total time in ms for a task to execute (excluding wait time of the task in transit). i.e. actual execution time on a worker node.
   * @return executionTime
  */
  
  @Schema(name = "executionTime", accessMode = Schema.AccessMode.READ_ONLY, description = "The total time in ms for a task to execute (excluding wait time of the task in transit). i.e. actual execution time on a worker node.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("executionTime")
  public Long getExecutionTime() {
    return executionTime;
  }

  public void setExecutionTime(Long executionTime) {
    this.executionTime = executionTime;
  }

  public TaskExecutionModel id(String id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a task execution.
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a task execution.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public TaskExecutionModel input(Map<String, Object> input) {
    this.input = input;
    return this;
  }

  public TaskExecutionModel putInputItem(String key, Object inputItem) {
    if (this.input == null) {
      this.input = new HashMap<>();
    }
    this.input.put(key, inputItem);
    return this;
  }

  /**
   * The input parameters for a task.
   * @return input
  */
  
  @Schema(name = "input", accessMode = Schema.AccessMode.READ_ONLY, description = "The input parameters for a task.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("input")
  public Map<String, Object> getInput() {
    return input;
  }

  public void setInput(Map<String, Object> input) {
    this.input = input;
  }

  public TaskExecutionModel jobId(String jobId) {
    this.jobId = jobId;
    return this;
  }

  /**
   * The id of a job for which a task belongs to.
   * @return jobId
  */
  
  @Schema(name = "jobId", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a job for which a task belongs to.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("jobId")
  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public TaskExecutionModel lastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  /**
   * The last modified by.
   * @return lastModifiedBy
  */
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedBy")
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public TaskExecutionModel lastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
    return this;
  }

  /**
   * The last modified date.
   * @return lastModifiedDate
  */
  @Valid 
  @Schema(name = "lastModifiedDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedDate")
  public LocalDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public TaskExecutionModel maxRetries(Integer maxRetries) {
    this.maxRetries = maxRetries;
    return this;
  }

  /**
   * The maximum number of times that a task may retry.
   * @return maxRetries
  */
  
  @Schema(name = "maxRetries", accessMode = Schema.AccessMode.READ_ONLY, description = "The maximum number of times that a task may retry.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("maxRetries")
  public Integer getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(Integer maxRetries) {
    this.maxRetries = maxRetries;
  }

  public TaskExecutionModel output(Object output) {
    this.output = output;
    return this;
  }

  /**
   * The result output generated by the task handler which executed a task.
   * @return output
  */
  
  @Schema(name = "output", accessMode = Schema.AccessMode.READ_ONLY, description = "The result output generated by the task handler which executed a task.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("output")
  public Object getOutput() {
    return output;
  }

  public void setOutput(Object output) {
    this.output = output;
  }

  public TaskExecutionModel parentId(String parentId) {
    this.parentId = parentId;
    return this;
  }

  /**
   * The id of the parent task, if this is a sub-task.
   * @return parentId
  */
  
  @Schema(name = "parentId", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of the parent task, if this is a sub-task.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("parentId")
  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public TaskExecutionModel priority(Integer priority) {
    this.priority = priority;
    return this;
  }

  /**
   * The priority value.
   * @return priority
  */
  
  @Schema(name = "priority", accessMode = Schema.AccessMode.READ_ONLY, description = "The priority value.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("priority")
  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public TaskExecutionModel progress(Integer progress) {
    this.progress = progress;
    return this;
  }

  /**
   * The current progress value, a number between 0 and 100.
   * @return progress
  */
  
  @Schema(name = "progress", accessMode = Schema.AccessMode.READ_ONLY, description = "The current progress value, a number between 0 and 100.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("progress")
  public Integer getProgress() {
    return progress;
  }

  public void setProgress(Integer progress) {
    this.progress = progress;
  }

  public TaskExecutionModel retryAttempts(Integer retryAttempts) {
    this.retryAttempts = retryAttempts;
    return this;
  }

  /**
   * The number of times that a task has been retried.
   * @return retryAttempts
  */
  
  @Schema(name = "retryAttempts", accessMode = Schema.AccessMode.READ_ONLY, description = "The number of times that a task has been retried.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("retryAttempts")
  public Integer getRetryAttempts() {
    return retryAttempts;
  }

  public void setRetryAttempts(Integer retryAttempts) {
    this.retryAttempts = retryAttempts;
  }

  public TaskExecutionModel retryDelay(String retryDelay) {
    this.retryDelay = retryDelay;
    return this;
  }

  /**
   * The delay to introduce between each retry. Values are to be specified using the ISO-8601 format (excluding the PT prefix). e.g. 10s (ten seconds), 1m (one minute) etc.
   * @return retryDelay
  */
  
  @Schema(name = "retryDelay", accessMode = Schema.AccessMode.READ_ONLY, description = "The delay to introduce between each retry. Values are to be specified using the ISO-8601 format (excluding the PT prefix). e.g. 10s (ten seconds), 1m (one minute) etc.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("retryDelay")
  public String getRetryDelay() {
    return retryDelay;
  }

  public void setRetryDelay(String retryDelay) {
    this.retryDelay = retryDelay;
  }

  public TaskExecutionModel retryDelayFactor(Integer retryDelayFactor) {
    this.retryDelayFactor = retryDelayFactor;
    return this;
  }

  /**
   * The factor to use in order to calculate the actual delay time between each successive retry -- multiplying by the value of the retryDelay.
   * @return retryDelayFactor
  */
  
  @Schema(name = "retryDelayFactor", accessMode = Schema.AccessMode.READ_ONLY, description = "The factor to use in order to calculate the actual delay time between each successive retry -- multiplying by the value of the retryDelay.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("retryDelayFactor")
  public Integer getRetryDelayFactor() {
    return retryDelayFactor;
  }

  public void setRetryDelayFactor(Integer retryDelayFactor) {
    this.retryDelayFactor = retryDelayFactor;
  }

  public TaskExecutionModel startDate(LocalDateTime startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * The time when a task instance was started.
   * @return startDate
  */
  @Valid 
  @Schema(name = "startDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The time when a task instance was started.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("startDate")
  public LocalDateTime getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDateTime startDate) {
    this.startDate = startDate;
  }

  public TaskExecutionModel status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * The current status of a task.
   * @return status
  */
  
  @Schema(name = "status", accessMode = Schema.AccessMode.READ_ONLY, description = "The current status of a task.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }

  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  public TaskExecutionModel taskNumber(Integer taskNumber) {
    this.taskNumber = taskNumber;
    return this;
  }

  /**
   * The numeric order of the task in the workflow.
   * @return taskNumber
  */
  
  @Schema(name = "taskNumber", accessMode = Schema.AccessMode.READ_ONLY, description = "The numeric order of the task in the workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("taskNumber")
  public Integer getTaskNumber() {
    return taskNumber;
  }

  public void setTaskNumber(Integer taskNumber) {
    this.taskNumber = taskNumber;
  }

  public TaskExecutionModel retryDelayMillis(Long retryDelayMillis) {
    this.retryDelayMillis = retryDelayMillis;
    return this;
  }

  /**
   * The calculated retry delay. i.e. delay * retryAttempts * retryDelayFactor.
   * @return retryDelayMillis
  */
  
  @Schema(name = "retryDelayMillis", accessMode = Schema.AccessMode.READ_ONLY, description = "The calculated retry delay. i.e. delay * retryAttempts * retryDelayFactor.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("retryDelayMillis")
  public Long getRetryDelayMillis() {
    return retryDelayMillis;
  }

  public void setRetryDelayMillis(Long retryDelayMillis) {
    this.retryDelayMillis = retryDelayMillis;
  }

  public TaskExecutionModel workflowTask(com.bytechef.platform.configuration.web.rest.model.WorkflowTaskModel workflowTask) {
    this.workflowTask = workflowTask;
    return this;
  }

  /**
   * Get workflowTask
   * @return workflowTask
  */
  @Valid 
  @Schema(name = "workflowTask", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowTask")
  public com.bytechef.platform.configuration.web.rest.model.WorkflowTaskModel getWorkflowTask() {
    return workflowTask;
  }

  public void setWorkflowTask(com.bytechef.platform.configuration.web.rest.model.WorkflowTaskModel workflowTask) {
    this.workflowTask = workflowTask;
  }

  public TaskExecutionModel type(String type) {
    this.type = type;
    return this;
  }

  /**
   * The type of the task.
   * @return type
  */
  
  @Schema(name = "type", accessMode = Schema.AccessMode.READ_ONLY, description = "The type of the task.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TaskExecutionModel taskExecution = (TaskExecutionModel) o;
    return Objects.equals(this.component, taskExecution.component) &&
        Objects.equals(this.createdBy, taskExecution.createdBy) &&
        Objects.equals(this.createdDate, taskExecution.createdDate) &&
        Objects.equals(this.endDate, taskExecution.endDate) &&
        Objects.equals(this.error, taskExecution.error) &&
        Objects.equals(this.executionTime, taskExecution.executionTime) &&
        Objects.equals(this.id, taskExecution.id) &&
        Objects.equals(this.input, taskExecution.input) &&
        Objects.equals(this.jobId, taskExecution.jobId) &&
        Objects.equals(this.lastModifiedBy, taskExecution.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, taskExecution.lastModifiedDate) &&
        Objects.equals(this.maxRetries, taskExecution.maxRetries) &&
        Objects.equals(this.output, taskExecution.output) &&
        Objects.equals(this.parentId, taskExecution.parentId) &&
        Objects.equals(this.priority, taskExecution.priority) &&
        Objects.equals(this.progress, taskExecution.progress) &&
        Objects.equals(this.retryAttempts, taskExecution.retryAttempts) &&
        Objects.equals(this.retryDelay, taskExecution.retryDelay) &&
        Objects.equals(this.retryDelayFactor, taskExecution.retryDelayFactor) &&
        Objects.equals(this.startDate, taskExecution.startDate) &&
        Objects.equals(this.status, taskExecution.status) &&
        Objects.equals(this.taskNumber, taskExecution.taskNumber) &&
        Objects.equals(this.retryDelayMillis, taskExecution.retryDelayMillis) &&
        Objects.equals(this.workflowTask, taskExecution.workflowTask) &&
        Objects.equals(this.type, taskExecution.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(component, createdBy, createdDate, endDate, error, executionTime, id, input, jobId, lastModifiedBy, lastModifiedDate, maxRetries, output, parentId, priority, progress, retryAttempts, retryDelay, retryDelayFactor, startDate, status, taskNumber, retryDelayMillis, workflowTask, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TaskExecutionModel {\n");
    sb.append("    component: ").append(toIndentedString(component)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    executionTime: ").append(toIndentedString(executionTime)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    input: ").append(toIndentedString(input)).append("\n");
    sb.append("    jobId: ").append(toIndentedString(jobId)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    maxRetries: ").append(toIndentedString(maxRetries)).append("\n");
    sb.append("    output: ").append(toIndentedString(output)).append("\n");
    sb.append("    parentId: ").append(toIndentedString(parentId)).append("\n");
    sb.append("    priority: ").append(toIndentedString(priority)).append("\n");
    sb.append("    progress: ").append(toIndentedString(progress)).append("\n");
    sb.append("    retryAttempts: ").append(toIndentedString(retryAttempts)).append("\n");
    sb.append("    retryDelay: ").append(toIndentedString(retryDelay)).append("\n");
    sb.append("    retryDelayFactor: ").append(toIndentedString(retryDelayFactor)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    taskNumber: ").append(toIndentedString(taskNumber)).append("\n");
    sb.append("    retryDelayMillis: ").append(toIndentedString(retryDelayMillis)).append("\n");
    sb.append("    workflowTask: ").append(toIndentedString(workflowTask)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
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

