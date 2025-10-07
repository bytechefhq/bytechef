package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.PropertyModel;
import com.bytechef.platform.configuration.web.rest.model.ResourcesModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
 * A task dispatcher defines a strategy for dispatching tasks to be executed.
 */

@Schema(name = "TaskDispatcherDefinition", description = "A task dispatcher defines a strategy for dispatching tasks to be executed.")
@JsonTypeName("TaskDispatcherDefinition")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-07T14:49:47.000989+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public class TaskDispatcherDefinitionModel {

  private @Nullable String description;

  private @Nullable String icon;

  private String name;

  private Boolean outputDefined;

  private @Nullable Boolean outputFunctionDefined;

  private @Nullable Boolean outputSchemaDefined;

  @Valid
  private List<@Valid PropertyModel> properties = new ArrayList<>();

  private @Nullable ResourcesModel resources;

  @Valid
  private List<@Valid PropertyModel> taskProperties = new ArrayList<>();

  private @Nullable String title;

  private @Nullable Boolean variablePropertiesDefined;

  private Integer version;

  public TaskDispatcherDefinitionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TaskDispatcherDefinitionModel(String name, Boolean outputDefined, Integer version) {
    this.name = name;
    this.outputDefined = outputDefined;
    this.version = version;
  }

  public TaskDispatcherDefinitionModel description(@Nullable String description) {
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

  public TaskDispatcherDefinitionModel icon(@Nullable String icon) {
    this.icon = icon;
    return this;
  }

  /**
   * The icon.
   * @return icon
   */
  
  @Schema(name = "icon", description = "The icon.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("icon")
  public @Nullable String getIcon() {
    return icon;
  }

  public void setIcon(@Nullable String icon) {
    this.icon = icon;
  }

  public TaskDispatcherDefinitionModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The task dispatcher name..
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The task dispatcher name..", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public TaskDispatcherDefinitionModel outputDefined(Boolean outputDefined) {
    this.outputDefined = outputDefined;
    return this;
  }

  /**
   * Does task dispatcher defines output.
   * @return outputDefined
   */
  @NotNull 
  @Schema(name = "outputDefined", description = "Does task dispatcher defines output.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("outputDefined")
  public Boolean getOutputDefined() {
    return outputDefined;
  }

  public void setOutputDefined(Boolean outputDefined) {
    this.outputDefined = outputDefined;
  }

  public TaskDispatcherDefinitionModel outputFunctionDefined(@Nullable Boolean outputFunctionDefined) {
    this.outputFunctionDefined = outputFunctionDefined;
    return this;
  }

  /**
   * Does task dispatcher defines output function.
   * @return outputFunctionDefined
   */
  
  @Schema(name = "outputFunctionDefined", description = "Does task dispatcher defines output function.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("outputFunctionDefined")
  public @Nullable Boolean getOutputFunctionDefined() {
    return outputFunctionDefined;
  }

  public void setOutputFunctionDefined(@Nullable Boolean outputFunctionDefined) {
    this.outputFunctionDefined = outputFunctionDefined;
  }

  public TaskDispatcherDefinitionModel outputSchemaDefined(@Nullable Boolean outputSchemaDefined) {
    this.outputSchemaDefined = outputSchemaDefined;
    return this;
  }

  /**
   * Does task dispatcher defines output schema.
   * @return outputSchemaDefined
   */
  
  @Schema(name = "outputSchemaDefined", description = "Does task dispatcher defines output schema.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("outputSchemaDefined")
  public @Nullable Boolean getOutputSchemaDefined() {
    return outputSchemaDefined;
  }

  public void setOutputSchemaDefined(@Nullable Boolean outputSchemaDefined) {
    this.outputSchemaDefined = outputSchemaDefined;
  }

  public TaskDispatcherDefinitionModel properties(List<@Valid PropertyModel> properties) {
    this.properties = properties;
    return this;
  }

  public TaskDispatcherDefinitionModel addPropertiesItem(PropertyModel propertiesItem) {
    if (this.properties == null) {
      this.properties = new ArrayList<>();
    }
    this.properties.add(propertiesItem);
    return this;
  }

  /**
   * The list of task dispatcher properties.
   * @return properties
   */
  @Valid 
  @Schema(name = "properties", description = "The list of task dispatcher properties.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("properties")
  public List<@Valid PropertyModel> getProperties() {
    return properties;
  }

  public void setProperties(List<@Valid PropertyModel> properties) {
    this.properties = properties;
  }

  public TaskDispatcherDefinitionModel resources(@Nullable ResourcesModel resources) {
    this.resources = resources;
    return this;
  }

  /**
   * Get resources
   * @return resources
   */
  @Valid 
  @Schema(name = "resources", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("resources")
  public @Nullable ResourcesModel getResources() {
    return resources;
  }

  public void setResources(@Nullable ResourcesModel resources) {
    this.resources = resources;
  }

  public TaskDispatcherDefinitionModel taskProperties(List<@Valid PropertyModel> taskProperties) {
    this.taskProperties = taskProperties;
    return this;
  }

  public TaskDispatcherDefinitionModel addTaskPropertiesItem(PropertyModel taskPropertiesItem) {
    if (this.taskProperties == null) {
      this.taskProperties = new ArrayList<>();
    }
    this.taskProperties.add(taskPropertiesItem);
    return this;
  }

  /**
   * Properties used to define tasks to be dispatched.
   * @return taskProperties
   */
  @Valid 
  @Schema(name = "taskProperties", description = "Properties used to define tasks to be dispatched.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("taskProperties")
  public List<@Valid PropertyModel> getTaskProperties() {
    return taskProperties;
  }

  public void setTaskProperties(List<@Valid PropertyModel> taskProperties) {
    this.taskProperties = taskProperties;
  }

  public TaskDispatcherDefinitionModel title(@Nullable String title) {
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

  public TaskDispatcherDefinitionModel variablePropertiesDefined(@Nullable Boolean variablePropertiesDefined) {
    this.variablePropertiesDefined = variablePropertiesDefined;
    return this;
  }

  /**
   * Does task dispatcher define dynamic variable properties.
   * @return variablePropertiesDefined
   */
  
  @Schema(name = "variablePropertiesDefined", description = "Does task dispatcher define dynamic variable properties.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("variablePropertiesDefined")
  public @Nullable Boolean getVariablePropertiesDefined() {
    return variablePropertiesDefined;
  }

  public void setVariablePropertiesDefined(@Nullable Boolean variablePropertiesDefined) {
    this.variablePropertiesDefined = variablePropertiesDefined;
  }

  public TaskDispatcherDefinitionModel version(Integer version) {
    this.version = version;
    return this;
  }

  /**
   * The version of a task dispatcher.
   * @return version
   */
  @NotNull 
  @Schema(name = "version", description = "The version of a task dispatcher.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("version")
  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TaskDispatcherDefinitionModel taskDispatcherDefinition = (TaskDispatcherDefinitionModel) o;
    return Objects.equals(this.description, taskDispatcherDefinition.description) &&
        Objects.equals(this.icon, taskDispatcherDefinition.icon) &&
        Objects.equals(this.name, taskDispatcherDefinition.name) &&
        Objects.equals(this.outputDefined, taskDispatcherDefinition.outputDefined) &&
        Objects.equals(this.outputFunctionDefined, taskDispatcherDefinition.outputFunctionDefined) &&
        Objects.equals(this.outputSchemaDefined, taskDispatcherDefinition.outputSchemaDefined) &&
        Objects.equals(this.properties, taskDispatcherDefinition.properties) &&
        Objects.equals(this.resources, taskDispatcherDefinition.resources) &&
        Objects.equals(this.taskProperties, taskDispatcherDefinition.taskProperties) &&
        Objects.equals(this.title, taskDispatcherDefinition.title) &&
        Objects.equals(this.variablePropertiesDefined, taskDispatcherDefinition.variablePropertiesDefined) &&
        Objects.equals(this.version, taskDispatcherDefinition.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, icon, name, outputDefined, outputFunctionDefined, outputSchemaDefined, properties, resources, taskProperties, title, variablePropertiesDefined, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TaskDispatcherDefinitionModel {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    outputDefined: ").append(toIndentedString(outputDefined)).append("\n");
    sb.append("    outputFunctionDefined: ").append(toIndentedString(outputFunctionDefined)).append("\n");
    sb.append("    outputSchemaDefined: ").append(toIndentedString(outputSchemaDefined)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
    sb.append("    resources: ").append(toIndentedString(resources)).append("\n");
    sb.append("    taskProperties: ").append(toIndentedString(taskProperties)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    variablePropertiesDefined: ").append(toIndentedString(variablePropertiesDefined)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
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

