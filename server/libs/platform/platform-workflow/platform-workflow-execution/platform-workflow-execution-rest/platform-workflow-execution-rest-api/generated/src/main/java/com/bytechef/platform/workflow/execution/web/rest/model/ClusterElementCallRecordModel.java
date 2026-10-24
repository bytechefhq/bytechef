package com.bytechef.platform.workflow.execution.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
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
 * One call a task made to one of its cluster elements, such as an AI agent&#39;s tool or sub-agent.
 */

@Schema(name = "ClusterElementCallRecord", description = "One call a task made to one of its cluster elements, such as an AI agent's tool or sub-agent.")
@JsonTypeName("ClusterElementCallRecord")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-24T15:45:24.720816+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ClusterElementCallRecordModel {

  private @Nullable String componentName;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime endDate;

  private @Nullable String error;

  private String id;

  private @Nullable String input;

  private @Nullable String output;

  private @Nullable String parentId;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime startDate;

  /**
   * The state of the call.
   */
  public enum StatusEnum {
    STARTED("STARTED"),
    
    SUSPENDED("SUSPENDED"),
    
    COMPLETED("COMPLETED"),
    
    FAILED("FAILED");

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

  private @Nullable String toolName;

  private @Nullable String workflowNodeName;

  public ClusterElementCallRecordModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ClusterElementCallRecordModel(String id, StatusEnum status) {
    this.id = id;
    this.status = status;
  }

  public ClusterElementCallRecordModel componentName(@Nullable String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of the component the called cluster element belongs to.
   * @return componentName
   */
  
  @Schema(name = "componentName", description = "The name of the component the called cluster element belongs to.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentName")
  public @Nullable String getComponentName() {
    return componentName;
  }

  @JsonProperty("componentName")
  public void setComponentName(@Nullable String componentName) {
    this.componentName = componentName;
  }

  public ClusterElementCallRecordModel endDate(@Nullable OffsetDateTime endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * The time the call ended.
   * @return endDate
   */
  @Valid 
  @Schema(name = "endDate", description = "The time the call ended.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("endDate")
  public @Nullable OffsetDateTime getEndDate() {
    return endDate;
  }

  @JsonProperty("endDate")
  public void setEndDate(@Nullable OffsetDateTime endDate) {
    this.endDate = endDate;
  }

  public ClusterElementCallRecordModel error(@Nullable String error) {
    this.error = error;
    return this;
  }

  /**
   * The error message of a failed call.
   * @return error
   */
  
  @Schema(name = "error", description = "The error message of a failed call.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("error")
  public @Nullable String getError() {
    return error;
  }

  @JsonProperty("error")
  public void setError(@Nullable String error) {
    this.error = error;
  }

  public ClusterElementCallRecordModel id(String id) {
    this.id = id;
    return this;
  }

  /**
   * The unique identifier of the call.
   * @return id
   */
  @NotNull 
  @Schema(name = "id", description = "The unique identifier of the call.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  public ClusterElementCallRecordModel input(@Nullable String input) {
    this.input = input;
    return this;
  }

  /**
   * The input of the call, capped in length.
   * @return input
   */
  
  @Schema(name = "input", description = "The input of the call, capped in length.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("input")
  public @Nullable String getInput() {
    return input;
  }

  @JsonProperty("input")
  public void setInput(@Nullable String input) {
    this.input = input;
  }

  public ClusterElementCallRecordModel output(@Nullable String output) {
    this.output = output;
    return this;
  }

  /**
   * The output of the call, capped in length.
   * @return output
   */
  
  @Schema(name = "output", description = "The output of the call, capped in length.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("output")
  public @Nullable String getOutput() {
    return output;
  }

  @JsonProperty("output")
  public void setOutput(@Nullable String output) {
    this.output = output;
  }

  public ClusterElementCallRecordModel parentId(@Nullable String parentId) {
    this.parentId = parentId;
    return this;
  }

  /**
   * The identifier of the call this call is nested under, such as a sub-agent call.
   * @return parentId
   */
  
  @Schema(name = "parentId", description = "The identifier of the call this call is nested under, such as a sub-agent call.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("parentId")
  public @Nullable String getParentId() {
    return parentId;
  }

  @JsonProperty("parentId")
  public void setParentId(@Nullable String parentId) {
    this.parentId = parentId;
  }

  public ClusterElementCallRecordModel startDate(@Nullable OffsetDateTime startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * The time the call started.
   * @return startDate
   */
  @Valid 
  @Schema(name = "startDate", description = "The time the call started.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("startDate")
  public @Nullable OffsetDateTime getStartDate() {
    return startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(@Nullable OffsetDateTime startDate) {
    this.startDate = startDate;
  }

  public ClusterElementCallRecordModel status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * The state of the call.
   * @return status
   */
  @NotNull 
  @Schema(name = "status", description = "The state of the call.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  public ClusterElementCallRecordModel toolName(@Nullable String toolName) {
    this.toolName = toolName;
    return this;
  }

  /**
   * The name of the tool that was called.
   * @return toolName
   */
  
  @Schema(name = "toolName", description = "The name of the tool that was called.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("toolName")
  public @Nullable String getToolName() {
    return toolName;
  }

  @JsonProperty("toolName")
  public void setToolName(@Nullable String toolName) {
    this.toolName = toolName;
  }

  public ClusterElementCallRecordModel workflowNodeName(@Nullable String workflowNodeName) {
    this.workflowNodeName = workflowNodeName;
    return this;
  }

  /**
   * The workflow node name of the cluster element.
   * @return workflowNodeName
   */
  
  @Schema(name = "workflowNodeName", description = "The workflow node name of the cluster element.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowNodeName")
  public @Nullable String getWorkflowNodeName() {
    return workflowNodeName;
  }

  @JsonProperty("workflowNodeName")
  public void setWorkflowNodeName(@Nullable String workflowNodeName) {
    this.workflowNodeName = workflowNodeName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterElementCallRecordModel clusterElementCallRecord = (ClusterElementCallRecordModel) o;
    return Objects.equals(this.componentName, clusterElementCallRecord.componentName) &&
        Objects.equals(this.endDate, clusterElementCallRecord.endDate) &&
        Objects.equals(this.error, clusterElementCallRecord.error) &&
        Objects.equals(this.id, clusterElementCallRecord.id) &&
        Objects.equals(this.input, clusterElementCallRecord.input) &&
        Objects.equals(this.output, clusterElementCallRecord.output) &&
        Objects.equals(this.parentId, clusterElementCallRecord.parentId) &&
        Objects.equals(this.startDate, clusterElementCallRecord.startDate) &&
        Objects.equals(this.status, clusterElementCallRecord.status) &&
        Objects.equals(this.toolName, clusterElementCallRecord.toolName) &&
        Objects.equals(this.workflowNodeName, clusterElementCallRecord.workflowNodeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, endDate, error, id, input, output, parentId, startDate, status, toolName, workflowNodeName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClusterElementCallRecordModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    input: ").append(toIndentedString(input)).append("\n");
    sb.append("    output: ").append(toIndentedString(output)).append("\n");
    sb.append("    parentId: ").append(toIndentedString(parentId)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    toolName: ").append(toIndentedString(toolName)).append("\n");
    sb.append("    workflowNodeName: ").append(toIndentedString(workflowNodeName)).append("\n");
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

