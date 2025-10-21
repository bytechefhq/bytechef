package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.HelpModel;
import com.bytechef.platform.configuration.web.rest.model.PropertyModel;
import com.bytechef.platform.configuration.web.rest.model.TriggerTypeModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A trigger definition defines ways to trigger workflows from the outside services.
 */

@Schema(name = "TriggerDefinition", description = "A trigger definition defines ways to trigger workflows from the outside services.")
@JsonTypeName("TriggerDefinition")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-21T12:06:41.161145+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public class TriggerDefinitionModel {

  private @Nullable String componentName;

  private @Nullable Integer componentVersion;

  private @Nullable String description;

  private @Nullable HelpModel help;

  private String name;

  private Boolean outputDefined;

  private Boolean outputFunctionDefined;

  private @Nullable Boolean outputSchemaDefined;

  @Valid
  private List<@Valid PropertyModel> properties = new ArrayList<>();

  private @Nullable String title;

  private TriggerTypeModel type;

  private @Nullable Boolean workflowNodeDescriptionDefined;

  public TriggerDefinitionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TriggerDefinitionModel(String name, Boolean outputDefined, Boolean outputFunctionDefined, TriggerTypeModel type) {
    this.name = name;
    this.outputDefined = outputDefined;
    this.outputFunctionDefined = outputFunctionDefined;
    this.type = type;
  }

  public TriggerDefinitionModel componentName(@Nullable String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The component name.
   * @return componentName
   */
  
  @Schema(name = "componentName", description = "The component name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentName")
  public @Nullable String getComponentName() {
    return componentName;
  }

  public void setComponentName(@Nullable String componentName) {
    this.componentName = componentName;
  }

  public TriggerDefinitionModel componentVersion(@Nullable Integer componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * The component version.
   * @return componentVersion
   */
  
  @Schema(name = "componentVersion", description = "The component version.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentVersion")
  public @Nullable Integer getComponentVersion() {
    return componentVersion;
  }

  public void setComponentVersion(@Nullable Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  public TriggerDefinitionModel description(@Nullable String description) {
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

  public TriggerDefinitionModel help(@Nullable HelpModel help) {
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

  public TriggerDefinitionModel name(String name) {
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

  public TriggerDefinitionModel outputDefined(Boolean outputDefined) {
    this.outputDefined = outputDefined;
    return this;
  }

  /**
   * Does trigger defines output.
   * @return outputDefined
   */
  @NotNull 
  @Schema(name = "outputDefined", description = "Does trigger defines output.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("outputDefined")
  public Boolean getOutputDefined() {
    return outputDefined;
  }

  public void setOutputDefined(Boolean outputDefined) {
    this.outputDefined = outputDefined;
  }

  public TriggerDefinitionModel outputFunctionDefined(Boolean outputFunctionDefined) {
    this.outputFunctionDefined = outputFunctionDefined;
    return this;
  }

  /**
   * Does trigger defines output function.
   * @return outputFunctionDefined
   */
  @NotNull 
  @Schema(name = "outputFunctionDefined", description = "Does trigger defines output function.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("outputFunctionDefined")
  public Boolean getOutputFunctionDefined() {
    return outputFunctionDefined;
  }

  public void setOutputFunctionDefined(Boolean outputFunctionDefined) {
    this.outputFunctionDefined = outputFunctionDefined;
  }

  public TriggerDefinitionModel outputSchemaDefined(@Nullable Boolean outputSchemaDefined) {
    this.outputSchemaDefined = outputSchemaDefined;
    return this;
  }

  /**
   * Does trigger defines output schema.
   * @return outputSchemaDefined
   */
  
  @Schema(name = "outputSchemaDefined", description = "Does trigger defines output schema.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("outputSchemaDefined")
  public @Nullable Boolean getOutputSchemaDefined() {
    return outputSchemaDefined;
  }

  public void setOutputSchemaDefined(@Nullable Boolean outputSchemaDefined) {
    this.outputSchemaDefined = outputSchemaDefined;
  }

  public TriggerDefinitionModel properties(List<@Valid PropertyModel> properties) {
    this.properties = properties;
    return this;
  }

  public TriggerDefinitionModel addPropertiesItem(PropertyModel propertiesItem) {
    if (this.properties == null) {
      this.properties = new ArrayList<>();
    }
    this.properties.add(propertiesItem);
    return this;
  }

  /**
   * The list of action properties.
   * @return properties
   */
  @Valid 
  @Schema(name = "properties", description = "The list of action properties.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("properties")
  public List<@Valid PropertyModel> getProperties() {
    return properties;
  }

  public void setProperties(List<@Valid PropertyModel> properties) {
    this.properties = properties;
  }

  public TriggerDefinitionModel title(@Nullable String title) {
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

  public TriggerDefinitionModel type(TriggerTypeModel type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull @Valid 
  @Schema(name = "type", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public TriggerTypeModel getType() {
    return type;
  }

  public void setType(TriggerTypeModel type) {
    this.type = type;
  }

  public TriggerDefinitionModel workflowNodeDescriptionDefined(@Nullable Boolean workflowNodeDescriptionDefined) {
    this.workflowNodeDescriptionDefined = workflowNodeDescriptionDefined;
    return this;
  }

  /**
   * Does trigger define dynamic node description.
   * @return workflowNodeDescriptionDefined
   */
  
  @Schema(name = "workflowNodeDescriptionDefined", description = "Does trigger define dynamic node description.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowNodeDescriptionDefined")
  public @Nullable Boolean getWorkflowNodeDescriptionDefined() {
    return workflowNodeDescriptionDefined;
  }

  public void setWorkflowNodeDescriptionDefined(@Nullable Boolean workflowNodeDescriptionDefined) {
    this.workflowNodeDescriptionDefined = workflowNodeDescriptionDefined;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TriggerDefinitionModel triggerDefinition = (TriggerDefinitionModel) o;
    return Objects.equals(this.componentName, triggerDefinition.componentName) &&
        Objects.equals(this.componentVersion, triggerDefinition.componentVersion) &&
        Objects.equals(this.description, triggerDefinition.description) &&
        Objects.equals(this.help, triggerDefinition.help) &&
        Objects.equals(this.name, triggerDefinition.name) &&
        Objects.equals(this.outputDefined, triggerDefinition.outputDefined) &&
        Objects.equals(this.outputFunctionDefined, triggerDefinition.outputFunctionDefined) &&
        Objects.equals(this.outputSchemaDefined, triggerDefinition.outputSchemaDefined) &&
        Objects.equals(this.properties, triggerDefinition.properties) &&
        Objects.equals(this.title, triggerDefinition.title) &&
        Objects.equals(this.type, triggerDefinition.type) &&
        Objects.equals(this.workflowNodeDescriptionDefined, triggerDefinition.workflowNodeDescriptionDefined);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, componentVersion, description, help, name, outputDefined, outputFunctionDefined, outputSchemaDefined, properties, title, type, workflowNodeDescriptionDefined);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TriggerDefinitionModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    help: ").append(toIndentedString(help)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    outputDefined: ").append(toIndentedString(outputDefined)).append("\n");
    sb.append("    outputFunctionDefined: ").append(toIndentedString(outputFunctionDefined)).append("\n");
    sb.append("    outputSchemaDefined: ").append(toIndentedString(outputSchemaDefined)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    workflowNodeDescriptionDefined: ").append(toIndentedString(workflowNodeDescriptionDefined)).append("\n");
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

