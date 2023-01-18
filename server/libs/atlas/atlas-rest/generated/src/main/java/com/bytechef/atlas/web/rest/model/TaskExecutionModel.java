package com.bytechef.atlas.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.atlas.web.rest.model.ExecutionErrorModel;
import com.bytechef.atlas.web.rest.model.WorkflowTaskModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Adds execution semantics to the task.
 */

@Schema(name = "TaskExecution", description = "Adds execution semantics to the task.")
@JsonTypeName("TaskExecution")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-01-18T08:53:27.088924+01:00[Europe/Zagreb]")
public class TaskExecutionModel {

  @JsonProperty("createdBy")
  private String createdBy;

  @JsonProperty("createdDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  @JsonProperty("endTime")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime endTime;

  @JsonProperty("error")
  private ExecutionErrorModel error;

  @JsonProperty("executionTime")
  private Long executionTime;

  @JsonProperty("id")
  private String id;

  @JsonProperty("jobId")
  private String jobId;

  @JsonProperty("lastModifiedBy")
  private String lastModifiedBy;

  @JsonProperty("lastModifiedDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  @JsonProperty("output")
  private Object output;

  @JsonProperty("parentId")
  private String parentId;

  @JsonProperty("priority")
  private Integer priority;

  @JsonProperty("progress")
  private Integer progress;

  @JsonProperty("retry")
  private Integer retry;

  @JsonProperty("retryAttempts")
  private Integer retryAttempts;

  @JsonProperty("retryDelay")
  private String retryDelay;

  @JsonProperty("retryDelayFactor")
  private Integer retryDelayFactor;

  @JsonProperty("startTime")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime startTime;

  /**
   * The current status of this task.
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

  @JsonProperty("status")
  private StatusEnum status;

  @JsonProperty("taskNumber")
  private Integer taskNumber;

  @JsonProperty("retryDelayMillis")
  private Long retryDelayMillis;

  @JsonProperty("workflowTask")
  private WorkflowTaskModel workflowTask;

  @JsonProperty("type")
  private String type;

  public TaskExecutionModel createdBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * The created by.
   * @return createdBy
  */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The created by.", required = false)
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
  @Schema(name = "createdDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The created date.", required = false)
  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public TaskExecutionModel endTime(LocalDateTime endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * The time when this task instance ended (CANCELLED, FAILED, COMPLETED).
   * @return endTime
  */
  @Valid 
  @Schema(name = "endTime", accessMode = Schema.AccessMode.READ_ONLY, description = "The time when this task instance ended (CANCELLED, FAILED, COMPLETED).", required = false)
  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
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
  @Schema(name = "error", required = false)
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
   * The total time in ms for this task to execute (excluding wait time of the task in transit). i.e. actual execution time on a worker node.
   * @return executionTime
  */
  
  @Schema(name = "executionTime", accessMode = Schema.AccessMode.READ_ONLY, description = "The total time in ms for this task to execute (excluding wait time of the task in transit). i.e. actual execution time on a worker node.", required = false)
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
   * Id of the task execution.
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "Id of the task execution.", required = false)
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public TaskExecutionModel jobId(String jobId) {
    this.jobId = jobId;
    return this;
  }

  /**
   * The id of the job for which this task belongs to.
   * @return jobId
  */
  
  @Schema(name = "jobId", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of the job for which this task belongs to.", required = true)
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
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified by.", required = false)
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
  @Schema(name = "lastModifiedDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified date.", required = false)
  public LocalDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public TaskExecutionModel output(Object output) {
    this.output = output;
    return this;
  }

  /**
   * The result output generated by the task handler which executed this task.
   * @return output
  */
  
  @Schema(name = "output", accessMode = Schema.AccessMode.READ_ONLY, description = "The result output generated by the task handler which executed this task.", required = false)
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
  
  @Schema(name = "parentId", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of the parent task, if this is a sub-task.", required = false)
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
  
  @Schema(name = "priority", accessMode = Schema.AccessMode.READ_ONLY, description = "The priority value.", required = true)
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
  
  @Schema(name = "progress", accessMode = Schema.AccessMode.READ_ONLY, description = "The current progress value, a number between 0 and 100.", required = false)
  public Integer getProgress() {
    return progress;
  }

  public void setProgress(Integer progress) {
    this.progress = progress;
  }

  public TaskExecutionModel retry(Integer retry) {
    this.retry = retry;
    return this;
  }

  /**
   * The maximum number of times that this task may retry.
   * @return retry
  */
  
  @Schema(name = "retry", accessMode = Schema.AccessMode.READ_ONLY, description = "The maximum number of times that this task may retry.", required = false)
  public Integer getRetry() {
    return retry;
  }

  public void setRetry(Integer retry) {
    this.retry = retry;
  }

  public TaskExecutionModel retryAttempts(Integer retryAttempts) {
    this.retryAttempts = retryAttempts;
    return this;
  }

  /**
   * The number of times that this task has been retried.
   * @return retryAttempts
  */
  
  @Schema(name = "retryAttempts", accessMode = Schema.AccessMode.READ_ONLY, description = "The number of times that this task has been retried.", required = false)
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
  
  @Schema(name = "retryDelay", accessMode = Schema.AccessMode.READ_ONLY, description = "The delay to introduce between each retry. Values are to be specified using the ISO-8601 format (excluding the PT prefix). e.g. 10s (ten seconds), 1m (one minute) etc.", required = false)
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
  
  @Schema(name = "retryDelayFactor", accessMode = Schema.AccessMode.READ_ONLY, description = "The factor to use in order to calculate the actual delay time between each successive retry -- multiplying by the value of the retryDelay.", required = false)
  public Integer getRetryDelayFactor() {
    return retryDelayFactor;
  }

  public void setRetryDelayFactor(Integer retryDelayFactor) {
    this.retryDelayFactor = retryDelayFactor;
  }

  public TaskExecutionModel startTime(LocalDateTime startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * The time when this task instance was started.
   * @return startTime
  */
  @Valid 
  @Schema(name = "startTime", accessMode = Schema.AccessMode.READ_ONLY, description = "The time when this task instance was started.", required = true)
  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public TaskExecutionModel status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * The current status of this task.
   * @return status
  */
  
  @Schema(name = "status", accessMode = Schema.AccessMode.READ_ONLY, description = "The current status of this task.", required = true)
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
  
  @Schema(name = "taskNumber", accessMode = Schema.AccessMode.READ_ONLY, description = "The numeric order of the task in the workflow.", required = false)
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
  
  @Schema(name = "retryDelayMillis", accessMode = Schema.AccessMode.READ_ONLY, description = "The calculated retry delay. i.e. delay * retryAttempts * retryDelayFactor.", required = false)
  public Long getRetryDelayMillis() {
    return retryDelayMillis;
  }

  public void setRetryDelayMillis(Long retryDelayMillis) {
    this.retryDelayMillis = retryDelayMillis;
  }

  public TaskExecutionModel workflowTask(WorkflowTaskModel workflowTask) {
    this.workflowTask = workflowTask;
    return this;
  }

  /**
   * Get workflowTask
   * @return workflowTask
  */
  @Valid 
  @Schema(name = "workflowTask", required = false)
  public WorkflowTaskModel getWorkflowTask() {
    return workflowTask;
  }

  public void setWorkflowTask(WorkflowTaskModel workflowTask) {
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
  
  @Schema(name = "type", accessMode = Schema.AccessMode.READ_ONLY, description = "The type of the task.", required = false)
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
    return Objects.equals(this.createdBy, taskExecution.createdBy) &&
        Objects.equals(this.createdDate, taskExecution.createdDate) &&
        Objects.equals(this.endTime, taskExecution.endTime) &&
        Objects.equals(this.error, taskExecution.error) &&
        Objects.equals(this.executionTime, taskExecution.executionTime) &&
        Objects.equals(this.id, taskExecution.id) &&
        Objects.equals(this.jobId, taskExecution.jobId) &&
        Objects.equals(this.lastModifiedBy, taskExecution.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, taskExecution.lastModifiedDate) &&
        Objects.equals(this.output, taskExecution.output) &&
        Objects.equals(this.parentId, taskExecution.parentId) &&
        Objects.equals(this.priority, taskExecution.priority) &&
        Objects.equals(this.progress, taskExecution.progress) &&
        Objects.equals(this.retry, taskExecution.retry) &&
        Objects.equals(this.retryAttempts, taskExecution.retryAttempts) &&
        Objects.equals(this.retryDelay, taskExecution.retryDelay) &&
        Objects.equals(this.retryDelayFactor, taskExecution.retryDelayFactor) &&
        Objects.equals(this.startTime, taskExecution.startTime) &&
        Objects.equals(this.status, taskExecution.status) &&
        Objects.equals(this.taskNumber, taskExecution.taskNumber) &&
        Objects.equals(this.retryDelayMillis, taskExecution.retryDelayMillis) &&
        Objects.equals(this.workflowTask, taskExecution.workflowTask) &&
        Objects.equals(this.type, taskExecution.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, endTime, error, executionTime, id, jobId, lastModifiedBy, lastModifiedDate, output, parentId, priority, progress, retry, retryAttempts, retryDelay, retryDelayFactor, startTime, status, taskNumber, retryDelayMillis, workflowTask, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TaskExecutionModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    executionTime: ").append(toIndentedString(executionTime)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    jobId: ").append(toIndentedString(jobId)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    output: ").append(toIndentedString(output)).append("\n");
    sb.append("    parentId: ").append(toIndentedString(parentId)).append("\n");
    sb.append("    priority: ").append(toIndentedString(priority)).append("\n");
    sb.append("    progress: ").append(toIndentedString(progress)).append("\n");
    sb.append("    retry: ").append(toIndentedString(retry)).append("\n");
    sb.append("    retryAttempts: ").append(toIndentedString(retryAttempts)).append("\n");
    sb.append("    retryDelay: ").append(toIndentedString(retryDelay)).append("\n");
    sb.append("    retryDelayFactor: ").append(toIndentedString(retryDelayFactor)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
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

