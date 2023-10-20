package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.DisplayModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ResourcesModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-01T08:54:46.758794+01:00[Europe/Zagreb]")
public class TaskDispatcherDefinitionModel {

  @JsonProperty("display")
  private DisplayModel display;

  @JsonProperty("name")
  private String name;

  @JsonProperty("outputSchema")
  @Valid
  private List<PropertyModel> outputSchema = null;

  @JsonProperty("properties")
  @Valid
  private List<PropertyModel> properties = null;

  @JsonProperty("resources")
  private ResourcesModel resources;

  @JsonProperty("version")
  private Integer version;

  @JsonProperty("taskProperties")
  @Valid
  private List<PropertyModel> taskProperties = null;

  public TaskDispatcherDefinitionModel display(DisplayModel display) {
    this.display = display;
    return this;
  }

  /**
   * Get display
   * @return display
  */
  @Valid 
  @Schema(name = "display", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public DisplayModel getDisplay() {
    return display;
  }

  public void setDisplay(DisplayModel display) {
    this.display = display;
  }

  public TaskDispatcherDefinitionModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The task dispatcher name..
   * @return name
  */
  
  @Schema(name = "name", description = "The task dispatcher name..", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public TaskDispatcherDefinitionModel outputSchema(List<PropertyModel> outputSchema) {
    this.outputSchema = outputSchema;
    return this;
  }

  public TaskDispatcherDefinitionModel addOutputSchemaItem(PropertyModel outputSchemaItem) {
    if (this.outputSchema == null) {
      this.outputSchema = new ArrayList<>();
    }
    this.outputSchema.add(outputSchemaItem);
    return this;
  }

  /**
   * The output schema of a task dispatching result.
   * @return outputSchema
  */
  @Valid 
  @Schema(name = "outputSchema", description = "The output schema of a task dispatching result.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public List<PropertyModel> getOutputSchema() {
    return outputSchema;
  }

  public void setOutputSchema(List<PropertyModel> outputSchema) {
    this.outputSchema = outputSchema;
  }

  public TaskDispatcherDefinitionModel properties(List<PropertyModel> properties) {
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
  public List<PropertyModel> getProperties() {
    return properties;
  }

  public void setProperties(List<PropertyModel> properties) {
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
  public ResourcesModel getResources() {
    return resources;
  }

  public void setResources(ResourcesModel resources) {
    this.resources = resources;
  }

  public TaskDispatcherDefinitionModel version(Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * @return version
  */
  
  @Schema(name = "version", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public TaskDispatcherDefinitionModel taskProperties(List<PropertyModel> taskProperties) {
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
  public List<PropertyModel> getTaskProperties() {
    return taskProperties;
  }

  public void setTaskProperties(List<PropertyModel> taskProperties) {
    this.taskProperties = taskProperties;
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
    return Objects.equals(this.display, taskDispatcherDefinition.display) &&
        Objects.equals(this.name, taskDispatcherDefinition.name) &&
        Objects.equals(this.outputSchema, taskDispatcherDefinition.outputSchema) &&
        Objects.equals(this.properties, taskDispatcherDefinition.properties) &&
        Objects.equals(this.resources, taskDispatcherDefinition.resources) &&
        Objects.equals(this.version, taskDispatcherDefinition.version) &&
        Objects.equals(this.taskProperties, taskDispatcherDefinition.taskProperties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(display, name, outputSchema, properties, resources, version, taskProperties);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TaskDispatcherDefinitionModel {\n");
    sb.append("    display: ").append(toIndentedString(display)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    outputSchema: ").append(toIndentedString(outputSchema)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
    sb.append("    resources: ").append(toIndentedString(resources)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    taskProperties: ").append(toIndentedString(taskProperties)).append("\n");
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

