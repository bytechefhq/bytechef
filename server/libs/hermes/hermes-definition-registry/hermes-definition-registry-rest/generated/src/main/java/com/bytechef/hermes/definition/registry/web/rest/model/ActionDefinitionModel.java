package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.DisplayModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyModel;
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
 * An action is a portion of reusable code that accomplish a specific task. When building a workflow, each action is represented as a task inside the workflow. The task &#39;type&#39; property is defined as [component name]/v[component version]/[action name]. Action properties are used to set properties of the task inside the workflow.
 */

@Schema(name = "ActionDefinition", description = "An action is a portion of reusable code that accomplish a specific task. When building a workflow, each action is represented as a task inside the workflow. The task 'type' property is defined as [component name]/v[component version]/[action name]. Action properties are used to set properties of the task inside the workflow.")
@JsonTypeName("ActionDefinition")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-02-20T11:29:33.968820+01:00[Europe/Zagreb]")
public class ActionDefinitionModel {

  @JsonProperty("display")
  private DisplayModel display;

  @JsonProperty("exampleOutput")
  private Object exampleOutput;

  @JsonProperty("name")
  private String name;

  @JsonProperty("output")
  @Valid
  private List<PropertyModel> output = null;

  @JsonProperty("properties")
  @Valid
  private List<PropertyModel> properties = null;

  @JsonProperty("performFunction")
  private Object performFunction;

  public ActionDefinitionModel display(DisplayModel display) {
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

  public ActionDefinitionModel exampleOutput(Object exampleOutput) {
    this.exampleOutput = exampleOutput;
    return this;
  }

  /**
   * The example of the action's output.
   * @return exampleOutput
  */
  
  @Schema(name = "exampleOutput", description = "The example of the action's output.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Object getExampleOutput() {
    return exampleOutput;
  }

  public void setExampleOutput(Object exampleOutput) {
    this.exampleOutput = exampleOutput;
  }

  public ActionDefinitionModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The action name.
   * @return name
  */
  
  @Schema(name = "name", description = "The action name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ActionDefinitionModel output(List<PropertyModel> output) {
    this.output = output;
    return this;
  }

  public ActionDefinitionModel addOutputItem(PropertyModel outputItem) {
    if (this.output == null) {
      this.output = new ArrayList<>();
    }
    this.output.add(outputItem);
    return this;
  }

  /**
   * The output schema of an execution result.
   * @return output
  */
  @Valid 
  @Schema(name = "output", description = "The output schema of an execution result.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public List<PropertyModel> getOutput() {
    return output;
  }

  public void setOutput(List<PropertyModel> output) {
    this.output = output;
  }

  public ActionDefinitionModel properties(List<PropertyModel> properties) {
    this.properties = properties;
    return this;
  }

  public ActionDefinitionModel addPropertiesItem(PropertyModel propertiesItem) {
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
  public List<PropertyModel> getProperties() {
    return properties;
  }

  public void setProperties(List<PropertyModel> properties) {
    this.properties = properties;
  }

  public ActionDefinitionModel performFunction(Object performFunction) {
    this.performFunction = performFunction;
    return this;
  }

  /**
   * Contains information required for a connection's authorization.
   * @return performFunction
  */
  
  @Schema(name = "performFunction", description = "Contains information required for a connection's authorization.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Object getPerformFunction() {
    return performFunction;
  }

  public void setPerformFunction(Object performFunction) {
    this.performFunction = performFunction;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ActionDefinitionModel actionDefinition = (ActionDefinitionModel) o;
    return Objects.equals(this.display, actionDefinition.display) &&
        Objects.equals(this.exampleOutput, actionDefinition.exampleOutput) &&
        Objects.equals(this.name, actionDefinition.name) &&
        Objects.equals(this.output, actionDefinition.output) &&
        Objects.equals(this.properties, actionDefinition.properties) &&
        Objects.equals(this.performFunction, actionDefinition.performFunction);
  }

  @Override
  public int hashCode() {
    return Objects.hash(display, exampleOutput, name, output, properties, performFunction);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ActionDefinitionModel {\n");
    sb.append("    display: ").append(toIndentedString(display)).append("\n");
    sb.append("    exampleOutput: ").append(toIndentedString(exampleOutput)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    output: ").append(toIndentedString(output)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
    sb.append("    performFunction: ").append(toIndentedString(performFunction)).append("\n");
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

