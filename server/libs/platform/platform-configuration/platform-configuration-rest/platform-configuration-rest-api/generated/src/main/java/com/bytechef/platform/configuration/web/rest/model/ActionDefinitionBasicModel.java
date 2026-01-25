package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.HelpModel;
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
 * An action is a portion of reusable code that accomplish a specific task. When building a workflow, each action is represented as a task inside the workflow. The task &#39;type&#39; property is defined as [component name]/v[component version]/[action name]. Action properties are used to set properties of the task inside the workflow.
 */

@Schema(name = "ActionDefinitionBasic", description = "An action is a portion of reusable code that accomplish a specific task. When building a workflow, each action is represented as a task inside the workflow. The task 'type' property is defined as [component name]/v[component version]/[action name]. Action properties are used to set properties of the task inside the workflow.")
@JsonTypeName("ActionDefinitionBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-23T12:36:59.494207+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class ActionDefinitionBasicModel {

  private String componentName;

  private Integer componentVersion;

  private @Nullable String description;

  private @Nullable HelpModel help;

  private String name;

  private Boolean outputDefined;

  private Boolean outputFunctionDefined;

  private @Nullable Boolean outputSchemaDefined;

  private @Nullable Boolean sseStreamResponse;

  private @Nullable String title;

  public ActionDefinitionBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ActionDefinitionBasicModel(String componentName, Integer componentVersion, String name, Boolean outputDefined, Boolean outputFunctionDefined) {
    this.componentName = componentName;
    this.componentVersion = componentVersion;
    this.name = name;
    this.outputDefined = outputDefined;
    this.outputFunctionDefined = outputFunctionDefined;
  }

  public ActionDefinitionBasicModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The component name.
   * @return componentName
   */
  @NotNull 
  @Schema(name = "componentName", description = "The component name.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public ActionDefinitionBasicModel componentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * The component version.
   * @return componentVersion
   */
  @NotNull 
  @Schema(name = "componentVersion", description = "The component version.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentVersion")
  public Integer getComponentVersion() {
    return componentVersion;
  }

  public void setComponentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  public ActionDefinitionBasicModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description.
   * @return description
   */
  
  @Schema(name = "description", description = "The description.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public ActionDefinitionBasicModel help(@Nullable HelpModel help) {
    this.help = help;
    return this;
  }

  /**
   * Get help
   * @return help
   */
  @Valid 
  @Schema(name = "help", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("help")
  public @Nullable HelpModel getHelp() {
    return help;
  }

  public void setHelp(@Nullable HelpModel help) {
    this.help = help;
  }

  public ActionDefinitionBasicModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The action name.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The action name.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ActionDefinitionBasicModel outputDefined(Boolean outputDefined) {
    this.outputDefined = outputDefined;
    return this;
  }

  /**
   * Does action defines output.
   * @return outputDefined
   */
  @NotNull 
  @Schema(name = "outputDefined", description = "Does action defines output.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("outputDefined")
  public Boolean getOutputDefined() {
    return outputDefined;
  }

  public void setOutputDefined(Boolean outputDefined) {
    this.outputDefined = outputDefined;
  }

  public ActionDefinitionBasicModel outputFunctionDefined(Boolean outputFunctionDefined) {
    this.outputFunctionDefined = outputFunctionDefined;
    return this;
  }

  /**
   * Does action defines output function.
   * @return outputFunctionDefined
   */
  @NotNull 
  @Schema(name = "outputFunctionDefined", description = "Does action defines output function.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("outputFunctionDefined")
  public Boolean getOutputFunctionDefined() {
    return outputFunctionDefined;
  }

  public void setOutputFunctionDefined(Boolean outputFunctionDefined) {
    this.outputFunctionDefined = outputFunctionDefined;
  }

  public ActionDefinitionBasicModel outputSchemaDefined(@Nullable Boolean outputSchemaDefined) {
    this.outputSchemaDefined = outputSchemaDefined;
    return this;
  }

  /**
   * Does action defines output schema.
   * @return outputSchemaDefined
   */
  
  @Schema(name = "outputSchemaDefined", description = "Does action defines output schema.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("outputSchemaDefined")
  public @Nullable Boolean getOutputSchemaDefined() {
    return outputSchemaDefined;
  }

  public void setOutputSchemaDefined(@Nullable Boolean outputSchemaDefined) {
    this.outputSchemaDefined = outputSchemaDefined;
  }

  public ActionDefinitionBasicModel sseStreamResponse(@Nullable Boolean sseStreamResponse) {
    this.sseStreamResponse = sseStreamResponse;
    return this;
  }

  /**
   * Does action support SSE streaming.
   * @return sseStreamResponse
   */
  
  @Schema(name = "sseStreamResponse", description = "Does action support SSE streaming.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("sseStreamResponse")
  public @Nullable Boolean getSseStreamResponse() {
    return sseStreamResponse;
  }

  public void setSseStreamResponse(@Nullable Boolean sseStreamResponse) {
    this.sseStreamResponse = sseStreamResponse;
  }

  public ActionDefinitionBasicModel title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * The title
   * @return title
   */
  
  @Schema(name = "title", description = "The title", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ActionDefinitionBasicModel actionDefinitionBasic = (ActionDefinitionBasicModel) o;
    return Objects.equals(this.componentName, actionDefinitionBasic.componentName) &&
        Objects.equals(this.componentVersion, actionDefinitionBasic.componentVersion) &&
        Objects.equals(this.description, actionDefinitionBasic.description) &&
        Objects.equals(this.help, actionDefinitionBasic.help) &&
        Objects.equals(this.name, actionDefinitionBasic.name) &&
        Objects.equals(this.outputDefined, actionDefinitionBasic.outputDefined) &&
        Objects.equals(this.outputFunctionDefined, actionDefinitionBasic.outputFunctionDefined) &&
        Objects.equals(this.outputSchemaDefined, actionDefinitionBasic.outputSchemaDefined) &&
        Objects.equals(this.sseStreamResponse, actionDefinitionBasic.sseStreamResponse) &&
        Objects.equals(this.title, actionDefinitionBasic.title);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, componentVersion, description, help, name, outputDefined, outputFunctionDefined, outputSchemaDefined, sseStreamResponse, title);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ActionDefinitionBasicModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    help: ").append(toIndentedString(help)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    outputDefined: ").append(toIndentedString(outputDefined)).append("\n");
    sb.append("    outputFunctionDefined: ").append(toIndentedString(outputFunctionDefined)).append("\n");
    sb.append("    outputSchemaDefined: ").append(toIndentedString(outputSchemaDefined)).append("\n");
    sb.append("    sseStreamResponse: ").append(toIndentedString(sseStreamResponse)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
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

