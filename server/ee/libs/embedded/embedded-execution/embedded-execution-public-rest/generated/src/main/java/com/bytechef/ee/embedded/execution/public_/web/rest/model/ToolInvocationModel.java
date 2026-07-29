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
 * A record of a single direct tool/action invocation.
 */

@Schema(name = "ToolInvocation", description = "A record of a single direct tool/action invocation.")
@JsonTypeName("ToolInvocation")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-19T07:23:07.589358527Z[Etc/UTC]", comments = "Generator version: 7.22.0")
public class ToolInvocationModel {

  private @Nullable Long id;

  private @Nullable ToolInvocationSurfaceModel surface;

  private @Nullable ToolInvocationKindModel kind;

  private @Nullable String toolName;

  private @Nullable String componentName;

  private @Nullable Integer componentVersion;

  private @Nullable String operationName;

  private @Nullable Long integrationInstanceId;

  private @Nullable Long jobId;

  private @Nullable ToolInvocationOutcomeModel outcome;

  private @Nullable String errorMessage;

  private @Nullable Integer durationMs;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  public ToolInvocationModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  
  @Schema(name = "id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable Long id) {
    this.id = id;
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

  public ToolInvocationModel toolName(@Nullable String toolName) {
    this.toolName = toolName;
    return this;
  }

  /**
   * Get toolName
   * @return toolName
   */
  
  @Schema(name = "toolName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("toolName")
  public @Nullable String getToolName() {
    return toolName;
  }

  @JsonProperty("toolName")
  public void setToolName(@Nullable String toolName) {
    this.toolName = toolName;
  }

  public ToolInvocationModel componentName(@Nullable String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * Get componentName
   * @return componentName
   */
  
  @Schema(name = "componentName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
   * Get componentVersion
   * @return componentVersion
   */
  
  @Schema(name = "componentVersion", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentVersion")
  public @Nullable Integer getComponentVersion() {
    return componentVersion;
  }

  @JsonProperty("componentVersion")
  public void setComponentVersion(@Nullable Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  public ToolInvocationModel operationName(@Nullable String operationName) {
    this.operationName = operationName;
    return this;
  }

  /**
   * Get operationName
   * @return operationName
   */
  
  @Schema(name = "operationName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("operationName")
  public @Nullable String getOperationName() {
    return operationName;
  }

  @JsonProperty("operationName")
  public void setOperationName(@Nullable String operationName) {
    this.operationName = operationName;
  }

  public ToolInvocationModel integrationInstanceId(@Nullable Long integrationInstanceId) {
    this.integrationInstanceId = integrationInstanceId;
    return this;
  }

  /**
   * Get integrationInstanceId
   * @return integrationInstanceId
   */
  
  @Schema(name = "integrationInstanceId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
   * Get jobId
   * @return jobId
   */
  
  @Schema(name = "jobId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("jobId")
  public @Nullable Long getJobId() {
    return jobId;
  }

  @JsonProperty("jobId")
  public void setJobId(@Nullable Long jobId) {
    this.jobId = jobId;
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

  public ToolInvocationModel errorMessage(@Nullable String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  /**
   * Get errorMessage
   * @return errorMessage
   */
  
  @Schema(name = "errorMessage", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("errorMessage")
  public @Nullable String getErrorMessage() {
    return errorMessage;
  }

  @JsonProperty("errorMessage")
  public void setErrorMessage(@Nullable String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public ToolInvocationModel durationMs(@Nullable Integer durationMs) {
    this.durationMs = durationMs;
    return this;
  }

  /**
   * Get durationMs
   * @return durationMs
   */
  
  @Schema(name = "durationMs", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("durationMs")
  public @Nullable Integer getDurationMs() {
    return durationMs;
  }

  @JsonProperty("durationMs")
  public void setDurationMs(@Nullable Integer durationMs) {
    this.durationMs = durationMs;
  }

  public ToolInvocationModel createdDate(@Nullable OffsetDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  /**
   * Get createdDate
   * @return createdDate
   */
  @Valid 
  @Schema(name = "createdDate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdDate")
  public @Nullable OffsetDateTime getCreatedDate() {
    return createdDate;
  }

  @JsonProperty("createdDate")
  public void setCreatedDate(@Nullable OffsetDateTime createdDate) {
    this.createdDate = createdDate;
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
    return Objects.equals(this.id, toolInvocation.id) &&
        Objects.equals(this.surface, toolInvocation.surface) &&
        Objects.equals(this.kind, toolInvocation.kind) &&
        Objects.equals(this.toolName, toolInvocation.toolName) &&
        Objects.equals(this.componentName, toolInvocation.componentName) &&
        Objects.equals(this.componentVersion, toolInvocation.componentVersion) &&
        Objects.equals(this.operationName, toolInvocation.operationName) &&
        Objects.equals(this.integrationInstanceId, toolInvocation.integrationInstanceId) &&
        Objects.equals(this.jobId, toolInvocation.jobId) &&
        Objects.equals(this.outcome, toolInvocation.outcome) &&
        Objects.equals(this.errorMessage, toolInvocation.errorMessage) &&
        Objects.equals(this.durationMs, toolInvocation.durationMs) &&
        Objects.equals(this.createdDate, toolInvocation.createdDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, surface, kind, toolName, componentName, componentVersion, operationName, integrationInstanceId, jobId, outcome, errorMessage, durationMs, createdDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ToolInvocationModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    surface: ").append(toIndentedString(surface)).append("\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    toolName: ").append(toIndentedString(toolName)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    operationName: ").append(toIndentedString(operationName)).append("\n");
    sb.append("    integrationInstanceId: ").append(toIndentedString(integrationInstanceId)).append("\n");
    sb.append("    jobId: ").append(toIndentedString(jobId)).append("\n");
    sb.append("    outcome: ").append(toIndentedString(outcome)).append("\n");
    sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
    sb.append("    durationMs: ").append(toIndentedString(durationMs)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
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

