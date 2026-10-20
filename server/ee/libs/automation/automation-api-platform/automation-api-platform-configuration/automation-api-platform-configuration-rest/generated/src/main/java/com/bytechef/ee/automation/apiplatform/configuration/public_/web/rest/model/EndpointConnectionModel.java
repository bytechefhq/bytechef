package com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model;

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
 * A connection bound to a connection slot of the endpoint&#39;s workflow.
 */

@Schema(name = "EndpointConnection", description = "A connection bound to a connection slot of the endpoint's workflow.")
@JsonTypeName("EndpointConnection")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T01:48:59.391908+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class EndpointConnectionModel {

  private String workflowNodeName;

  private String key;

  private Long connectionId;

  public EndpointConnectionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EndpointConnectionModel(String workflowNodeName, String key, Long connectionId) {
    this.workflowNodeName = workflowNodeName;
    this.key = key;
    this.connectionId = connectionId;
  }

  public EndpointConnectionModel workflowNodeName(String workflowNodeName) {
    this.workflowNodeName = workflowNodeName;
    return this;
  }

  /**
   * The workflow node the slot belongs to.
   * @return workflowNodeName
   */
  @NotNull 
  @Schema(name = "workflowNodeName", description = "The workflow node the slot belongs to.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workflowNodeName")
  public String getWorkflowNodeName() {
    return workflowNodeName;
  }

  @JsonProperty("workflowNodeName")
  public void setWorkflowNodeName(String workflowNodeName) {
    this.workflowNodeName = workflowNodeName;
  }

  public EndpointConnectionModel key(String key) {
    this.key = key;
    return this;
  }

  /**
   * The key of the slot in the node.
   * @return key
   */
  @NotNull 
  @Schema(name = "key", description = "The key of the slot in the node.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("key")
  public String getKey() {
    return key;
  }

  @JsonProperty("key")
  public void setKey(String key) {
    this.key = key;
  }

  public EndpointConnectionModel connectionId(Long connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  /**
   * The id of the connection bound to the slot.
   * @return connectionId
   */
  @NotNull 
  @Schema(name = "connectionId", description = "The id of the connection bound to the slot.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connectionId")
  public Long getConnectionId() {
    return connectionId;
  }

  @JsonProperty("connectionId")
  public void setConnectionId(Long connectionId) {
    this.connectionId = connectionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EndpointConnectionModel endpointConnection = (EndpointConnectionModel) o;
    return Objects.equals(this.workflowNodeName, endpointConnection.workflowNodeName) &&
        Objects.equals(this.key, endpointConnection.key) &&
        Objects.equals(this.connectionId, endpointConnection.connectionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workflowNodeName, key, connectionId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EndpointConnectionModel {\n");
    sb.append("    workflowNodeName: ").append(toIndentedString(workflowNodeName)).append("\n");
    sb.append("    key: ").append(toIndentedString(key)).append("\n");
    sb.append("    connectionId: ").append(toIndentedString(connectionId)).append("\n");
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

