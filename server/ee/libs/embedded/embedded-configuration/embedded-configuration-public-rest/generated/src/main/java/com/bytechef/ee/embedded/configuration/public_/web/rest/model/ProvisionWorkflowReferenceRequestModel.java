package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Connections the connected user chose, keyed by component name. Components not listed keep their current connection or are auto-matched.
 */

@Schema(name = "ProvisionWorkflowReferenceRequest", description = "Connections the connected user chose, keyed by component name. Components not listed keep their current connection or are auto-matched.")
@JsonTypeName("ProvisionWorkflowReferenceRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-23T21:34:31.743535+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class ProvisionWorkflowReferenceRequestModel {

  @Valid
  private Map<String, Long> connections = new HashMap<>();

  @Valid
  private Map<String, Object> inputs = new HashMap<>();

  public ProvisionWorkflowReferenceRequestModel connections(Map<String, Long> connections) {
    this.connections = connections;
    return this;
  }

  public ProvisionWorkflowReferenceRequestModel putConnectionsItem(String key, Long connectionsItem) {
    if (this.connections == null) {
      this.connections = new HashMap<>();
    }
    this.connections.put(key, connectionsItem);
    return this;
  }

  /**
   * Get connections
   * @return connections
   */
  
  @Schema(name = "connections", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connections")
  public Map<String, Long> getConnections() {
    return connections;
  }

  @JsonProperty("connections")
  public void setConnections(Map<String, Long> connections) {
    this.connections = connections;
  }

  public ProvisionWorkflowReferenceRequestModel inputs(Map<String, Object> inputs) {
    this.inputs = inputs;
    return this;
  }

  public ProvisionWorkflowReferenceRequestModel putInputsItem(String key, Object inputsItem) {
    if (this.inputs == null) {
      this.inputs = new HashMap<>();
    }
    this.inputs.put(key, inputsItem);
    return this;
  }

  /**
   * Input values for the workflow's inputs, keyed by input name. When given, they replace the reference's stored input values before it is enabled, so a workflow with required inputs is enabled in the same call.
   * @return inputs
   */
  
  @Schema(name = "inputs", description = "Input values for the workflow's inputs, keyed by input name. When given, they replace the reference's stored input values before it is enabled, so a workflow with required inputs is enabled in the same call.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
  public Map<String, Object> getInputs() {
    return inputs;
  }

  @JsonProperty("inputs")
  public void setInputs(Map<String, Object> inputs) {
    this.inputs = inputs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProvisionWorkflowReferenceRequestModel provisionWorkflowReferenceRequest = (ProvisionWorkflowReferenceRequestModel) o;
    return Objects.equals(this.connections, provisionWorkflowReferenceRequest.connections) &&
        Objects.equals(this.inputs, provisionWorkflowReferenceRequest.inputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connections, inputs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProvisionWorkflowReferenceRequestModel {\n");
    sb.append("    connections: ").append(toIndentedString(connections)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

