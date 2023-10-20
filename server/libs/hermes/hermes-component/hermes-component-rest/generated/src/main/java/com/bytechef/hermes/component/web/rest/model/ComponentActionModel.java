package com.bytechef.hermes.component.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.component.web.rest.model.ComponentActionInputsInnerModel;
import com.bytechef.hermes.component.web.rest.model.DisplayModel;
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
 * ComponentActionModel
 */

@JsonTypeName("ComponentAction")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-27T16:22:37.776273+02:00[Europe/Zagreb]")
public class ComponentActionModel {

  @JsonProperty("display")
  private DisplayModel display;

  @JsonProperty("name")
  private String name;

  @JsonProperty("inputs")
  @Valid
  private List<ComponentActionInputsInnerModel> inputs = null;

  @JsonProperty("exampleOutput")
  private Object exampleOutput;

  @JsonProperty("outputSchema")
  @Valid
  private List<ComponentActionInputsInnerModel> outputSchema = null;

  public ComponentActionModel display(DisplayModel display) {
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

  public ComponentActionModel name(String name) {
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

  public ComponentActionModel inputs(List<ComponentActionInputsInnerModel> inputs) {
    this.inputs = inputs;
    return this;
  }

  public ComponentActionModel addInputsItem(ComponentActionInputsInnerModel inputsItem) {
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
  public List<ComponentActionInputsInnerModel> getInputs() {
    return inputs;
  }

  public void setInputs(List<ComponentActionInputsInnerModel> inputs) {
    this.inputs = inputs;
  }

  public ComponentActionModel exampleOutput(Object exampleOutput) {
    this.exampleOutput = exampleOutput;
    return this;
  }

  /**
   * Get exampleOutput
   * @return exampleOutput
  */
  
  @Schema(name = "exampleOutput", required = false)
  public Object getExampleOutput() {
    return exampleOutput;
  }

  public void setExampleOutput(Object exampleOutput) {
    this.exampleOutput = exampleOutput;
  }

  public ComponentActionModel outputSchema(List<ComponentActionInputsInnerModel> outputSchema) {
    this.outputSchema = outputSchema;
    return this;
  }

  public ComponentActionModel addOutputSchemaItem(ComponentActionInputsInnerModel outputSchemaItem) {
    if (this.outputSchema == null) {
      this.outputSchema = new ArrayList<>();
    }
    this.outputSchema.add(outputSchemaItem);
    return this;
  }

  /**
   * Get outputSchema
   * @return outputSchema
  */
  @Valid 
  @Schema(name = "outputSchema", required = false)
  public List<ComponentActionInputsInnerModel> getOutputSchema() {
    return outputSchema;
  }

  public void setOutputSchema(List<ComponentActionInputsInnerModel> outputSchema) {
    this.outputSchema = outputSchema;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComponentActionModel componentAction = (ComponentActionModel) o;
    return Objects.equals(this.display, componentAction.display) &&
        Objects.equals(this.name, componentAction.name) &&
        Objects.equals(this.inputs, componentAction.inputs) &&
        Objects.equals(this.exampleOutput, componentAction.exampleOutput) &&
        Objects.equals(this.outputSchema, componentAction.outputSchema);
  }

  @Override
  public int hashCode() {
    return Objects.hash(display, name, inputs, exampleOutput, outputSchema);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComponentActionModel {\n");
    sb.append("    display: ").append(toIndentedString(display)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    exampleOutput: ").append(toIndentedString(exampleOutput)).append("\n");
    sb.append("    outputSchema: ").append(toIndentedString(outputSchema)).append("\n");
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

