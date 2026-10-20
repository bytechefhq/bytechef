package com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.EndpointConnectionModel;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.HttpMethodModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The values to change on an API collection endpoint. A field left out keeps its stored value.
 */

@Schema(name = "ApiCollectionEndpointUpdate", description = "The values to change on an API collection endpoint. A field left out keeps its stored value.")
@JsonTypeName("ApiCollectionEndpointUpdate")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T01:48:59.391908+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ApiCollectionEndpointUpdateModel {

  private @Nullable String name;

  private @Nullable HttpMethodModel httpMethod;

  private @Nullable String path;

  private @Nullable String workflowUuid;

  private @Nullable List<@Valid EndpointConnectionModel> connections;

  private @Nullable Map<String, Object> inputs;

  public ApiCollectionEndpointUpdateModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  
  @Schema(name = "name", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public ApiCollectionEndpointUpdateModel httpMethod(@Nullable HttpMethodModel httpMethod) {
    this.httpMethod = httpMethod;
    return this;
  }

  /**
   * Get httpMethod
   * @return httpMethod
   */
  @Valid 
  @Schema(name = "httpMethod", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("httpMethod")
  public @Nullable HttpMethodModel getHttpMethod() {
    return httpMethod;
  }

  @JsonProperty("httpMethod")
  public void setHttpMethod(@Nullable HttpMethodModel httpMethod) {
    this.httpMethod = httpMethod;
  }

  public ApiCollectionEndpointUpdateModel path(@Nullable String path) {
    this.path = path;
    return this;
  }

  /**
   * Get path
   * @return path
   */
  
  @Schema(name = "path", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("path")
  public @Nullable String getPath() {
    return path;
  }

  @JsonProperty("path")
  public void setPath(@Nullable String path) {
    this.path = path;
  }

  public ApiCollectionEndpointUpdateModel workflowUuid(@Nullable String workflowUuid) {
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

  public ApiCollectionEndpointUpdateModel connections(@Nullable List<@Valid EndpointConnectionModel> connections) {
    this.connections = connections;
    return this;
  }

  public ApiCollectionEndpointUpdateModel addConnectionsItem(EndpointConnectionModel connectionsItem) {
    if (this.connections == null) {
      this.connections = new ArrayList<>();
    }
    this.connections.add(connectionsItem);
    return this;
  }

  /**
   * Get connections
   * @return connections
   */
  
  @Schema(name = "connections", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connections")
  public @Nullable List<@Valid EndpointConnectionModel> getConnections() {
    return connections;
  }

  @JsonProperty("connections")
  public void setConnections(@Nullable List<@Valid EndpointConnectionModel> connections) {
    this.connections = connections;
  }

  public ApiCollectionEndpointUpdateModel inputs(@Nullable Map<String, Object> inputs) {
    this.inputs = inputs;
    return this;
  }

  public ApiCollectionEndpointUpdateModel putInputsItem(String key, Object inputsItem) {
    if (this.inputs == null) {
      this.inputs = new HashMap<>();
    }
    this.inputs.put(key, inputsItem);
    return this;
  }

  /**
   * Get inputs
   * @return inputs
   */
  
  @Schema(name = "inputs", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
  public @Nullable Map<String, Object> getInputs() {
    return inputs;
  }

  @JsonProperty("inputs")
  public void setInputs(@Nullable Map<String, Object> inputs) {
    this.inputs = inputs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiCollectionEndpointUpdateModel apiCollectionEndpointUpdate = (ApiCollectionEndpointUpdateModel) o;
    return Objects.equals(this.name, apiCollectionEndpointUpdate.name) &&
        Objects.equals(this.httpMethod, apiCollectionEndpointUpdate.httpMethod) &&
        Objects.equals(this.path, apiCollectionEndpointUpdate.path) &&
        Objects.equals(this.workflowUuid, apiCollectionEndpointUpdate.workflowUuid) &&
        Objects.equals(this.connections, apiCollectionEndpointUpdate.connections) &&
        Objects.equals(this.inputs, apiCollectionEndpointUpdate.inputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, httpMethod, path, workflowUuid, connections, inputs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCollectionEndpointUpdateModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    httpMethod: ").append(toIndentedString(httpMethod)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    workflowUuid: ").append(toIndentedString(workflowUuid)).append("\n");
    sb.append("    connections: ").append(toIndentedString(connections)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
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

