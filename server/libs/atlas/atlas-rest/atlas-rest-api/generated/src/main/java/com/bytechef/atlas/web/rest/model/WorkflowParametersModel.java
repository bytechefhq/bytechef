package com.bytechef.atlas.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Defines parameters used to execute a workflow.
 */

@Schema(name = "WorkflowParameters", description = "Defines parameters used to execute a workflow.")
@JsonTypeName("WorkflowParameters")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-29T08:44:25.432901+02:00[Europe/Zagreb]")
public class WorkflowParametersModel {

  @JsonProperty("inputs")
  @Valid
  private Map<String, Object> inputs = null;

  @JsonProperty("outputs")
  @Valid
  private Map<String, Object> outputs = null;

  public WorkflowParametersModel inputs(Map<String, Object> inputs) {
    this.inputs = inputs;
    return this;
  }

  public WorkflowParametersModel putInputsItem(String key, Object inputsItem) {
    if (this.inputs == null) {
      this.inputs = new HashMap<>();
    }
    this.inputs.put(key, inputsItem);
    return this;
  }

  /**
   * The inputs expected by the workflow
   * @return inputs
  */
  
  @Schema(name = "inputs", description = "The inputs expected by the workflow", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Map<String, Object> getInputs() {
    return inputs;
  }

  public void setInputs(Map<String, Object> inputs) {
    this.inputs = inputs;
  }

  public WorkflowParametersModel outputs(Map<String, Object> outputs) {
    this.outputs = outputs;
    return this;
  }

  public WorkflowParametersModel putOutputsItem(String key, Object outputsItem) {
    if (this.outputs == null) {
      this.outputs = new HashMap<>();
    }
    this.outputs.put(key, outputsItem);
    return this;
  }

  /**
   * The outputs expected by the workflow.
   * @return outputs
  */
  
  @Schema(name = "outputs", description = "The outputs expected by the workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Map<String, Object> getOutputs() {
    return outputs;
  }

  public void setOutputs(Map<String, Object> outputs) {
    this.outputs = outputs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkflowParametersModel workflowParameters = (WorkflowParametersModel) o;
    return Objects.equals(this.inputs, workflowParameters.inputs) &&
        Objects.equals(this.outputs, workflowParameters.outputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(inputs, outputs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowParametersModel {\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    outputs: ").append(toIndentedString(outputs)).append("\n");
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

