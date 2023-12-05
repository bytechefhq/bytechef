package com.bytechef.helios.configuration.web.rest.model;

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
 * The connection used in a particular task.
 */

@Schema(name = "ProjectInstanceWorkflowConnection", description = "The connection used in a particular task.")
@JsonTypeName("ProjectInstanceWorkflowConnection")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-12-04T15:12:04.933898+01:00[Europe/Zagreb]")
public class ProjectInstanceWorkflowConnectionModel {

  private Long connectionId;

  private String key;

  private String operationName;

  public ProjectInstanceWorkflowConnectionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProjectInstanceWorkflowConnectionModel(Long connectionId, String key, String operationName) {
    this.connectionId = connectionId;
    this.key = key;
    this.operationName = operationName;
  }

  public ProjectInstanceWorkflowConnectionModel connectionId(Long connectionId) {
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

  public ProjectInstanceWorkflowConnectionModel key(String key) {
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

  public ProjectInstanceWorkflowConnectionModel operationName(String operationName) {
    this.operationName = operationName;
    return this;
  }

  /**
   * The action/trigger name to which a connection belongs.
   * @return operationName
  */
  @NotNull 
  @Schema(name = "operationName", description = "The action/trigger name to which a connection belongs.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("operationName")
  public String getOperationName() {
    return operationName;
  }

  public void setOperationName(String operationName) {
    this.operationName = operationName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectInstanceWorkflowConnectionModel projectInstanceWorkflowConnection = (ProjectInstanceWorkflowConnectionModel) o;
    return Objects.equals(this.connectionId, projectInstanceWorkflowConnection.connectionId) &&
        Objects.equals(this.key, projectInstanceWorkflowConnection.key) &&
        Objects.equals(this.operationName, projectInstanceWorkflowConnection.operationName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionId, key, operationName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectInstanceWorkflowConnectionModel {\n");
    sb.append("    connectionId: ").append(toIndentedString(connectionId)).append("\n");
    sb.append("    key: ").append(toIndentedString(key)).append("\n");
    sb.append("    operationName: ").append(toIndentedString(operationName)).append("\n");
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

