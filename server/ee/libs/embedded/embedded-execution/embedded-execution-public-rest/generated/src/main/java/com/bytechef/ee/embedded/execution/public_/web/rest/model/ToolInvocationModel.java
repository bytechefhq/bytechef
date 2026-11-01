package com.bytechef.ee.embedded.execution.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ToolInvocationKindModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ToolInvocationOutcomeModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ToolInvocationSurfaceModel;
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
 * A record of a single tool invocation.
 */

@Schema(name = "ToolInvocation", description = "A record of a single tool invocation.")
@JsonTypeName("ToolInvocation")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-01T09:08:13.694310+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ToolInvocationModel {

  private @Nullable String componentName;

  private @Nullable Integer componentVersion;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private @Nullable Integer durationMs;

  private @Nullable String errorMessage;

  private @Nullable String errorType;

  private @Nullable Long id;

  private @Nullable Long integrationInstanceId;

  private @Nullable Long jobId;

  private @Nullable ToolInvocationKindModel kind;

  private @Nullable String operationName;

  private @Nullable ToolInvocationOutcomeModel outcome;

  private @Nullable ToolInvocationSurfaceModel surface;

  private @Nullable String toolName;

  public ToolInvocationModel componentName(@Nullable String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of the component the tool belongs to.
   * @return componentName
   */
  
  @Schema(name = "componentName", description = "The name of the component the tool belongs to.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentName")
  public @Nullable String getComponentName() {
    return componentName;
  }

  @JsonProperty("componentName")
  public void setComponentName(@Nullable String componentName) {
    this.componentName = componentName;
  }

  public ToolInvocationModel componentVersion(@Nullable Integer componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * The version of the component the tool belongs to.
   * @return componentVersion
   */
  
  @Schema(name = "componentVersion", description = "The version of the component the tool belongs to.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentVersion")
  public @Nullable Integer getComponentVersion() {
    return componentVersion;
  }

  @JsonProperty("componentVersion")
  public void setComponentVersion(@Nullable Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  public ToolInvocationModel createdDate(@Nullable OffsetDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  /**
   * When the invocation was recorded.
   * @return createdDate
   */
  @Valid 
  @Schema(name = "createdDate", description = "When the invocation was recorded.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdDate")
  public @Nullable OffsetDateTime getCreatedDate() {
    return createdDate;
  }

  @JsonProperty("createdDate")
  public void setCreatedDate(@Nullable OffsetDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public ToolInvocationModel durationMs(@Nullable Integer durationMs) {
    this.durationMs = durationMs;
    return this;
  }

  /**
   * How long the invocation took, in milliseconds.
   * @return durationMs
   */
  
  @Schema(name = "durationMs", description = "How long the invocation took, in milliseconds.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("durationMs")
  public @Nullable Integer getDurationMs() {
    return durationMs;
  }

  @JsonProperty("durationMs")
  public void setDurationMs(@Nullable Integer durationMs) {
    this.durationMs = durationMs;
  }

  public ToolInvocationModel errorMessage(@Nullable String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  /**
   * The error message of a failed invocation.
   * @return errorMessage
   */
  
  @Schema(name = "errorMessage", description = "The error message of a failed invocation.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("errorMessage")
  public @Nullable String getErrorMessage() {
    return errorMessage;
  }

  @JsonProperty("errorMessage")
  public void setErrorMessage(@Nullable String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public ToolInvocationModel errorType(@Nullable String errorType) {
    this.errorType = errorType;
    return this;
  }

  /**
   * The error type of a failed invocation.
   * @return errorType
   */
  
  @Schema(name = "errorType", description = "The error type of a failed invocation.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("errorType")
  public @Nullable String getErrorType() {
    return errorType;
  }

  @JsonProperty("errorType")
  public void setErrorType(@Nullable String errorType) {
    this.errorType = errorType;
  }

  public ToolInvocationModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of the invocation record.
   * @return id
   */
  
  @Schema(name = "id", description = "The id of the invocation record.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public ToolInvocationModel integrationInstanceId(@Nullable Long integrationInstanceId) {
    this.integrationInstanceId = integrationInstanceId;
    return this;
  }

  /**
   * The id of the integration instance the invocation ran on.
   * @return integrationInstanceId
   */
  
  @Schema(name = "integrationInstanceId", description = "The id of the integration instance the invocation ran on.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationInstanceId")
  public @Nullable Long getIntegrationInstanceId() {
    return integrationInstanceId;
  }

  @JsonProperty("integrationInstanceId")
  public void setIntegrationInstanceId(@Nullable Long integrationInstanceId) {
    this.integrationInstanceId = integrationInstanceId;
  }

  public ToolInvocationModel jobId(@Nullable Long jobId) {
    this.jobId = jobId;
    return this;
  }

  /**
   * The id of the workflow run a workflow tool started.
   * @return jobId
   */
  
  @Schema(name = "jobId", description = "The id of the workflow run a workflow tool started.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("jobId")
  public @Nullable Long getJobId() {
    return jobId;
  }

  @JsonProperty("jobId")
  public void setJobId(@Nullable Long jobId) {
    this.jobId = jobId;
  }

  public ToolInvocationModel kind(@Nullable ToolInvocationKindModel kind) {
    this.kind = kind;
    return this;
  }

  /**
   * Get kind
   * @return kind
   */
  @Valid 
  @Schema(name = "kind", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("kind")
  public @Nullable ToolInvocationKindModel getKind() {
    return kind;
  }

  @JsonProperty("kind")
  public void setKind(@Nullable ToolInvocationKindModel kind) {
    this.kind = kind;
  }

  public ToolInvocationModel operationName(@Nullable String operationName) {
    this.operationName = operationName;
    return this;
  }

  /**
   * The name of the action or cluster element the tool ran.
   * @return operationName
   */
  
  @Schema(name = "operationName", description = "The name of the action or cluster element the tool ran.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("operationName")
  public @Nullable String getOperationName() {
    return operationName;
  }

  @JsonProperty("operationName")
  public void setOperationName(@Nullable String operationName) {
    this.operationName = operationName;
  }

  public ToolInvocationModel outcome(@Nullable ToolInvocationOutcomeModel outcome) {
    this.outcome = outcome;
    return this;
  }

  /**
   * Get outcome
   * @return outcome
   */
  @Valid 
  @Schema(name = "outcome", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("outcome")
  public @Nullable ToolInvocationOutcomeModel getOutcome() {
    return outcome;
  }

  @JsonProperty("outcome")
  public void setOutcome(@Nullable ToolInvocationOutcomeModel outcome) {
    this.outcome = outcome;
  }

  public ToolInvocationModel surface(@Nullable ToolInvocationSurfaceModel surface) {
    this.surface = surface;
    return this;
  }

  /**
   * Get surface
   * @return surface
   */
  @Valid 
  @Schema(name = "surface", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("surface")
  public @Nullable ToolInvocationSurfaceModel getSurface() {
    return surface;
  }

  @JsonProperty("surface")
  public void setSurface(@Nullable ToolInvocationSurfaceModel surface) {
    this.surface = surface;
  }

  public ToolInvocationModel toolName(@Nullable String toolName) {
    this.toolName = toolName;
    return this;
  }

  /**
   * The name of the tool.
   * @return toolName
   */
  
  @Schema(name = "toolName", description = "The name of the tool.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("toolName")
  public @Nullable String getToolName() {
    return toolName;
  }

  @JsonProperty("toolName")
  public void setToolName(@Nullable String toolName) {
    this.toolName = toolName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ToolInvocationModel toolInvocation = (ToolInvocationModel) o;
    return Objects.equals(this.componentName, toolInvocation.componentName) &&
        Objects.equals(this.componentVersion, toolInvocation.componentVersion) &&
        Objects.equals(this.createdDate, toolInvocation.createdDate) &&
        Objects.equals(this.durationMs, toolInvocation.durationMs) &&
        Objects.equals(this.errorMessage, toolInvocation.errorMessage) &&
        Objects.equals(this.errorType, toolInvocation.errorType) &&
        Objects.equals(this.id, toolInvocation.id) &&
        Objects.equals(this.integrationInstanceId, toolInvocation.integrationInstanceId) &&
        Objects.equals(this.jobId, toolInvocation.jobId) &&
        Objects.equals(this.kind, toolInvocation.kind) &&
        Objects.equals(this.operationName, toolInvocation.operationName) &&
        Objects.equals(this.outcome, toolInvocation.outcome) &&
        Objects.equals(this.surface, toolInvocation.surface) &&
        Objects.equals(this.toolName, toolInvocation.toolName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, componentVersion, createdDate, durationMs, errorMessage, errorType, id, integrationInstanceId, jobId, kind, operationName, outcome, surface, toolName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ToolInvocationModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    durationMs: ").append(toIndentedString(durationMs)).append("\n");
    sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
    sb.append("    errorType: ").append(toIndentedString(errorType)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    integrationInstanceId: ").append(toIndentedString(integrationInstanceId)).append("\n");
    sb.append("    jobId: ").append(toIndentedString(jobId)).append("\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    operationName: ").append(toIndentedString(operationName)).append("\n");
    sb.append("    outcome: ").append(toIndentedString(outcome)).append("\n");
    sb.append("    surface: ").append(toIndentedString(surface)).append("\n");
    sb.append("    toolName: ").append(toIndentedString(toolName)).append("\n");
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

