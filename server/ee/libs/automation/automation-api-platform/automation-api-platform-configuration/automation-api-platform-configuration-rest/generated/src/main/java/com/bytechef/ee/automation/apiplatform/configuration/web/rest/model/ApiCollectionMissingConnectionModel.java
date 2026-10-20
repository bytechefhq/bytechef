package com.bytechef.ee.automation.apiplatform.configuration.web.rest.model;

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
 * A connection slot of the new project version that no carried connection binds.
 */

@Schema(name = "ApiCollectionMissingConnection", description = "A connection slot of the new project version that no carried connection binds.")
@JsonTypeName("ApiCollectionMissingConnection")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T01:38:17.244806+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ApiCollectionMissingConnectionModel {

  private @Nullable String componentName;

  private @Nullable String endpointName;

  private @Nullable String key;

  private @Nullable String workflowNodeName;

  public ApiCollectionMissingConnectionModel componentName(@Nullable String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of the component the slot needs a connection for.
   * @return componentName
   */
  
  @Schema(name = "componentName", description = "The name of the component the slot needs a connection for.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentName")
  public @Nullable String getComponentName() {
    return componentName;
  }

  @JsonProperty("componentName")
  public void setComponentName(@Nullable String componentName) {
    this.componentName = componentName;
  }

  public ApiCollectionMissingConnectionModel endpointName(@Nullable String endpointName) {
    this.endpointName = endpointName;
    return this;
  }

  /**
   * The name of the endpoint whose workflow has the slot.
   * @return endpointName
   */
  
  @Schema(name = "endpointName", description = "The name of the endpoint whose workflow has the slot.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("endpointName")
  public @Nullable String getEndpointName() {
    return endpointName;
  }

  @JsonProperty("endpointName")
  public void setEndpointName(@Nullable String endpointName) {
    this.endpointName = endpointName;
  }

  public ApiCollectionMissingConnectionModel key(@Nullable String key) {
    this.key = key;
    return this;
  }

  /**
   * The connection key of the slot.
   * @return key
   */
  
  @Schema(name = "key", description = "The connection key of the slot.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("key")
  public @Nullable String getKey() {
    return key;
  }

  @JsonProperty("key")
  public void setKey(@Nullable String key) {
    this.key = key;
  }

  public ApiCollectionMissingConnectionModel workflowNodeName(@Nullable String workflowNodeName) {
    this.workflowNodeName = workflowNodeName;
    return this;
  }

  /**
   * The workflow node name of the slot.
   * @return workflowNodeName
   */
  
  @Schema(name = "workflowNodeName", description = "The workflow node name of the slot.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowNodeName")
  public @Nullable String getWorkflowNodeName() {
    return workflowNodeName;
  }

  @JsonProperty("workflowNodeName")
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
    ApiCollectionMissingConnectionModel apiCollectionMissingConnection = (ApiCollectionMissingConnectionModel) o;
    return Objects.equals(this.componentName, apiCollectionMissingConnection.componentName) &&
        Objects.equals(this.endpointName, apiCollectionMissingConnection.endpointName) &&
        Objects.equals(this.key, apiCollectionMissingConnection.key) &&
        Objects.equals(this.workflowNodeName, apiCollectionMissingConnection.workflowNodeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, endpointName, key, workflowNodeName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCollectionMissingConnectionModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    endpointName: ").append(toIndentedString(endpointName)).append("\n");
    sb.append("    key: ").append(toIndentedString(key)).append("\n");
    sb.append("    workflowNodeName: ").append(toIndentedString(workflowNodeName)).append("\n");
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

