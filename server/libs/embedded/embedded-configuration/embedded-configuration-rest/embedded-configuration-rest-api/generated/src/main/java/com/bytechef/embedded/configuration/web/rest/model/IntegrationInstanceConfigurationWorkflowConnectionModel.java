package com.bytechef.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-30T07:20:53.361037+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public class IntegrationInstanceConfigurationWorkflowConnectionModel {

  private Long connectionId;

  private String key;

  private String workflowNodeName;

  public IntegrationInstanceConfigurationWorkflowConnectionModel connectionId(Long connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  /**
   * The connection id
   * @return connectionId
  */
  
  @Schema(name = "connectionId", description = "The connection id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionId")
  public Long getConnectionId() {
    return connectionId;
  }

  public void setConnectionId(Long connectionId) {
    this.connectionId = connectionId;
  }

  public IntegrationInstanceConfigurationWorkflowConnectionModel key(String key) {
    this.key = key;
    return this;
  }

  /**
   * The connection key under which a connection is defined in a workflow definition.
   * @return key
  */
  
  @Schema(name = "key", description = "The connection key under which a connection is defined in a workflow definition.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("key")
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public IntegrationInstanceConfigurationWorkflowConnectionModel workflowNodeName(String workflowNodeName) {
    this.workflowNodeName = workflowNodeName;
    return this;
  }

  /**
   * The action/trigger name to which a connection belongs.
   * @return workflowNodeName
  */
  
  @Schema(name = "workflowNodeName", description = "The action/trigger name to which a connection belongs.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    IntegrationInstanceConfigurationWorkflowConnectionModel integrationInstanceConfigurationWorkflowConnection = (IntegrationInstanceConfigurationWorkflowConnectionModel) o;
    return Objects.equals(this.connectionId, integrationInstanceConfigurationWorkflowConnection.connectionId) &&
        Objects.equals(this.key, integrationInstanceConfigurationWorkflowConnection.key) &&
        Objects.equals(this.workflowNodeName, integrationInstanceConfigurationWorkflowConnection.workflowNodeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionId, key, workflowNodeName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationInstanceConfigurationWorkflowConnectionModel {\n");
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

