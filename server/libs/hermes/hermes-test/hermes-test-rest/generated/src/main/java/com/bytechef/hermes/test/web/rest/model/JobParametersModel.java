package com.bytechef.hermes.test.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.test.web.rest.model.JobConnectionModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
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
 * Defines parameters used to execute a job.
 */

@Schema(name = "JobParameters", description = "Defines parameters used to execute a job.")
@JsonTypeName("JobParameters")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-07-21T07:36:04.025028+02:00[Europe/Zagreb]")
public class JobParametersModel {

  @Valid
  private List<@Valid JobConnectionModel> connections;

  @Valid
  private Map<String, Object> inputs = new HashMap<>();

  private String label;

  private String workflowId;

  /**
   * Default constructor
   * @deprecated Use {@link JobParametersModel#JobParametersModel(String)}
   */
  @Deprecated
  public JobParametersModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public JobParametersModel(String workflowId) {
    this.workflowId = workflowId;
  }

  public JobParametersModel connections(List<@Valid JobConnectionModel> connections) {
    this.connections = connections;
    return this;
  }

  public JobParametersModel addConnectionsItem(JobConnectionModel connectionsItem) {
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
  public List<@Valid JobConnectionModel> getConnections() {
    return connections;
  }

  public void setConnections(List<@Valid JobConnectionModel> connections) {
    this.connections = connections;
  }

  public JobParametersModel inputs(Map<String, Object> inputs) {
    this.inputs = inputs;
    return this;
  }

  public JobParametersModel putInputsItem(String key, Object inputsItem) {
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

  public JobParametersModel label(String label) {
    this.label = label;
    return this;
  }

  /**
   * The job's human-readable name
   * @return label
  */
  
  @Schema(name = "label", description = "The job's human-readable name", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public JobParametersModel workflowId(String workflowId) {
    this.workflowId = workflowId;
    return this;
  }

  /**
   * Id of the workflow to execute.
   * @return workflowId
  */
  @NotNull 
  @Schema(name = "workflowId", description = "Id of the workflow to execute.", requiredMode = Schema.RequiredMode.REQUIRED)
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
    JobParametersModel jobParameters = (JobParametersModel) o;
    return Objects.equals(this.connections, jobParameters.connections) &&
        Objects.equals(this.inputs, jobParameters.inputs) &&
        Objects.equals(this.label, jobParameters.label) &&
        Objects.equals(this.workflowId, jobParameters.workflowId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connections, inputs, label, workflowId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JobParametersModel {\n");
    sb.append("    connections: ").append(toIndentedString(connections)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
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

