package com.bytechef.hermes.task.dispatcher.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.task.dispatcher.web.rest.model.DisplayModel;
import com.bytechef.hermes.task.dispatcher.web.rest.model.ResourcesModel;
import com.bytechef.hermes.task.dispatcher.web.rest.model.TaskDispatcherDefinitionInputsInnerModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * TaskDispatcherDefinitionModel
 */

@JsonTypeName("TaskDispatcherDefinition")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-27T16:28:51.543539+02:00[Europe/Zagreb]")
public class TaskDispatcherDefinitionModel {

  @JsonProperty("display")
  private DisplayModel display;

  @JsonProperty("inputs")
  @Valid
  private List<TaskDispatcherDefinitionInputsInnerModel> inputs = null;

  @JsonProperty("name")
  private String name;

  @JsonProperty("resources")
  private ResourcesModel resources;

  @JsonProperty("version")
  private Integer version;

  public TaskDispatcherDefinitionModel display(DisplayModel display) {
    this.display = display;
    return this;
  }

  /**
   * Get display
   * @return display
  */
  @Valid 
  @Schema(name = "display", required = false)
  public DisplayModel getDisplay() {
    return display;
  }

  public void setDisplay(DisplayModel display) {
    this.display = display;
  }

  public TaskDispatcherDefinitionModel inputs(List<TaskDispatcherDefinitionInputsInnerModel> inputs) {
    this.inputs = inputs;
    return this;
  }

  public TaskDispatcherDefinitionModel addInputsItem(TaskDispatcherDefinitionInputsInnerModel inputsItem) {
    if (this.inputs == null) {
      this.inputs = new ArrayList<>();
    }
    this.inputs.add(inputsItem);
    return this;
  }

  /**
   * Get inputs
   * @return inputs
  */
  @Valid 
  @Schema(name = "inputs", required = false)
  public List<TaskDispatcherDefinitionInputsInnerModel> getInputs() {
    return inputs;
  }

  public void setInputs(List<TaskDispatcherDefinitionInputsInnerModel> inputs) {
    this.inputs = inputs;
  }

  public TaskDispatcherDefinitionModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
  */
  
  @Schema(name = "name", required = false)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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
  @Schema(name = "resources", required = false)
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
  
  @Schema(name = "version", required = false)
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
    return Objects.equals(this.display, taskDispatcherDefinition.display) &&
        Objects.equals(this.inputs, taskDispatcherDefinition.inputs) &&
        Objects.equals(this.name, taskDispatcherDefinition.name) &&
        Objects.equals(this.resources, taskDispatcherDefinition.resources) &&
        Objects.equals(this.version, taskDispatcherDefinition.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(display, inputs, name, resources, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TaskDispatcherDefinitionModel {\n");
    sb.append("    display: ").append(toIndentedString(display)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    resources: ").append(toIndentedString(resources)).append("\n");
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

