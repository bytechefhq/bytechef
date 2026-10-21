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
 * A deployed app-callable workflow that can be bound to an app.
 */

@Schema(name = "AppBindableWorkflow", description = "A deployed app-callable workflow that can be bound to an app.")
@JsonTypeName("AppBindableWorkflow")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:30.252991+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class AppBindableWorkflowModel {

  private @Nullable Long projectDeploymentId;

  private @Nullable String projectDeploymentName;

  private @Nullable String projectName;

  private @Nullable String workflowUuid;

  private @Nullable String workflowName;

  private @Nullable String description;

  public AppBindableWorkflowModel projectDeploymentId(@Nullable Long projectDeploymentId) {
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

  public AppBindableWorkflowModel projectDeploymentName(@Nullable String projectDeploymentName) {
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

  public AppBindableWorkflowModel projectName(@Nullable String projectName) {
    this.projectName = projectName;
    return this;
  }

  /**
   * Get projectName
   * @return projectName
   */
  
  @Schema(name = "projectName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectName")
  public @Nullable String getProjectName() {
    return projectName;
  }

  @JsonProperty("projectName")
  public void setProjectName(@Nullable String projectName) {
    this.projectName = projectName;
  }

  public AppBindableWorkflowModel workflowUuid(@Nullable String workflowUuid) {
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

  public AppBindableWorkflowModel workflowName(@Nullable String workflowName) {
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

  public AppBindableWorkflowModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
   */
  
  @Schema(name = "description", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppBindableWorkflowModel appBindableWorkflow = (AppBindableWorkflowModel) o;
    return Objects.equals(this.projectDeploymentId, appBindableWorkflow.projectDeploymentId) &&
        Objects.equals(this.projectDeploymentName, appBindableWorkflow.projectDeploymentName) &&
        Objects.equals(this.projectName, appBindableWorkflow.projectName) &&
        Objects.equals(this.workflowUuid, appBindableWorkflow.workflowUuid) &&
        Objects.equals(this.workflowName, appBindableWorkflow.workflowName) &&
        Objects.equals(this.description, appBindableWorkflow.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectDeploymentId, projectDeploymentName, projectName, workflowUuid, workflowName, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppBindableWorkflowModel {\n");
    sb.append("    projectDeploymentId: ").append(toIndentedString(projectDeploymentId)).append("\n");
    sb.append("    projectDeploymentName: ").append(toIndentedString(projectDeploymentName)).append("\n");
    sb.append("    projectName: ").append(toIndentedString(projectName)).append("\n");
    sb.append("    workflowUuid: ").append(toIndentedString(workflowUuid)).append("\n");
    sb.append("    workflowName: ").append(toIndentedString(workflowName)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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

