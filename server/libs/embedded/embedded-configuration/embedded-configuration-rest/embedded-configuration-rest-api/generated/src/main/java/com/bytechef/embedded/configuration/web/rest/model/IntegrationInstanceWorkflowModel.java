package com.bytechef.embedded.configuration.web.rest.model;

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
 * Contains user configurations for the execution of a particular integration workflow.
 */

@Schema(name = "IntegrationInstanceWorkflow", description = "Contains user configurations for the execution of a particular integration workflow.")
@JsonTypeName("IntegrationInstanceWorkflow")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-08-26T07:29:40.820234+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public class IntegrationInstanceWorkflowModel {

  @Valid
  private Map<String, Object> inputs = new HashMap<>();

  private Boolean enabled;

  private Long integrationInstanceConfigurationWorkflowId;

  private String workflowId;

  public IntegrationInstanceWorkflowModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public IntegrationInstanceWorkflowModel(Boolean enabled, Long integrationInstanceConfigurationWorkflowId, String workflowId) {
    this.enabled = enabled;
    this.integrationInstanceConfigurationWorkflowId = integrationInstanceConfigurationWorkflowId;
    this.workflowId = workflowId;
  }

  public IntegrationInstanceWorkflowModel inputs(Map<String, Object> inputs) {
    this.inputs = inputs;
    return this;
  }

  public IntegrationInstanceWorkflowModel putInputsItem(String key, Object inputsItem) {
    if (this.inputs == null) {
      this.inputs = new HashMap<>();
    }
    this.inputs.put(key, inputsItem);
    return this;
  }

  /**
   * The input parameters of an integration instance used as workflow input values.
   * @return inputs
  */
  
  @Schema(name = "inputs", description = "The input parameters of an integration instance used as workflow input values.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
  public Map<String, Object> getInputs() {
    return inputs;
  }

  public void setInputs(Map<String, Object> inputs) {
    this.inputs = inputs;
  }

  public IntegrationInstanceWorkflowModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If a workflow is enabled or not in the integration instance workflow.
   * @return enabled
  */
  @NotNull 
  @Schema(name = "enabled", description = "If a workflow is enabled or not in the integration instance workflow.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public IntegrationInstanceWorkflowModel integrationInstanceConfigurationWorkflowId(Long integrationInstanceConfigurationWorkflowId) {
    this.integrationInstanceConfigurationWorkflowId = integrationInstanceConfigurationWorkflowId;
    return this;
  }

  /**
   * The id of a integration instance configuration workflow.
   * @return integrationInstanceConfigurationWorkflowId
  */
  @NotNull 
  @Schema(name = "integrationInstanceConfigurationWorkflowId", description = "The id of a integration instance configuration workflow.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("integrationInstanceConfigurationWorkflowId")
  public Long getIntegrationInstanceConfigurationWorkflowId() {
    return integrationInstanceConfigurationWorkflowId;
  }

  public void setIntegrationInstanceConfigurationWorkflowId(Long integrationInstanceConfigurationWorkflowId) {
    this.integrationInstanceConfigurationWorkflowId = integrationInstanceConfigurationWorkflowId;
  }

  public IntegrationInstanceWorkflowModel workflowId(String workflowId) {
    this.workflowId = workflowId;
    return this;
  }

  /**
   * The id of a workflow.
   * @return workflowId
  */
  @NotNull 
  @Schema(name = "workflowId", description = "The id of a workflow.", requiredMode = Schema.RequiredMode.REQUIRED)
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
    IntegrationInstanceWorkflowModel integrationInstanceWorkflow = (IntegrationInstanceWorkflowModel) o;
    return Objects.equals(this.inputs, integrationInstanceWorkflow.inputs) &&
        Objects.equals(this.enabled, integrationInstanceWorkflow.enabled) &&
        Objects.equals(this.integrationInstanceConfigurationWorkflowId, integrationInstanceWorkflow.integrationInstanceConfigurationWorkflowId) &&
        Objects.equals(this.workflowId, integrationInstanceWorkflow.workflowId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(inputs, enabled, integrationInstanceConfigurationWorkflowId, workflowId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationInstanceWorkflowModel {\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    integrationInstanceConfigurationWorkflowId: ").append(toIndentedString(integrationInstanceConfigurationWorkflowId)).append("\n");
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

