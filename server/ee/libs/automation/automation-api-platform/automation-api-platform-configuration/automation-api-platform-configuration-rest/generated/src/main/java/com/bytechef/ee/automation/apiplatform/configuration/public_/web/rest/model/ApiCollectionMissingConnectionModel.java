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
 * A required connection of a workflow that no carried connection binds.
 */

@Schema(name = "ApiCollectionMissingConnection", description = "A required connection of a workflow that no carried connection binds.")
@JsonTypeName("ApiCollectionMissingConnection")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T01:43:17.216712+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ApiCollectionMissingConnectionModel {

  private @Nullable String endpointName;

  private @Nullable String workflowNodeName;

  private @Nullable String key;

  private @Nullable String componentName;

  public ApiCollectionMissingConnectionModel endpointName(@Nullable String endpointName) {
    this.endpointName = endpointName;
    return this;
  }

  /**
   * Get endpointName
   * @return endpointName
   */
  
  @Schema(name = "endpointName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("endpointName")
  public @Nullable String getEndpointName() {
    return endpointName;
  }

  @JsonProperty("endpointName")
  public void setEndpointName(@Nullable String endpointName) {
    this.endpointName = endpointName;
  }

  public ApiCollectionMissingConnectionModel workflowNodeName(@Nullable String workflowNodeName) {
    this.workflowNodeName = workflowNodeName;
    return this;
  }

  /**
   * Get workflowNodeName
   * @return workflowNodeName
   */
  
  @Schema(name = "workflowNodeName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowNodeName")
  public @Nullable String getWorkflowNodeName() {
    return workflowNodeName;
  }

  @JsonProperty("workflowNodeName")
  public void setWorkflowNodeName(@Nullable String workflowNodeName) {
    this.workflowNodeName = workflowNodeName;
  }

  public ApiCollectionMissingConnectionModel key(@Nullable String key) {
    this.key = key;
    return this;
  }

  /**
   * Get key
   * @return key
   */
  
  @Schema(name = "key", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("key")
  public @Nullable String getKey() {
    return key;
  }

  @JsonProperty("key")
  public void setKey(@Nullable String key) {
    this.key = key;
  }

  public ApiCollectionMissingConnectionModel componentName(@Nullable String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * Get componentName
   * @return componentName
   */
  
  @Schema(name = "componentName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentName")
  public @Nullable String getComponentName() {
    return componentName;
  }

  @JsonProperty("componentName")
  public void setComponentName(@Nullable String componentName) {
    this.componentName = componentName;
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
    return Objects.equals(this.endpointName, apiCollectionMissingConnection.endpointName) &&
        Objects.equals(this.workflowNodeName, apiCollectionMissingConnection.workflowNodeName) &&
        Objects.equals(this.key, apiCollectionMissingConnection.key) &&
        Objects.equals(this.componentName, apiCollectionMissingConnection.componentName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(endpointName, workflowNodeName, key, componentName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCollectionMissingConnectionModel {\n");
    sb.append("    endpointName: ").append(toIndentedString(endpointName)).append("\n");
    sb.append("    workflowNodeName: ").append(toIndentedString(workflowNodeName)).append("\n");
    sb.append("    key: ").append(toIndentedString(key)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
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

