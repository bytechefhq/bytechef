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
 * The values to create an API collection endpoint from.
 */

@Schema(name = "ApiCollectionEndpointInput", description = "The values to create an API collection endpoint from.")
@JsonTypeName("ApiCollectionEndpointInput")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T01:48:59.391908+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ApiCollectionEndpointInputModel {

  private String name;

  private HttpMethodModel httpMethod;

  private String path;

  private String workflowUuid;

  private @Nullable List<@Valid EndpointConnectionModel> connections;

  private @Nullable Map<String, Object> inputs;

  public ApiCollectionEndpointInputModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ApiCollectionEndpointInputModel(String name, HttpMethodModel httpMethod, String path, String workflowUuid) {
    this.name = name;
    this.httpMethod = httpMethod;
    this.path = path;
    this.workflowUuid = workflowUuid;
  }

  public ApiCollectionEndpointInputModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public ApiCollectionEndpointInputModel httpMethod(HttpMethodModel httpMethod) {
    this.httpMethod = httpMethod;
    return this;
  }

  /**
   * Get httpMethod
   * @return httpMethod
   */
  @NotNull @Valid 
  @Schema(name = "httpMethod", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("httpMethod")
  public HttpMethodModel getHttpMethod() {
    return httpMethod;
  }

  @JsonProperty("httpMethod")
  public void setHttpMethod(HttpMethodModel httpMethod) {
    this.httpMethod = httpMethod;
  }

  public ApiCollectionEndpointInputModel path(String path) {
    this.path = path;
    return this;
  }

  /**
   * Get path
   * @return path
   */
  @NotNull 
  @Schema(name = "path", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("path")
  public String getPath() {
    return path;
  }

  @JsonProperty("path")
  public void setPath(String path) {
    this.path = path;
  }

  public ApiCollectionEndpointInputModel workflowUuid(String workflowUuid) {
    this.workflowUuid = workflowUuid;
    return this;
  }

  /**
   * The uuid of the project workflow the endpoint serves.
   * @return workflowUuid
   */
  @NotNull 
  @Schema(name = "workflowUuid", description = "The uuid of the project workflow the endpoint serves.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workflowUuid")
  public String getWorkflowUuid() {
    return workflowUuid;
  }

  @JsonProperty("workflowUuid")
  public void setWorkflowUuid(String workflowUuid) {
    this.workflowUuid = workflowUuid;
  }

  public ApiCollectionEndpointInputModel connections(@Nullable List<@Valid EndpointConnectionModel> connections) {
    this.connections = connections;
    return this;
  }

  public ApiCollectionEndpointInputModel addConnectionsItem(EndpointConnectionModel connectionsItem) {
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

  public ApiCollectionEndpointInputModel inputs(@Nullable Map<String, Object> inputs) {
    this.inputs = inputs;
    return this;
  }

  public ApiCollectionEndpointInputModel putInputsItem(String key, Object inputsItem) {
    if (this.inputs == null) {
      this.inputs = new HashMap<>();
    }
    this.inputs.put(key, inputsItem);
    return this;
  }

  /**
   * The values of the workflow's inputs.
   * @return inputs
   */
  
  @Schema(name = "inputs", description = "The values of the workflow's inputs.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    ApiCollectionEndpointInputModel apiCollectionEndpointInput = (ApiCollectionEndpointInputModel) o;
    return Objects.equals(this.name, apiCollectionEndpointInput.name) &&
        Objects.equals(this.httpMethod, apiCollectionEndpointInput.httpMethod) &&
        Objects.equals(this.path, apiCollectionEndpointInput.path) &&
        Objects.equals(this.workflowUuid, apiCollectionEndpointInput.workflowUuid) &&
        Objects.equals(this.connections, apiCollectionEndpointInput.connections) &&
        Objects.equals(this.inputs, apiCollectionEndpointInput.inputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, httpMethod, path, workflowUuid, connections, inputs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCollectionEndpointInputModel {\n");
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

