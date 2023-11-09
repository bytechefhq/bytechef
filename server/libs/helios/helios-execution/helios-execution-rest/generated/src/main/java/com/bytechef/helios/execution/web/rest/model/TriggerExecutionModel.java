package com.bytechef.helios.execution.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.helios.execution.web.rest.model.ComponentDefinitionModel;
import com.bytechef.helios.execution.web.rest.model.ExecutionErrorModel;
import com.bytechef.helios.execution.web.rest.model.WorkflowTriggerModel;
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
 * Adds execution semantics to a trigger.
 */

@Schema(name = "TriggerExecution", description = "Adds execution semantics to a trigger.")
@JsonTypeName("TriggerExecution")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-11-10T13:27:34.788965+01:00[Europe/Zagreb]")
public class TriggerExecutionModel {

  private Boolean batch;

  private ComponentDefinitionModel component;

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

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  private Integer maxRetries;

  private Object output;

  private Integer priority;

  private Integer retryAttempts;

  private String retryDelay;

  private Integer retryDelayFactor;

  private Long retryDelayMillis;

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

  private WorkflowTriggerModel workflowTrigger;

  private String type;

  public TriggerExecutionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TriggerExecutionModel(Integer priority, LocalDateTime startDate, StatusEnum status) {
    this.priority = priority;
    this.startDate = startDate;
    this.status = status;
  }

  public TriggerExecutionModel batch(Boolean batch) {
    this.batch = batch;
    return this;
  }

  /**
   * Get batch
   * @return batch
  */
  
  @Schema(name = "batch", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("batch")
  public Boolean getBatch() {
    return batch;
  }

  public void setBatch(Boolean batch) {
    this.batch = batch;
  }

  public TriggerExecutionModel component(ComponentDefinitionModel component) {
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
  public ComponentDefinitionModel getComponent() {
    return component;
  }

  public void setComponent(ComponentDefinitionModel component) {
    this.component = component;
  }

  public TriggerExecutionModel createdBy(String createdBy) {
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

  public TriggerExecutionModel createdDate(LocalDateTime createdDate) {
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

  public TriggerExecutionModel endDate(LocalDateTime endDate) {
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

  public TriggerExecutionModel error(ExecutionErrorModel error) {
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

  public TriggerExecutionModel executionTime(Long executionTime) {
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

  public TriggerExecutionModel id(String id) {
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

  public TriggerExecutionModel input(Map<String, Object> input) {
    this.input = input;
    return this;
  }

  public TriggerExecutionModel putInputItem(String key, Object inputItem) {
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

  public TriggerExecutionModel lastModifiedBy(String lastModifiedBy) {
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

  public TriggerExecutionModel lastModifiedDate(LocalDateTime lastModifiedDate) {
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

  public TriggerExecutionModel maxRetries(Integer maxRetries) {
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

  public TriggerExecutionModel output(Object output) {
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

  public TriggerExecutionModel priority(Integer priority) {
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

  public TriggerExecutionModel retryAttempts(Integer retryAttempts) {
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

  public TriggerExecutionModel retryDelay(String retryDelay) {
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

  public TriggerExecutionModel retryDelayFactor(Integer retryDelayFactor) {
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

  public TriggerExecutionModel retryDelayMillis(Long retryDelayMillis) {
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

  public TriggerExecutionModel startDate(LocalDateTime startDate) {
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

  public TriggerExecutionModel status(StatusEnum status) {
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

  public TriggerExecutionModel workflowTrigger(WorkflowTriggerModel workflowTrigger) {
    this.workflowTrigger = workflowTrigger;
    return this;
  }

  /**
   * Get workflowTrigger
   * @return workflowTrigger
  */
  @Valid 
  @Schema(name = "workflowTrigger", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowTrigger")
  public WorkflowTriggerModel getWorkflowTrigger() {
    return workflowTrigger;
  }

  public void setWorkflowTrigger(WorkflowTriggerModel workflowTrigger) {
    this.workflowTrigger = workflowTrigger;
  }

  public TriggerExecutionModel type(String type) {
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
    TriggerExecutionModel triggerExecution = (TriggerExecutionModel) o;
    return Objects.equals(this.batch, triggerExecution.batch) &&
        Objects.equals(this.component, triggerExecution.component) &&
        Objects.equals(this.createdBy, triggerExecution.createdBy) &&
        Objects.equals(this.createdDate, triggerExecution.createdDate) &&
        Objects.equals(this.endDate, triggerExecution.endDate) &&
        Objects.equals(this.error, triggerExecution.error) &&
        Objects.equals(this.executionTime, triggerExecution.executionTime) &&
        Objects.equals(this.id, triggerExecution.id) &&
        Objects.equals(this.input, triggerExecution.input) &&
        Objects.equals(this.lastModifiedBy, triggerExecution.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, triggerExecution.lastModifiedDate) &&
        Objects.equals(this.maxRetries, triggerExecution.maxRetries) &&
        Objects.equals(this.output, triggerExecution.output) &&
        Objects.equals(this.priority, triggerExecution.priority) &&
        Objects.equals(this.retryAttempts, triggerExecution.retryAttempts) &&
        Objects.equals(this.retryDelay, triggerExecution.retryDelay) &&
        Objects.equals(this.retryDelayFactor, triggerExecution.retryDelayFactor) &&
        Objects.equals(this.retryDelayMillis, triggerExecution.retryDelayMillis) &&
        Objects.equals(this.startDate, triggerExecution.startDate) &&
        Objects.equals(this.status, triggerExecution.status) &&
        Objects.equals(this.workflowTrigger, triggerExecution.workflowTrigger) &&
        Objects.equals(this.type, triggerExecution.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(batch, component, createdBy, createdDate, endDate, error, executionTime, id, input, lastModifiedBy, lastModifiedDate, maxRetries, output, priority, retryAttempts, retryDelay, retryDelayFactor, retryDelayMillis, startDate, status, workflowTrigger, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TriggerExecutionModel {\n");
    sb.append("    batch: ").append(toIndentedString(batch)).append("\n");
    sb.append("    component: ").append(toIndentedString(component)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    executionTime: ").append(toIndentedString(executionTime)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    input: ").append(toIndentedString(input)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    maxRetries: ").append(toIndentedString(maxRetries)).append("\n");
    sb.append("    output: ").append(toIndentedString(output)).append("\n");
    sb.append("    priority: ").append(toIndentedString(priority)).append("\n");
    sb.append("    retryAttempts: ").append(toIndentedString(retryAttempts)).append("\n");
    sb.append("    retryDelay: ").append(toIndentedString(retryDelay)).append("\n");
    sb.append("    retryDelayFactor: ").append(toIndentedString(retryDelayFactor)).append("\n");
    sb.append("    retryDelayMillis: ").append(toIndentedString(retryDelayMillis)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    workflowTrigger: ").append(toIndentedString(workflowTrigger)).append("\n");
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

