package com.bytechef.helios.execution.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.helios.execution.web.rest.model.TaskConnectionModel;
import com.bytechef.helios.execution.web.rest.model.TriggerOutputModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Defines parameters used to test a workflow.
 */

@Schema(name = "TestParameters", description = "Defines parameters used to test a workflow.")
@JsonTypeName("TestParameters")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-12-04T18:50:03.122916+01:00[Europe/Zagreb]")
public class TestParametersModel {

  @Valid
  private List<@Valid TaskConnectionModel> connections;

  @Valid
  private Map<String, Object> inputs = new HashMap<>();

  @Valid
  private List<@Valid TriggerOutputModel> triggerOutputs;

  private String workflowId;

  public TestParametersModel connections(List<@Valid TaskConnectionModel> connections) {
    this.connections = connections;
    return this;
  }

  public TestParametersModel addConnectionsItem(TaskConnectionModel connectionsItem) {
    if (this.connections == null) {
      this.connections = new ArrayList<>();
    }
    this.connections.add(connectionsItem);
    return this;
  }

  /**
   * Get connections
   * @return connections
  */
  @Valid 
  @Schema(name = "connections", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connections")
  public List<@Valid TaskConnectionModel> getConnections() {
    return connections;
  }

  public void setConnections(List<@Valid TaskConnectionModel> connections) {
    this.connections = connections;
  }

  public TestParametersModel inputs(Map<String, Object> inputs) {
    this.inputs = inputs;
    return this;
  }

  public TestParametersModel putInputsItem(String key, Object inputsItem) {
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
  @JsonProperty("inputs")
  public Map<String, Object> getInputs() {
    return inputs;
  }

  public void setInputs(Map<String, Object> inputs) {
    this.inputs = inputs;
  }

  public TestParametersModel triggerOutputs(List<@Valid TriggerOutputModel> triggerOutputs) {
    this.triggerOutputs = triggerOutputs;
    return this;
  }

  public TestParametersModel addTriggerOutputsItem(TriggerOutputModel triggerOutputsItem) {
    if (this.triggerOutputs == null) {
      this.triggerOutputs = new ArrayList<>();
    }
    this.triggerOutputs.add(triggerOutputsItem);
    return this;
  }

  /**
   * Get triggerOutputs
   * @return triggerOutputs
  */
  @Valid 
  @Schema(name = "triggerOutputs", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("triggerOutputs")
  public List<@Valid TriggerOutputModel> getTriggerOutputs() {
    return triggerOutputs;
  }

  public void setTriggerOutputs(List<@Valid TriggerOutputModel> triggerOutputs) {
    this.triggerOutputs = triggerOutputs;
  }

  public TestParametersModel workflowId(String workflowId) {
    this.workflowId = workflowId;
    return this;
  }

  /**
   * Id of the workflow to execute.
   * @return workflowId
  */
  
  @Schema(name = "workflowId", description = "Id of the workflow to execute.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowId")
  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TestParametersModel testParameters = (TestParametersModel) o;
    return Objects.equals(this.connections, testParameters.connections) &&
        Objects.equals(this.inputs, testParameters.inputs) &&
        Objects.equals(this.triggerOutputs, testParameters.triggerOutputs) &&
        Objects.equals(this.workflowId, testParameters.workflowId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connections, inputs, triggerOutputs, workflowId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TestParametersModel {\n");
    sb.append("    connections: ").append(toIndentedString(connections)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    triggerOutputs: ").append(toIndentedString(triggerOutputs)).append("\n");
    sb.append("    workflowId: ").append(toIndentedString(workflowId)).append("\n");
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

