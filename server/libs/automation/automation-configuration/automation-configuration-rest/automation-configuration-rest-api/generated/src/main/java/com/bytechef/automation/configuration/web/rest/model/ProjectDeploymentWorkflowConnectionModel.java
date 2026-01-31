package com.bytechef.automation.configuration.web.rest.model;

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
 * The connection used in a particular action task or trigger.
 */

@Schema(name = "ProjectDeploymentWorkflowConnection", description = "The connection used in a particular action task or trigger.")
@JsonTypeName("ProjectDeploymentWorkflowConnection")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-28T12:19:12.459673+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class ProjectDeploymentWorkflowConnectionModel {

  private Long connectionId;

  private String workflowConnectionKey;

  private String workflowNodeName;

  public ProjectDeploymentWorkflowConnectionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProjectDeploymentWorkflowConnectionModel(Long connectionId, String workflowConnectionKey, String workflowNodeName) {
    this.connectionId = connectionId;
    this.workflowConnectionKey = workflowConnectionKey;
    this.workflowNodeName = workflowNodeName;
  }

  public ProjectDeploymentWorkflowConnectionModel connectionId(Long connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  /**
   * The connection id
   * @return connectionId
   */
  @NotNull 
  @Schema(name = "connectionId", description = "The connection id", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connectionId")
  public Long getConnectionId() {
    return connectionId;
  }

  public void setConnectionId(Long connectionId) {
    this.connectionId = connectionId;
  }

  public ProjectDeploymentWorkflowConnectionModel workflowConnectionKey(String workflowConnectionKey) {
    this.workflowConnectionKey = workflowConnectionKey;
    return this;
  }

  /**
   * The connection key under which a connection is defined in a workflow definition.
   * @return workflowConnectionKey
   */
  @NotNull 
  @Schema(name = "workflowConnectionKey", description = "The connection key under which a connection is defined in a workflow definition.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workflowConnectionKey")
  public String getWorkflowConnectionKey() {
    return workflowConnectionKey;
  }

  public void setWorkflowConnectionKey(String workflowConnectionKey) {
    this.workflowConnectionKey = workflowConnectionKey;
  }

  public ProjectDeploymentWorkflowConnectionModel workflowNodeName(String workflowNodeName) {
    this.workflowNodeName = workflowNodeName;
    return this;
  }

  /**
   * The action/trigger name to which a connection belongs.
   * @return workflowNodeName
   */
  @NotNull 
  @Schema(name = "workflowNodeName", description = "The action/trigger name to which a connection belongs.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workflowNodeName")
  public String getWorkflowNodeName() {
    return workflowNodeName;
  }

  public void setWorkflowNodeName(String workflowNodeName) {
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
    ProjectDeploymentWorkflowConnectionModel projectDeploymentWorkflowConnection = (ProjectDeploymentWorkflowConnectionModel) o;
    return Objects.equals(this.connectionId, projectDeploymentWorkflowConnection.connectionId) &&
        Objects.equals(this.workflowConnectionKey, projectDeploymentWorkflowConnection.workflowConnectionKey) &&
        Objects.equals(this.workflowNodeName, projectDeploymentWorkflowConnection.workflowNodeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionId, workflowConnectionKey, workflowNodeName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectDeploymentWorkflowConnectionModel {\n");
    sb.append("    connectionId: ").append(toIndentedString(connectionId)).append("\n");
    sb.append("    workflowConnectionKey: ").append(toIndentedString(workflowConnectionKey)).append("\n");
    sb.append("    workflowNodeName: ").append(toIndentedString(workflowNodeName)).append("\n");
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

