package com.bytechef.ee.automation.app.public_.web.rest.management.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A workflow bound to an app under a workflow key.
 */

@Schema(name = "AppWorkflowBinding", description = "A workflow bound to an app under a workflow key.")
@JsonTypeName("AppWorkflowBinding")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:30.252991+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class AppWorkflowBindingModel {

  private @Nullable Long id;

  private @Nullable Long appId;

  private @Nullable String workflowKey;

  private @Nullable Long projectDeploymentId;

  private @Nullable String projectDeploymentName;

  private @Nullable String workflowUuid;

  private @Nullable String workflowName;

  private @Nullable Boolean available;

  public AppWorkflowBindingModel id(@Nullable Long id) {
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

  public AppWorkflowBindingModel appId(@Nullable Long appId) {
    this.appId = appId;
    return this;
  }

  /**
   * Get appId
   * @return appId
   */
  
  @Schema(name = "appId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("appId")
  public @Nullable Long getAppId() {
    return appId;
  }

  @JsonProperty("appId")
  public void setAppId(@Nullable Long appId) {
    this.appId = appId;
  }

  public AppWorkflowBindingModel workflowKey(@Nullable String workflowKey) {
    this.workflowKey = workflowKey;
    return this;
  }

  /**
   * Get workflowKey
   * @return workflowKey
   */
  
  @Schema(name = "workflowKey", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowKey")
  public @Nullable String getWorkflowKey() {
    return workflowKey;
  }

  @JsonProperty("workflowKey")
  public void setWorkflowKey(@Nullable String workflowKey) {
    this.workflowKey = workflowKey;
  }

  public AppWorkflowBindingModel projectDeploymentId(@Nullable Long projectDeploymentId) {
    this.projectDeploymentId = projectDeploymentId;
    return this;
  }

  /**
   * Get projectDeploymentId
   * @return projectDeploymentId
   */
  
  @Schema(name = "projectDeploymentId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectDeploymentId")
  public @Nullable Long getProjectDeploymentId() {
    return projectDeploymentId;
  }

  @JsonProperty("projectDeploymentId")
  public void setProjectDeploymentId(@Nullable Long projectDeploymentId) {
    this.projectDeploymentId = projectDeploymentId;
  }

  public AppWorkflowBindingModel projectDeploymentName(@Nullable String projectDeploymentName) {
    this.projectDeploymentName = projectDeploymentName;
    return this;
  }

  /**
   * Get projectDeploymentName
   * @return projectDeploymentName
   */
  
  @Schema(name = "projectDeploymentName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectDeploymentName")
  public @Nullable String getProjectDeploymentName() {
    return projectDeploymentName;
  }

  @JsonProperty("projectDeploymentName")
  public void setProjectDeploymentName(@Nullable String projectDeploymentName) {
    this.projectDeploymentName = projectDeploymentName;
  }

  public AppWorkflowBindingModel workflowUuid(@Nullable String workflowUuid) {
    this.workflowUuid = workflowUuid;
    return this;
  }

  /**
   * Get workflowUuid
   * @return workflowUuid
   */
  
  @Schema(name = "workflowUuid", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowUuid")
  public @Nullable String getWorkflowUuid() {
    return workflowUuid;
  }

  @JsonProperty("workflowUuid")
  public void setWorkflowUuid(@Nullable String workflowUuid) {
    this.workflowUuid = workflowUuid;
  }

  public AppWorkflowBindingModel workflowName(@Nullable String workflowName) {
    this.workflowName = workflowName;
    return this;
  }

  /**
   * Get workflowName
   * @return workflowName
   */
  
  @Schema(name = "workflowName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowName")
  public @Nullable String getWorkflowName() {
    return workflowName;
  }

  @JsonProperty("workflowName")
  public void setWorkflowName(@Nullable String workflowName) {
    this.workflowName = workflowName;
  }

  public AppWorkflowBindingModel available(@Nullable Boolean available) {
    this.available = available;
    return this;
  }

  /**
   * Get available
   * @return available
   */
  
  @Schema(name = "available", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("available")
  public @Nullable Boolean getAvailable() {
    return available;
  }

  @JsonProperty("available")
  public void setAvailable(@Nullable Boolean available) {
    this.available = available;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppWorkflowBindingModel appWorkflowBinding = (AppWorkflowBindingModel) o;
    return Objects.equals(this.id, appWorkflowBinding.id) &&
        Objects.equals(this.appId, appWorkflowBinding.appId) &&
        Objects.equals(this.workflowKey, appWorkflowBinding.workflowKey) &&
        Objects.equals(this.projectDeploymentId, appWorkflowBinding.projectDeploymentId) &&
        Objects.equals(this.projectDeploymentName, appWorkflowBinding.projectDeploymentName) &&
        Objects.equals(this.workflowUuid, appWorkflowBinding.workflowUuid) &&
        Objects.equals(this.workflowName, appWorkflowBinding.workflowName) &&
        Objects.equals(this.available, appWorkflowBinding.available);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, appId, workflowKey, projectDeploymentId, projectDeploymentName, workflowUuid, workflowName, available);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppWorkflowBindingModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    appId: ").append(toIndentedString(appId)).append("\n");
    sb.append("    workflowKey: ").append(toIndentedString(workflowKey)).append("\n");
    sb.append("    projectDeploymentId: ").append(toIndentedString(projectDeploymentId)).append("\n");
    sb.append("    projectDeploymentName: ").append(toIndentedString(projectDeploymentName)).append("\n");
    sb.append("    workflowUuid: ").append(toIndentedString(workflowUuid)).append("\n");
    sb.append("    workflowName: ").append(toIndentedString(workflowName)).append("\n");
    sb.append("    available: ").append(toIndentedString(available)).append("\n");
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

