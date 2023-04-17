package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.DisplayModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ResourcesModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
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

@Schema(name = "TaskDispatcherDefinitionBasic", description = "A task dispatcher defines a strategy for dispatching tasks to be executed.")
@JsonTypeName("TaskDispatcherDefinitionBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-15T19:47:32.550589+02:00[Europe/Zagreb]")
public class TaskDispatcherDefinitionBasicModel {

  private DisplayModel display;

  private String name;

  private ResourcesModel resources;

  /**
   * Default constructor
   * @deprecated Use {@link TaskDispatcherDefinitionBasicModel#TaskDispatcherDefinitionBasicModel(DisplayModel, String)}
   */
  @Deprecated
  public TaskDispatcherDefinitionBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TaskDispatcherDefinitionBasicModel(DisplayModel display, String name) {
    this.display = display;
    this.name = name;
  }

  public TaskDispatcherDefinitionBasicModel display(DisplayModel display) {
    this.display = display;
    return this;
  }

  /**
   * Get display
   * @return display
  */
  @NotNull @Valid 
  @Schema(name = "display", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("display")
  public DisplayModel getDisplay() {
    return display;
  }

  public void setDisplay(DisplayModel display) {
    this.display = display;
  }

  public TaskDispatcherDefinitionBasicModel name(String name) {
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

  public TaskDispatcherDefinitionBasicModel resources(ResourcesModel resources) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TaskDispatcherDefinitionBasicModel taskDispatcherDefinitionBasic = (TaskDispatcherDefinitionBasicModel) o;
    return Objects.equals(this.display, taskDispatcherDefinitionBasic.display) &&
        Objects.equals(this.name, taskDispatcherDefinitionBasic.name) &&
        Objects.equals(this.resources, taskDispatcherDefinitionBasic.resources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(display, name, resources);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TaskDispatcherDefinitionBasicModel {\n");
    sb.append("    display: ").append(toIndentedString(display)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    resources: ").append(toIndentedString(resources)).append("\n");
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

