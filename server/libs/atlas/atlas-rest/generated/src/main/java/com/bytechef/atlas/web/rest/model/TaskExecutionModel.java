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
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * TaskExecutionModel
 */

@JsonTypeName("TaskExecution")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-27T10:08:31.057495+02:00[Europe/Zagreb]")
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
   * Gets or Sets status
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

  @JsonProperty("workflowTask")
  private WorkflowTaskModel workflowTask;

  @JsonProperty("retryDelayMillis")
  private Long retryDelayMillis;

  @JsonProperty("type")
  private String type;

  public TaskExecutionModel createdBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * Get createdBy
   * @return createdBy
  */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, required = false)
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
   * Get createdDate
   * @return createdDate
  */
  @Valid 
  @Schema(name = "createdDate", accessMode = Schema.AccessMode.READ_ONLY, required = false)
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
   * Get endTime
   * @return endTime
  */
  @Valid 
  @Schema(name = "endTime", required = false)
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
   * Get executionTime
   * @return executionTime
  */
  
  @Schema(name = "executionTime", required = false)
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
   * Get id
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, required = false)
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
   * Get jobId
   * @return jobId
  */
  
  @Schema(name = "jobId", required = false)
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
   * Get lastModifiedBy
   * @return lastModifiedBy
  */
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, required = false)
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
   * Get lastModifiedDate
   * @return lastModifiedDate
  */
  @Valid 
  @Schema(name = "lastModifiedDate", accessMode = Schema.AccessMode.READ_ONLY, required = false)
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
   * Get output
   * @return output
  */
  
  @Schema(name = "output", required = false)
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
   * Get parentId
   * @return parentId
  */
  
  @Schema(name = "parentId", required = false)
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
   * Get priority
   * @return priority
  */
  
  @Schema(name = "priority", required = false)
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
   * Get progress
   * @return progress
  */
  
  @Schema(name = "progress", required = false)
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
   * Get retry
   * @return retry
  */
  
  @Schema(name = "retry", required = false)
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
   * Get retryAttempts
   * @return retryAttempts
  */
  
  @Schema(name = "retryAttempts", required = false)
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
   * Get retryDelay
   * @return retryDelay
  */
  
  @Schema(name = "retryDelay", required = false)
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
   * Get retryDelayFactor
   * @return retryDelayFactor
  */
  
  @Schema(name = "retryDelayFactor", required = false)
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
   * Get startTime
   * @return startTime
  */
  @Valid 
  @Schema(name = "startTime", required = false)
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
   * Get status
   * @return status
  */
  
  @Schema(name = "status", required = false)
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
   * Get taskNumber
   * @return taskNumber
  */
  
  @Schema(name = "taskNumber", required = false)
  public Integer getTaskNumber() {
    return taskNumber;
  }

  public void setTaskNumber(Integer taskNumber) {
    this.taskNumber = taskNumber;
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

  public TaskExecutionModel retryDelayMillis(Long retryDelayMillis) {
    this.retryDelayMillis = retryDelayMillis;
    return this;
  }

  /**
   * Get retryDelayMillis
   * @return retryDelayMillis
  */
  
  @Schema(name = "retryDelayMillis", required = false)
  public Long getRetryDelayMillis() {
    return retryDelayMillis;
  }

  public void setRetryDelayMillis(Long retryDelayMillis) {
    this.retryDelayMillis = retryDelayMillis;
  }

  public TaskExecutionModel type(String type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
  */
  
  @Schema(name = "type", required = false)
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
        Objects.equals(this.workflowTask, taskExecution.workflowTask) &&
        Objects.equals(this.retryDelayMillis, taskExecution.retryDelayMillis) &&
        Objects.equals(this.type, taskExecution.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, endTime, error, executionTime, id, jobId, lastModifiedBy, lastModifiedDate, output, parentId, priority, progress, retry, retryAttempts, retryDelay, retryDelayFactor, startTime, status, taskNumber, workflowTask, retryDelayMillis, type);
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
    sb.append("    workflowTask: ").append(toIndentedString(workflowTask)).append("\n");
    sb.append("    retryDelayMillis: ").append(toIndentedString(retryDelayMillis)).append("\n");
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

