package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.WorkflowConnectionModel;
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
 * Represents a definition of a workflow trigger.
 */

@Schema(name = "WorkflowTrigger", description = "Represents a definition of a workflow trigger.")
@JsonTypeName("WorkflowTrigger")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-03-29T14:33:24.959619+01:00[Europe/Zagreb]", comments = "Generator version: 7.4.0")
public class WorkflowTriggerModel {

  @Valid
  private List<@Valid WorkflowConnectionModel> connections;

  private String label;

  private String name;

  @Valid
  private Map<String, Object> parameters = new HashMap<>();

  private String timeout;

  private String type;

  public WorkflowTriggerModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WorkflowTriggerModel(String name, String type) {
    this.name = name;
    this.type = type;
  }

  public WorkflowTriggerModel connections(List<@Valid WorkflowConnectionModel> connections) {
    this.connections = connections;
    return this;
  }

  public WorkflowTriggerModel addConnectionsItem(WorkflowConnectionModel connectionsItem) {
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
  @Schema(name = "connections", accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connections")
  public List<@Valid WorkflowConnectionModel> getConnections() {
    return connections;
  }

  public void setConnections(List<@Valid WorkflowConnectionModel> connections) {
    this.connections = connections;
  }

  public WorkflowTriggerModel label(String label) {
    this.label = label;
    return this;
  }

  /**
   * The human-readable description of the task.
   * @return label
  */
  
  @Schema(name = "label", description = "The human-readable description of the task.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public WorkflowTriggerModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The identifier name of the task. Task names are used for assigning the output of one task so it can be later used by subsequent tasks.
   * @return name
  */
  @NotNull 
  @Schema(name = "name", description = "The identifier name of the task. Task names are used for assigning the output of one task so it can be later used by subsequent tasks.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public WorkflowTriggerModel parameters(Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  public WorkflowTriggerModel putParametersItem(String key, Object parametersItem) {
    if (this.parameters == null) {
      this.parameters = new HashMap<>();
    }
    this.parameters.put(key, parametersItem);
    return this;
  }

  /**
   * Key-value map of task parameters.
   * @return parameters
  */
  
  @Schema(name = "parameters", description = "Key-value map of task parameters.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("parameters")
  public Map<String, Object> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  public WorkflowTriggerModel timeout(String timeout) {
    this.timeout = timeout;
    return this;
  }

  /**
   * The timeout expression which describes when a trigger should be deemed as timed-out.
   * @return timeout
  */
  
  @Schema(name = "timeout", description = "The timeout expression which describes when a trigger should be deemed as timed-out.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("timeout")
  public String getTimeout() {
    return timeout;
  }

  public void setTimeout(String timeout) {
    this.timeout = timeout;
  }

  public WorkflowTriggerModel type(String type) {
    this.type = type;
    return this;
  }

  /**
   * The type of a trigger.
   * @return type
  */
  @NotNull 
  @Schema(name = "type", description = "The type of a trigger.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkflowTriggerModel workflowTrigger = (WorkflowTriggerModel) o;
    return Objects.equals(this.connections, workflowTrigger.connections) &&
        Objects.equals(this.label, workflowTrigger.label) &&
        Objects.equals(this.name, workflowTrigger.name) &&
        Objects.equals(this.parameters, workflowTrigger.parameters) &&
        Objects.equals(this.timeout, workflowTrigger.timeout) &&
        Objects.equals(this.type, workflowTrigger.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connections, label, name, parameters, timeout, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowTriggerModel {\n");
    sb.append("    connections: ").append(toIndentedString(connections)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
    sb.append("    timeout: ").append(toIndentedString(timeout)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
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

