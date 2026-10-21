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
 * The deployed workflow to bind.
 */

@Schema(name = "AppWorkflowBindingInput", description = "The deployed workflow to bind.")
@JsonTypeName("AppWorkflowBindingInput")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:30.252991+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class AppWorkflowBindingInputModel {

  private Long projectDeploymentId;

  private String workflowUuid;

  public AppWorkflowBindingInputModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AppWorkflowBindingInputModel(Long projectDeploymentId, String workflowUuid) {
    this.projectDeploymentId = projectDeploymentId;
    this.workflowUuid = workflowUuid;
  }

  public AppWorkflowBindingInputModel projectDeploymentId(Long projectDeploymentId) {
    this.projectDeploymentId = projectDeploymentId;
    return this;
  }

  /**
   * Get projectDeploymentId
   * @return projectDeploymentId
   */
  @NotNull 
  @Schema(name = "projectDeploymentId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("projectDeploymentId")
  public Long getProjectDeploymentId() {
    return projectDeploymentId;
  }

  @JsonProperty("projectDeploymentId")
  public void setProjectDeploymentId(Long projectDeploymentId) {
    this.projectDeploymentId = projectDeploymentId;
  }

  public AppWorkflowBindingInputModel workflowUuid(String workflowUuid) {
    this.workflowUuid = workflowUuid;
    return this;
  }

  /**
   * Get workflowUuid
   * @return workflowUuid
   */
  @NotNull 
  @Schema(name = "workflowUuid", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workflowUuid")
  public String getWorkflowUuid() {
    return workflowUuid;
  }

  @JsonProperty("workflowUuid")
  public void setWorkflowUuid(String workflowUuid) {
    this.workflowUuid = workflowUuid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppWorkflowBindingInputModel appWorkflowBindingInput = (AppWorkflowBindingInputModel) o;
    return Objects.equals(this.projectDeploymentId, appWorkflowBindingInput.projectDeploymentId) &&
        Objects.equals(this.workflowUuid, appWorkflowBindingInput.workflowUuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectDeploymentId, workflowUuid);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppWorkflowBindingInputModel {\n");
    sb.append("    projectDeploymentId: ").append(toIndentedString(projectDeploymentId)).append("\n");
    sb.append("    workflowUuid: ").append(toIndentedString(workflowUuid)).append("\n");
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

