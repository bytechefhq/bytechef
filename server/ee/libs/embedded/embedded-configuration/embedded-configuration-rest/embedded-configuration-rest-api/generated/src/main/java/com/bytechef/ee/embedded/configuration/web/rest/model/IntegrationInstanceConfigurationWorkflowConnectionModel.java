package com.bytechef.ee.embedded.configuration.web.rest.model;

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
 * The connection used in a particular task or trigger.
 */

@Schema(name = "IntegrationInstanceConfigurationWorkflowConnection", description = "The connection used in a particular task or trigger.")
@JsonTypeName("IntegrationInstanceConfigurationWorkflowConnection")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-07T14:49:49.053188+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public class IntegrationInstanceConfigurationWorkflowConnectionModel {

  private @Nullable Long connectionId;

  private @Nullable String workflowConnectionKey;

  private @Nullable String workflowNodeName;

  public IntegrationInstanceConfigurationWorkflowConnectionModel connectionId(@Nullable Long connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  /**
   * The connection id
   * @return connectionId
   */
  
  @Schema(name = "connectionId", description = "The connection id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionId")
  public @Nullable Long getConnectionId() {
    return connectionId;
  }

  public void setConnectionId(@Nullable Long connectionId) {
    this.connectionId = connectionId;
  }

  public IntegrationInstanceConfigurationWorkflowConnectionModel workflowConnectionKey(@Nullable String workflowConnectionKey) {
    this.workflowConnectionKey = workflowConnectionKey;
    return this;
  }

  /**
   * The connection key under which a connection is defined in a workflow definition.
   * @return workflowConnectionKey
   */
  
  @Schema(name = "workflowConnectionKey", description = "The connection key under which a connection is defined in a workflow definition.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowConnectionKey")
  public @Nullable String getWorkflowConnectionKey() {
    return workflowConnectionKey;
  }

  public void setWorkflowConnectionKey(@Nullable String workflowConnectionKey) {
    this.workflowConnectionKey = workflowConnectionKey;
  }

  public IntegrationInstanceConfigurationWorkflowConnectionModel workflowNodeName(@Nullable String workflowNodeName) {
    this.workflowNodeName = workflowNodeName;
    return this;
  }

  /**
   * The action/trigger name to which a connection belongs.
   * @return workflowNodeName
   */
  
  @Schema(name = "workflowNodeName", description = "The action/trigger name to which a connection belongs.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowNodeName")
  public @Nullable String getWorkflowNodeName() {
    return workflowNodeName;
  }

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
    IntegrationInstanceConfigurationWorkflowConnectionModel integrationInstanceConfigurationWorkflowConnection = (IntegrationInstanceConfigurationWorkflowConnectionModel) o;
    return Objects.equals(this.connectionId, integrationInstanceConfigurationWorkflowConnection.connectionId) &&
        Objects.equals(this.workflowConnectionKey, integrationInstanceConfigurationWorkflowConnection.workflowConnectionKey) &&
        Objects.equals(this.workflowNodeName, integrationInstanceConfigurationWorkflowConnection.workflowNodeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionId, workflowConnectionKey, workflowNodeName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationInstanceConfigurationWorkflowConnectionModel {\n");
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

