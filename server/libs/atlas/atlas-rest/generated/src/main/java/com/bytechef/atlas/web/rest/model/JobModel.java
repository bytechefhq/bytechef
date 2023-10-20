package com.bytechef.atlas.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.atlas.web.rest.model.ExecutionErrorModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * Represents an execution of a workflow.
 */

@Schema(name = "Job", description = "Represents an execution of a workflow.")
@JsonTypeName("Job")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-01-10T15:10:10.566850+01:00[Europe/Zagreb]")
public class JobModel {

  @JsonProperty("createdBy")
  private String createdBy;

  @JsonProperty("createdDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  @JsonProperty("currentTask")
  private Integer currentTask;

  @JsonProperty("endTime")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime endTime;

  @JsonProperty("error")
  private ExecutionErrorModel error;

  @JsonProperty("id")
  private String id;

  @JsonProperty("inputs")
  @Valid
  private Map<String, Object> inputs = null;

  @JsonProperty("label")
  private String label;

  @JsonProperty("lastModifiedBy")
  private String lastModifiedBy;

  @JsonProperty("lastModifiedDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  @JsonProperty("outputs")
  @Valid
  private Map<String, Object> outputs = null;

  @JsonProperty("parentTaskExecutionId")
  private Long parentTaskExecutionId;

  @JsonProperty("priority")
  private Integer priority;

  @JsonProperty("startTime")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime startTime;

  /**
   * The job's status.
   */
  public enum StatusEnum {
    CREATED("CREATED"),
    
    STARTED("STARTED"),
    
    STOPPED("STOPPED"),
    
    FAILED("FAILED"),
    
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

  @JsonProperty("webhooks")
  @Valid
  private List<Map<String, Object>> webhooks = null;

  @JsonProperty("workflowId")
  private String workflowId;

  public JobModel createdBy(String createdBy) {
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

  public JobModel createdDate(LocalDateTime createdDate) {
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

  public JobModel currentTask(Integer currentTask) {
    this.currentTask = currentTask;
    return this;
  }

  /**
   * The index of the step on the job's workflow on which the job is working on right now.
   * @return currentTask
  */
  
  @Schema(name = "currentTask", accessMode = Schema.AccessMode.READ_ONLY, description = "The index of the step on the job's workflow on which the job is working on right now.", required = false)
  public Integer getCurrentTask() {
    return currentTask;
  }

  public void setCurrentTask(Integer currentTask) {
    this.currentTask = currentTask;
  }

  public JobModel endTime(LocalDateTime endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * The time execution entered end status COMPLETED, STOPPED, FAILED
   * @return endTime
  */
  @Valid 
  @Schema(name = "endTime", description = "The time execution entered end status COMPLETED, STOPPED, FAILED", required = false)
  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public JobModel error(ExecutionErrorModel error) {
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

  public JobModel id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Id of the job.
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "Id of the job.", required = false)
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public JobModel inputs(Map<String, Object> inputs) {
    this.inputs = inputs;
    return this;
  }

  public JobModel putInputsItem(String key, Object inputsItem) {
    if (this.inputs == null) {
      this.inputs = new HashMap<>();
    }
    this.inputs.put(key, inputsItem);
    return this;
  }

  /**
   * The key-value map of the inputs passed to the job when it was created.
   * @return inputs
  */
  
  @Schema(name = "inputs", accessMode = Schema.AccessMode.READ_ONLY, description = "The key-value map of the inputs passed to the job when it was created.", required = false)
  public Map<String, Object> getInputs() {
    return inputs;
  }

  public void setInputs(Map<String, Object> inputs) {
    this.inputs = inputs;
  }

  public JobModel label(String label) {
    this.label = label;
    return this;
  }

  /**
   * The job's human-readable name.
   * @return label
  */
  
  @Schema(name = "label", accessMode = Schema.AccessMode.READ_ONLY, description = "The job's human-readable name.", required = false)
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public JobModel lastModifiedBy(String lastModifiedBy) {
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

  public JobModel lastModifiedDate(LocalDateTime lastModifiedDate) {
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

  public JobModel outputs(Map<String, Object> outputs) {
    this.outputs = outputs;
    return this;
  }

  public JobModel putOutputsItem(String key, Object outputsItem) {
    if (this.outputs == null) {
      this.outputs = new HashMap<>();
    }
    this.outputs.put(key, outputsItem);
    return this;
  }

  /**
   * The key-value map of the outputs returned.
   * @return outputs
  */
  
  @Schema(name = "outputs", accessMode = Schema.AccessMode.READ_ONLY, description = "The key-value map of the outputs returned.", required = false)
  public Map<String, Object> getOutputs() {
    return outputs;
  }

  public void setOutputs(Map<String, Object> outputs) {
    this.outputs = outputs;
  }

  public JobModel parentTaskExecutionId(Long parentTaskExecutionId) {
    this.parentTaskExecutionId = parentTaskExecutionId;
    return this;
  }

  /**
   * The id of the parent task that created this job. Required for sub-flows.
   * @return parentTaskExecutionId
  */
  
  @Schema(name = "parentTaskExecutionId", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of the parent task that created this job. Required for sub-flows.", required = false)
  public Long getParentTaskExecutionId() {
    return parentTaskExecutionId;
  }

  public void setParentTaskExecutionId(Long parentTaskExecutionId) {
    this.parentTaskExecutionId = parentTaskExecutionId;
  }

  public JobModel priority(Integer priority) {
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

  public JobModel startTime(LocalDateTime startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * The time of when the job began.
   * @return startTime
  */
  @Valid 
  @Schema(name = "startTime", accessMode = Schema.AccessMode.READ_ONLY, description = "The time of when the job began.", required = true)
  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public JobModel status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * The job's status.
   * @return status
  */
  
  @Schema(name = "status", accessMode = Schema.AccessMode.READ_ONLY, description = "The job's status.", required = true)
  public StatusEnum getStatus() {
    return status;
  }

  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  public JobModel webhooks(List<Map<String, Object>> webhooks) {
    this.webhooks = webhooks;
    return this;
  }

  public JobModel addWebhooksItem(Map<String, Object> webhooksItem) {
    if (this.webhooks == null) {
      this.webhooks = new ArrayList<>();
    }
    this.webhooks.add(webhooksItem);
    return this;
  }

  /**
   * The list of the webhooks configured.
   * @return webhooks
  */
  @Valid 
  @Schema(name = "webhooks", accessMode = Schema.AccessMode.READ_ONLY, description = "The list of the webhooks configured.", required = false)
  public List<Map<String, Object>> getWebhooks() {
    return webhooks;
  }

  public void setWebhooks(List<Map<String, Object>> webhooks) {
    this.webhooks = webhooks;
  }

  public JobModel workflowId(String workflowId) {
    this.workflowId = workflowId;
    return this;
  }

  /**
   * Get workflowId
   * @return workflowId
  */
  
  @Schema(name = "workflowId", accessMode = Schema.AccessMode.READ_ONLY, required = false)
  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JobModel job = (JobModel) o;
    return Objects.equals(this.createdBy, job.createdBy) &&
        Objects.equals(this.createdDate, job.createdDate) &&
        Objects.equals(this.currentTask, job.currentTask) &&
        Objects.equals(this.endTime, job.endTime) &&
        Objects.equals(this.error, job.error) &&
        Objects.equals(this.id, job.id) &&
        Objects.equals(this.inputs, job.inputs) &&
        Objects.equals(this.label, job.label) &&
        Objects.equals(this.lastModifiedBy, job.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, job.lastModifiedDate) &&
        Objects.equals(this.outputs, job.outputs) &&
        Objects.equals(this.parentTaskExecutionId, job.parentTaskExecutionId) &&
        Objects.equals(this.priority, job.priority) &&
        Objects.equals(this.startTime, job.startTime) &&
        Objects.equals(this.status, job.status) &&
        Objects.equals(this.webhooks, job.webhooks) &&
        Objects.equals(this.workflowId, job.workflowId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, currentTask, endTime, error, id, inputs, label, lastModifiedBy, lastModifiedDate, outputs, parentTaskExecutionId, priority, startTime, status, webhooks, workflowId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JobModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    currentTask: ").append(toIndentedString(currentTask)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    outputs: ").append(toIndentedString(outputs)).append("\n");
    sb.append("    parentTaskExecutionId: ").append(toIndentedString(parentTaskExecutionId)).append("\n");
    sb.append("    priority: ").append(toIndentedString(priority)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    webhooks: ").append(toIndentedString(webhooks)).append("\n");
    sb.append("    workflowId: ").append(toIndentedString(workflowId)).append("\n");
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

