package com.bytechef.automation.configuration.web.rest.model;

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
 * ComponentConnectionModel
 */

@JsonTypeName("ComponentConnection")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:32.946393+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class ComponentConnectionModel {

  private String componentName;

  private Integer componentVersion;

  private String key;

  private Boolean required;

  private String workflowNodeName;

  public ComponentConnectionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ComponentConnectionModel(String componentName, Integer componentVersion, String key, Boolean required, String workflowNodeName) {
    this.componentName = componentName;
    this.componentVersion = componentVersion;
    this.key = key;
    this.required = required;
    this.workflowNodeName = workflowNodeName;
  }

  public ComponentConnectionModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of the component
   * @return componentName
   */
  @NotNull 
  @Schema(name = "componentName", description = "The name of the component", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public ComponentConnectionModel componentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * The version of the component
   * @return componentVersion
   */
  @NotNull 
  @Schema(name = "componentVersion", description = "The version of the component", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentVersion")
  public Integer getComponentVersion() {
    return componentVersion;
  }

  public void setComponentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  public ComponentConnectionModel key(String key) {
    this.key = key;
    return this;
  }

  /**
   * The key of the connection
   * @return key
   */
  @NotNull 
  @Schema(name = "key", description = "The key of the connection", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("key")
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public ComponentConnectionModel required(Boolean required) {
    this.required = required;
    return this;
  }

  /**
   * If the connection is required, or not
   * @return required
   */
  @NotNull 
  @Schema(name = "required", description = "If the connection is required, or not", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("required")
  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public ComponentConnectionModel workflowNodeName(String workflowNodeName) {
    this.workflowNodeName = workflowNodeName;
    return this;
  }

  /**
   * Get workflowNodeName
   * @return workflowNodeName
   */
  @NotNull 
  @Schema(name = "workflowNodeName", requiredMode = Schema.RequiredMode.REQUIRED)
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
    ComponentConnectionModel componentConnection = (ComponentConnectionModel) o;
    return Objects.equals(this.componentName, componentConnection.componentName) &&
        Objects.equals(this.componentVersion, componentConnection.componentVersion) &&
        Objects.equals(this.key, componentConnection.key) &&
        Objects.equals(this.required, componentConnection.required) &&
        Objects.equals(this.workflowNodeName, componentConnection.workflowNodeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, componentVersion, key, required, workflowNodeName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComponentConnectionModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    key: ").append(toIndentedString(key)).append("\n");
    sb.append("    required: ").append(toIndentedString(required)).append("\n");
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

