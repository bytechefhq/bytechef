package com.bytechef.platform.workflow.execution.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.workflow.execution.web.rest.model.ExecutionErrorModel;
import com.bytechef.platform.workflow.execution.web.rest.model.TaskExecutionModel;
import com.bytechef.platform.workflow.execution.web.rest.model.WebhookModel;
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
 * Represents an execution of a workflow.
 */

@Schema(name = "Job", description = "Represents an execution of a workflow.")
@JsonTypeName("Job")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-02T08:44:57.102176+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class JobModel {

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private @Nullable Integer currentTask;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime endDate;

  private @Nullable ExecutionErrorModel error;

  private @Nullable String id;

  @Valid
  private Map<String, Object> inputs = new HashMap<>();

  private @Nullable String label;

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  @Valid
  private Map<String, Object> metadata = new HashMap<>();

  @Valid
  private Map<String, Object> outputs = new HashMap<>();

  private @Nullable Long parentTaskExecutionId;

  private Integer priority;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime startDate;

  /**
   * The job's status.
   */
  public enum StatusEnum {
    CREATED("CREATED"),
    
    STARTED("STARTED"),
    
    STOPPED("STOPPED"),
    
    FAILED("FAILED"),
    
    COMPLETED("COMPLETED");

    private final String value;

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

  @Valid
  private List<@Valid TaskExecutionModel> taskExecutions = new ArrayList<>();

  @Valid
  private List<@Valid WebhookModel> webhooks = new ArrayList<>();

  private @Nullable String workflowId;

  public JobModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public JobModel(Integer priority, OffsetDateTime startDate, StatusEnum status) {
    this.priority = priority;
    this.startDate = startDate;
    this.status = status;
  }

  public JobModel createdBy(@Nullable String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * The created by.
   * @return createdBy
   */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The created by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdBy")
  public @Nullable String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(@Nullable String createdBy) {
    this.createdBy = createdBy;
  }

  public JobModel createdDate(@Nullable OffsetDateTime createdDate) {
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
  public @Nullable OffsetDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(@Nullable OffsetDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public JobModel currentTask(@Nullable Integer currentTask) {
    this.currentTask = currentTask;
    return this;
  }

  /**
   * The index of the step on the job's workflow on which the job is working on right now.
   * @return currentTask
   */
  
  @Schema(name = "currentTask", accessMode = Schema.AccessMode.READ_ONLY, description = "The index of the step on the job's workflow on which the job is working on right now.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("currentTask")
  public @Nullable Integer getCurrentTask() {
    return currentTask;
  }

  public void setCurrentTask(@Nullable Integer currentTask) {
    this.currentTask = currentTask;
  }

  public JobModel endDate(@Nullable OffsetDateTime endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * The time execution entered end status COMPLETED, STOPPED, FAILED
   * @return endDate
   */
  @Valid 
  @Schema(name = "endDate", description = "The time execution entered end status COMPLETED, STOPPED, FAILED", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("endDate")
  public @Nullable OffsetDateTime getEndDate() {
    return endDate;
  }

  public void setEndDate(@Nullable OffsetDateTime endDate) {
    this.endDate = endDate;
  }

  public JobModel error(@Nullable ExecutionErrorModel error) {
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

  public void setError(@Nullable ExecutionErrorModel error) {
    this.error = error;
  }

  public JobModel id(@Nullable String id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a job.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a job.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable String getId() {
    return id;
  }

  public void setId(@Nullable String id) {
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
  
  @Schema(name = "inputs", accessMode = Schema.AccessMode.READ_ONLY, description = "The key-value map of the inputs passed to the job when it was created.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
  public Map<String, Object> getInputs() {
    return inputs;
  }

  public void setInputs(Map<String, Object> inputs) {
    this.inputs = inputs;
  }

  public JobModel label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * The job's human-readable name.
   * @return label
   */
  
  @Schema(name = "label", accessMode = Schema.AccessMode.READ_ONLY, description = "The job's human-readable name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  public void setLabel(@Nullable String label) {
    this.label = label;
  }

  public JobModel lastModifiedBy(@Nullable String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  /**
   * The last modified by.
   * @return lastModifiedBy
   */
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedBy")
  public @Nullable String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(@Nullable String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public JobModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
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
  public @Nullable OffsetDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public JobModel metadata(Map<String, Object> metadata) {
    this.metadata = metadata;
    return this;
  }

  public JobModel putMetadataItem(String key, Object metadataItem) {
    if (this.metadata == null) {
      this.metadata = new HashMap<>();
    }
    this.metadata.put(key, metadataItem);
    return this;
  }

  /**
   * Key-value map of metadata.
   * @return metadata
   */
  
  @Schema(name = "metadata", accessMode = Schema.AccessMode.READ_ONLY, description = "Key-value map of metadata.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("metadata")
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
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
  
  @Schema(name = "outputs", accessMode = Schema.AccessMode.READ_ONLY, description = "The key-value map of the outputs returned.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("outputs")
  public Map<String, Object> getOutputs() {
    return outputs;
  }

  public void setOutputs(Map<String, Object> outputs) {
    this.outputs = outputs;
  }

  public JobModel parentTaskExecutionId(@Nullable Long parentTaskExecutionId) {
    this.parentTaskExecutionId = parentTaskExecutionId;
    return this;
  }

  /**
   * The id of the parent task that created this job. Required for sub-flows.
   * @return parentTaskExecutionId
   */
  
  @Schema(name = "parentTaskExecutionId", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of the parent task that created this job. Required for sub-flows.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("parentTaskExecutionId")
  public @Nullable Long getParentTaskExecutionId() {
    return parentTaskExecutionId;
  }

  public void setParentTaskExecutionId(@Nullable Long parentTaskExecutionId) {
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
  
  @Schema(name = "priority", accessMode = Schema.AccessMode.READ_ONLY, description = "The priority value.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("priority")
  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public JobModel startDate(OffsetDateTime startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * The time of when the job began.
   * @return startDate
   */
  @Valid 
  @Schema(name = "startDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The time of when the job began.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("startDate")
  public OffsetDateTime getStartDate() {
    return startDate;
  }

  public void setStartDate(OffsetDateTime startDate) {
    this.startDate = startDate;
  }

  public JobModel status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * The job's status.
   * @return status
   */
  
  @Schema(name = "status", accessMode = Schema.AccessMode.READ_ONLY, description = "The job's status.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }

  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  public JobModel taskExecutions(List<@Valid TaskExecutionModel> taskExecutions) {
    this.taskExecutions = taskExecutions;
    return this;
  }

  public JobModel addTaskExecutionsItem(TaskExecutionModel taskExecutionsItem) {
    if (this.taskExecutions == null) {
      this.taskExecutions = new ArrayList<>();
    }
    this.taskExecutions.add(taskExecutionsItem);
    return this;
  }

  /**
   * Get taskExecutions
   * @return taskExecutions
   */
  @Valid 
  @Schema(name = "taskExecutions", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("taskExecutions")
  public List<@Valid TaskExecutionModel> getTaskExecutions() {
    return taskExecutions;
  }

  public void setTaskExecutions(List<@Valid TaskExecutionModel> taskExecutions) {
    this.taskExecutions = taskExecutions;
  }

  public JobModel webhooks(List<@Valid WebhookModel> webhooks) {
    this.webhooks = webhooks;
    return this;
  }

  public JobModel addWebhooksItem(WebhookModel webhooksItem) {
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
  @Schema(name = "webhooks", accessMode = Schema.AccessMode.READ_ONLY, description = "The list of the webhooks configured.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("webhooks")
  public List<@Valid WebhookModel> getWebhooks() {
    return webhooks;
  }

  public void setWebhooks(List<@Valid WebhookModel> webhooks) {
    this.webhooks = webhooks;
  }

  public JobModel workflowId(@Nullable String workflowId) {
    this.workflowId = workflowId;
    return this;
  }

  /**
   * Get workflowId
   * @return workflowId
   */
  
  @Schema(name = "workflowId", accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowId")
  public @Nullable String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(@Nullable String workflowId) {
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
        Objects.equals(this.endDate, job.endDate) &&
        Objects.equals(this.error, job.error) &&
        Objects.equals(this.id, job.id) &&
        Objects.equals(this.inputs, job.inputs) &&
        Objects.equals(this.label, job.label) &&
        Objects.equals(this.lastModifiedBy, job.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, job.lastModifiedDate) &&
        Objects.equals(this.metadata, job.metadata) &&
        Objects.equals(this.outputs, job.outputs) &&
        Objects.equals(this.parentTaskExecutionId, job.parentTaskExecutionId) &&
        Objects.equals(this.priority, job.priority) &&
        Objects.equals(this.startDate, job.startDate) &&
        Objects.equals(this.status, job.status) &&
        Objects.equals(this.taskExecutions, job.taskExecutions) &&
        Objects.equals(this.webhooks, job.webhooks) &&
        Objects.equals(this.workflowId, job.workflowId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, currentTask, endDate, error, id, inputs, label, lastModifiedBy, lastModifiedDate, metadata, outputs, parentTaskExecutionId, priority, startDate, status, taskExecutions, webhooks, workflowId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JobModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    currentTask: ").append(toIndentedString(currentTask)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
    sb.append("    outputs: ").append(toIndentedString(outputs)).append("\n");
    sb.append("    parentTaskExecutionId: ").append(toIndentedString(parentTaskExecutionId)).append("\n");
    sb.append("    priority: ").append(toIndentedString(priority)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    taskExecutions: ").append(toIndentedString(taskExecutions)).append("\n");
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

