package com.bytechef.ee.automation.app.public_.web.rest.model;

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
 * A tool the calling app may execute.
 */

@Schema(name = "AppTool", description = "A tool the calling app may execute.")
@JsonTypeName("AppTool")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:31.849354+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class AppToolModel {

  private String name;

  private String bindingKey;

  private String componentName;

  private String toolName;

  private @Nullable String description;

  private String parameters;

  public AppToolModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AppToolModel(String name, String bindingKey, String componentName, String toolName, String parameters) {
    this.name = name;
    this.bindingKey = bindingKey;
    this.componentName = componentName;
    this.toolName = toolName;
    this.parameters = parameters;
  }

  public AppToolModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * <BINDING_KEY>__<TOOL_NAME>, unique per app, for LLM function calling.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "<BINDING_KEY>__<TOOL_NAME>, unique per app, for LLM function calling.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public AppToolModel bindingKey(String bindingKey) {
    this.bindingKey = bindingKey;
    return this;
  }

  /**
   * Get bindingKey
   * @return bindingKey
   */
  @NotNull 
  @Schema(name = "bindingKey", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("bindingKey")
  public String getBindingKey() {
    return bindingKey;
  }

  @JsonProperty("bindingKey")
  public void setBindingKey(String bindingKey) {
    this.bindingKey = bindingKey;
  }

  public AppToolModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * Get componentName
   * @return componentName
   */
  @NotNull 
  @Schema(name = "componentName", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  @JsonProperty("componentName")
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public AppToolModel toolName(String toolName) {
    this.toolName = toolName;
    return this;
  }

  /**
   * Get toolName
   * @return toolName
   */
  @NotNull 
  @Schema(name = "toolName", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("toolName")
  public String getToolName() {
    return toolName;
  }

  @JsonProperty("toolName")
  public void setToolName(String toolName) {
    this.toolName = toolName;
  }

  public AppToolModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
   */
  
  @Schema(name = "description", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public AppToolModel parameters(String parameters) {
    this.parameters = parameters;
    return this;
  }

  /**
   * JSON Schema of the tool input.
   * @return parameters
   */
  @NotNull 
  @Schema(name = "parameters", description = "JSON Schema of the tool input.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("parameters")
  public String getParameters() {
    return parameters;
  }

  @JsonProperty("parameters")
  public void setParameters(String parameters) {
    this.parameters = parameters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppToolModel appTool = (AppToolModel) o;
    return Objects.equals(this.name, appTool.name) &&
        Objects.equals(this.bindingKey, appTool.bindingKey) &&
        Objects.equals(this.componentName, appTool.componentName) &&
        Objects.equals(this.toolName, appTool.toolName) &&
        Objects.equals(this.description, appTool.description) &&
        Objects.equals(this.parameters, appTool.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, bindingKey, componentName, toolName, description, parameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppToolModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    bindingKey: ").append(toIndentedString(bindingKey)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    toolName: ").append(toIndentedString(toolName)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
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

