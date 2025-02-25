package com.bytechef.automation.configuration.web.rest.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import jakarta.annotation.Generated;

/**
 * The connection used in a particular action task or trigger.
 */

@Schema(name = "ProjectDeploymentWorkflowConnection", description = "The connection used in a particular action task or trigger.")
@JsonTypeName("ProjectDeploymentWorkflowConnection")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-01-10T05:57:54.056231+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public class ProjectIDeploymentWorkflowConnectionModel {

  private Long connectionId;

  private String key;

  private String workflowNodeName;

  public ProjectIDeploymentWorkflowConnectionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProjectIDeploymentWorkflowConnectionModel(Long connectionId, String key, String workflowNodeName) {
    this.connectionId = connectionId;
    this.key = key;
    this.workflowNodeName = workflowNodeName;
  }

  public ProjectIDeploymentWorkflowConnectionModel connectionId(Long connectionId) {
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

  public ProjectIDeploymentWorkflowConnectionModel key(String key) {
    this.key = key;
    return this;
  }

  /**
   * The connection key under which a connection is defined in a workflow definition.
   * @return key
   */
  @NotNull
  @Schema(name = "key", description = "The connection key under which a connection is defined in a workflow definition.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("key")
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public ProjectIDeploymentWorkflowConnectionModel workflowNodeName(String workflowNodeName) {
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
    ProjectIDeploymentWorkflowConnectionModel projectDeploymentWorkflowConnection = (ProjectIDeploymentWorkflowConnectionModel) o;
    return Objects.equals(this.connectionId, projectDeploymentWorkflowConnection.connectionId) &&
        Objects.equals(this.key, projectDeploymentWorkflowConnection.key) &&
        Objects.equals(this.workflowNodeName, projectDeploymentWorkflowConnection.workflowNodeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionId, key, workflowNodeName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectDeploymentWorkflowConnectionModel {\n");
    sb.append("    connectionId: ").append(toIndentedString(connectionId)).append("\n");
    sb.append("    key: ").append(toIndentedString(key)).append("\n");
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

