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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-01T22:58:40.927821+02:00[Europe/Zagreb]")
public class ActionDefinitionModel {

  private DisplayModel display;

  private Object exampleOutput;

  private String name;

  @Valid
  private List<@Valid PropertyModel> outputSchema;

  @Valid
  private List<@Valid PropertyModel> properties;

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
  @JsonProperty("display")
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
   * The example value of the action's output.
   * @return exampleOutput
  */
  
  @Schema(name = "exampleOutput", description = "The example value of the action's output.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("exampleOutput")
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
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ActionDefinitionModel outputSchema(List<@Valid PropertyModel> outputSchema) {
    this.outputSchema = outputSchema;
    return this;
  }

  public ActionDefinitionModel addOutputSchemaItem(PropertyModel outputSchemaItem) {
    if (this.outputSchema == null) {
      this.outputSchema = new ArrayList<>();
    }
    this.outputSchema.add(outputSchemaItem);
    return this;
  }

  /**
   * The output schema of an execution result.
   * @return outputSchema
  */
  @Valid 
  @Schema(name = "outputSchema", description = "The output schema of an execution result.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("outputSchema")
  public List<@Valid PropertyModel> getOutputSchema() {
    return outputSchema;
  }

  public void setOutputSchema(List<@Valid PropertyModel> outputSchema) {
    this.outputSchema = outputSchema;
  }

  public ActionDefinitionModel properties(List<@Valid PropertyModel> properties) {
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
  @JsonProperty("properties")
  public List<@Valid PropertyModel> getProperties() {
    return properties;
  }

  public void setProperties(List<@Valid PropertyModel> properties) {
    this.properties = properties;
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
        Objects.equals(this.outputSchema, actionDefinition.outputSchema) &&
        Objects.equals(this.properties, actionDefinition.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(display, exampleOutput, name, outputSchema, properties);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ActionDefinitionModel {\n");
    sb.append("    display: ").append(toIndentedString(display)).append("\n");
    sb.append("    exampleOutput: ").append(toIndentedString(exampleOutput)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    outputSchema: ").append(toIndentedString(outputSchema)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
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

