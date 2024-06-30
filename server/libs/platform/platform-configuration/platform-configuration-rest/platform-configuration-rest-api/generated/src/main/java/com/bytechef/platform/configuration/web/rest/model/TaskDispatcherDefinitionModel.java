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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-30T07:20:54.243996+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public class TaskDispatcherDefinitionModel {

  private Boolean dynamicOutput;

  private String description;

  private String icon;

  private String name;

  private Boolean outputDefined;

  @Valid
  private List<@Valid PropertyModel> properties = new ArrayList<>();

  private ResourcesModel resources;

  @Valid
  private List<@Valid PropertyModel> taskProperties = new ArrayList<>();

  private String title;

  private Boolean variablePropertiesDefined;

  private Integer version;

  public TaskDispatcherDefinitionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TaskDispatcherDefinitionModel(Boolean dynamicOutput, String name, Boolean outputDefined, Integer version) {
    this.dynamicOutput = dynamicOutput;
    this.name = name;
    this.outputDefined = outputDefined;
    this.version = version;
  }

  public TaskDispatcherDefinitionModel dynamicOutput(Boolean dynamicOutput) {
    this.dynamicOutput = dynamicOutput;
    return this;
  }

  /**
   * Does task dispatcher define dynamic output schema.
   * @return dynamicOutput
  */
  @NotNull 
  @Schema(name = "dynamicOutput", description = "Does task dispatcher define dynamic output schema.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("dynamicOutput")
  public Boolean getDynamicOutput() {
    return dynamicOutput;
  }

  public void setDynamicOutput(Boolean dynamicOutput) {
    this.dynamicOutput = dynamicOutput;
  }

  public TaskDispatcherDefinitionModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The description.
   * @return description
  */
  
  @Schema(name = "description", description = "The description.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public TaskDispatcherDefinitionModel icon(String icon) {
    this.icon = icon;
    return this;
  }

  /**
   * The icon.
   * @return icon
  */
  
  @Schema(name = "icon", description = "The icon.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("icon")
  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
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
   * Does task dispatcher define output schema.
   * @return outputDefined
  */
  @NotNull 
  @Schema(name = "outputDefined", description = "Does task dispatcher define output schema.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("outputDefined")
  public Boolean getOutputDefined() {
    return outputDefined;
  }

  public void setOutputDefined(Boolean outputDefined) {
    this.outputDefined = outputDefined;
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

  public TaskDispatcherDefinitionModel resources(ResourcesModel resources) {
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
  public ResourcesModel getResources() {
    return resources;
  }

  public void setResources(ResourcesModel resources) {
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

  public TaskDispatcherDefinitionModel title(String title) {
    this.title = title;
    return this;
  }

  /**
   * The title
   * @return title
  */
  
  @Schema(name = "title", description = "The title", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public TaskDispatcherDefinitionModel variablePropertiesDefined(Boolean variablePropertiesDefined) {
    this.variablePropertiesDefined = variablePropertiesDefined;
    return this;
  }

  /**
   * Does task dispatcher define dynamic variable properties.
   * @return variablePropertiesDefined
  */
  
  @Schema(name = "variablePropertiesDefined", description = "Does task dispatcher define dynamic variable properties.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("variablePropertiesDefined")
  public Boolean getVariablePropertiesDefined() {
    return variablePropertiesDefined;
  }

  public void setVariablePropertiesDefined(Boolean variablePropertiesDefined) {
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
    return Objects.equals(this.dynamicOutput, taskDispatcherDefinition.dynamicOutput) &&
        Objects.equals(this.description, taskDispatcherDefinition.description) &&
        Objects.equals(this.icon, taskDispatcherDefinition.icon) &&
        Objects.equals(this.name, taskDispatcherDefinition.name) &&
        Objects.equals(this.outputDefined, taskDispatcherDefinition.outputDefined) &&
        Objects.equals(this.properties, taskDispatcherDefinition.properties) &&
        Objects.equals(this.resources, taskDispatcherDefinition.resources) &&
        Objects.equals(this.taskProperties, taskDispatcherDefinition.taskProperties) &&
        Objects.equals(this.title, taskDispatcherDefinition.title) &&
        Objects.equals(this.variablePropertiesDefined, taskDispatcherDefinition.variablePropertiesDefined) &&
        Objects.equals(this.version, taskDispatcherDefinition.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dynamicOutput, description, icon, name, outputDefined, properties, resources, taskProperties, title, variablePropertiesDefined, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TaskDispatcherDefinitionModel {\n");
    sb.append("    dynamicOutput: ").append(toIndentedString(dynamicOutput)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    outputDefined: ").append(toIndentedString(outputDefined)).append("\n");
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

