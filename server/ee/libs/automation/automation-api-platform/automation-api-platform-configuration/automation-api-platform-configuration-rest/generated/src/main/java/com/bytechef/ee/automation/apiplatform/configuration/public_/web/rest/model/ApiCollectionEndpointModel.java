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
 * An endpoint of an API collection.
 */

@Schema(name = "ApiCollectionEndpoint", description = "An endpoint of an API collection.")
@JsonTypeName("ApiCollectionEndpoint")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T01:48:59.391908+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ApiCollectionEndpointModel {

  private @Nullable Long id;

  private @Nullable Long apiCollectionId;

  private @Nullable String name;

  private @Nullable HttpMethodModel httpMethod;

  private @Nullable String path;

  private @Nullable String workflowUuid;

  private @Nullable Boolean enabled;

  private @Nullable List<@Valid EndpointConnectionModel> connections;

  private @Nullable Map<String, Object> inputs;

  public ApiCollectionEndpointModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  
  @Schema(name = "id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public ApiCollectionEndpointModel apiCollectionId(@Nullable Long apiCollectionId) {
    this.apiCollectionId = apiCollectionId;
    return this;
  }

  /**
   * Get apiCollectionId
   * @return apiCollectionId
   */
  
  @Schema(name = "apiCollectionId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("apiCollectionId")
  public @Nullable Long getApiCollectionId() {
    return apiCollectionId;
  }

  @JsonProperty("apiCollectionId")
  public void setApiCollectionId(@Nullable Long apiCollectionId) {
    this.apiCollectionId = apiCollectionId;
  }

  public ApiCollectionEndpointModel name(@Nullable String name) {
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

  public ApiCollectionEndpointModel httpMethod(@Nullable HttpMethodModel httpMethod) {
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

  public ApiCollectionEndpointModel path(@Nullable String path) {
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

  public ApiCollectionEndpointModel workflowUuid(@Nullable String workflowUuid) {
    this.workflowUuid = workflowUuid;
    return this;
  }

  /**
   * The uuid of the project workflow the endpoint serves.
   * @return workflowUuid
   */
  
  @Schema(name = "workflowUuid", description = "The uuid of the project workflow the endpoint serves.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowUuid")
  public @Nullable String getWorkflowUuid() {
    return workflowUuid;
  }

  @JsonProperty("workflowUuid")
  public void setWorkflowUuid(@Nullable String workflowUuid) {
    this.workflowUuid = workflowUuid;
  }

  public ApiCollectionEndpointModel enabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Get enabled
   * @return enabled
   */
  
  @Schema(name = "enabled", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public @Nullable Boolean getEnabled() {
    return enabled;
  }

  @JsonProperty("enabled")
  public void setEnabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
  }

  public ApiCollectionEndpointModel connections(@Nullable List<@Valid EndpointConnectionModel> connections) {
    this.connections = connections;
    return this;
  }

  public ApiCollectionEndpointModel addConnectionsItem(EndpointConnectionModel connectionsItem) {
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

  public ApiCollectionEndpointModel inputs(@Nullable Map<String, Object> inputs) {
    this.inputs = inputs;
    return this;
  }

  public ApiCollectionEndpointModel putInputsItem(String key, Object inputsItem) {
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
    ApiCollectionEndpointModel apiCollectionEndpoint = (ApiCollectionEndpointModel) o;
    return Objects.equals(this.id, apiCollectionEndpoint.id) &&
        Objects.equals(this.apiCollectionId, apiCollectionEndpoint.apiCollectionId) &&
        Objects.equals(this.name, apiCollectionEndpoint.name) &&
        Objects.equals(this.httpMethod, apiCollectionEndpoint.httpMethod) &&
        Objects.equals(this.path, apiCollectionEndpoint.path) &&
        Objects.equals(this.workflowUuid, apiCollectionEndpoint.workflowUuid) &&
        Objects.equals(this.enabled, apiCollectionEndpoint.enabled) &&
        Objects.equals(this.connections, apiCollectionEndpoint.connections) &&
        Objects.equals(this.inputs, apiCollectionEndpoint.inputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, apiCollectionId, name, httpMethod, path, workflowUuid, enabled, connections, inputs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCollectionEndpointModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    apiCollectionId: ").append(toIndentedString(apiCollectionId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    httpMethod: ").append(toIndentedString(httpMethod)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    workflowUuid: ").append(toIndentedString(workflowUuid)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
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

