package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.PropertyModel;
import com.bytechef.platform.configuration.web.rest.model.ResourcesModel;
import com.bytechef.platform.configuration.web.rest.model.TaskDispatcherOutputSchemaModel;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-01-18T05:49:30.457496+01:00[Europe/Zagreb]")
public class TaskDispatcherDefinitionModel {

  private String description;

  private String icon;

  private String name;

  private TaskDispatcherOutputSchemaModel outputSchema;

  private Boolean outputSchemaDataSource;

  @Valid
  private List<@Valid PropertyModel> properties;

  private ResourcesModel resources;

  @Valid
  private List<@Valid PropertyModel> taskProperties;

  private String title;

  @Valid
  private List<@Valid PropertyModel> variableProperties;

  private Boolean variablePropertiesDataSource;

  private Integer version;

  public TaskDispatcherDefinitionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TaskDispatcherDefinitionModel(String name, Integer version) {
    this.name = name;
    this.version = version;
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

  public TaskDispatcherDefinitionModel outputSchema(TaskDispatcherOutputSchemaModel outputSchema) {
    this.outputSchema = outputSchema;
    return this;
  }

  /**
   * Get outputSchema
   * @return outputSchema
  */
  @Valid 
  @Schema(name = "outputSchema", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("outputSchema")
  public TaskDispatcherOutputSchemaModel getOutputSchema() {
    return outputSchema;
  }

  public void setOutputSchema(TaskDispatcherOutputSchemaModel outputSchema) {
    this.outputSchema = outputSchema;
  }

  public TaskDispatcherDefinitionModel outputSchemaDataSource(Boolean outputSchemaDataSource) {
    this.outputSchemaDataSource = outputSchemaDataSource;
    return this;
  }

  /**
   * Does task dispatcher has defined dynamic output schema.
   * @return outputSchemaDataSource
  */
  
  @Schema(name = "outputSchemaDataSource", description = "Does task dispatcher has defined dynamic output schema.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("outputSchemaDataSource")
  public Boolean getOutputSchemaDataSource() {
    return outputSchemaDataSource;
  }

  public void setOutputSchemaDataSource(Boolean outputSchemaDataSource) {
    this.outputSchemaDataSource = outputSchemaDataSource;
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

  public TaskDispatcherDefinitionModel variableProperties(List<@Valid PropertyModel> variableProperties) {
    this.variableProperties = variableProperties;
    return this;
  }

  public TaskDispatcherDefinitionModel addVariablePropertiesItem(PropertyModel variablePropertiesItem) {
    if (this.variableProperties == null) {
      this.variableProperties = new ArrayList<>();
    }
    this.variableProperties.add(variablePropertiesItem);
    return this;
  }

  /**
   * Properties used to define tasks to be dispatched.
   * @return variableProperties
  */
  @Valid 
  @Schema(name = "variableProperties", description = "Properties used to define tasks to be dispatched.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("variableProperties")
  public List<@Valid PropertyModel> getVariableProperties() {
    return variableProperties;
  }

  public void setVariableProperties(List<@Valid PropertyModel> variableProperties) {
    this.variableProperties = variableProperties;
  }

  public TaskDispatcherDefinitionModel variablePropertiesDataSource(Boolean variablePropertiesDataSource) {
    this.variablePropertiesDataSource = variablePropertiesDataSource;
    return this;
  }

  /**
   * Does task dispatcher has defined dynamic variable properties.
   * @return variablePropertiesDataSource
  */
  
  @Schema(name = "variablePropertiesDataSource", description = "Does task dispatcher has defined dynamic variable properties.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("variablePropertiesDataSource")
  public Boolean getVariablePropertiesDataSource() {
    return variablePropertiesDataSource;
  }

  public void setVariablePropertiesDataSource(Boolean variablePropertiesDataSource) {
    this.variablePropertiesDataSource = variablePropertiesDataSource;
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
        Objects.equals(this.outputSchema, taskDispatcherDefinition.outputSchema) &&
        Objects.equals(this.outputSchemaDataSource, taskDispatcherDefinition.outputSchemaDataSource) &&
        Objects.equals(this.properties, taskDispatcherDefinition.properties) &&
        Objects.equals(this.resources, taskDispatcherDefinition.resources) &&
        Objects.equals(this.taskProperties, taskDispatcherDefinition.taskProperties) &&
        Objects.equals(this.title, taskDispatcherDefinition.title) &&
        Objects.equals(this.variableProperties, taskDispatcherDefinition.variableProperties) &&
        Objects.equals(this.variablePropertiesDataSource, taskDispatcherDefinition.variablePropertiesDataSource) &&
        Objects.equals(this.version, taskDispatcherDefinition.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, icon, name, outputSchema, outputSchemaDataSource, properties, resources, taskProperties, title, variableProperties, variablePropertiesDataSource, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TaskDispatcherDefinitionModel {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    outputSchema: ").append(toIndentedString(outputSchema)).append("\n");
    sb.append("    outputSchemaDataSource: ").append(toIndentedString(outputSchemaDataSource)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
    sb.append("    resources: ").append(toIndentedString(resources)).append("\n");
    sb.append("    taskProperties: ").append(toIndentedString(taskProperties)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    variableProperties: ").append(toIndentedString(variableProperties)).append("\n");
    sb.append("    variablePropertiesDataSource: ").append(toIndentedString(variablePropertiesDataSource)).append("\n");
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

