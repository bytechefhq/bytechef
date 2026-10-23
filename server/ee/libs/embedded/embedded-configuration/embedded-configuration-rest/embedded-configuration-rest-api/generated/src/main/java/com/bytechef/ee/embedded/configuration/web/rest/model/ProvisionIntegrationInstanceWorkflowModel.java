package com.bytechef.ee.embedded.configuration.web.rest.model;

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
 * A workflow to enable on a provisioned integration instance.
 */

@Schema(name = "ProvisionIntegrationInstanceWorkflow", description = "A workflow to enable on a provisioned integration instance.")
@JsonTypeName("ProvisionIntegrationInstanceWorkflow")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-23T19:46:00.733909+02:00[Europe/Zagreb]", comments = "Generator version: 7.25.0")
public class ProvisionIntegrationInstanceWorkflowModel {

  private String workflowUuid;

  private Map<String, Object> inputs = new HashMap<>();

  public ProvisionIntegrationInstanceWorkflowModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProvisionIntegrationInstanceWorkflowModel(String workflowUuid) {
    this.workflowUuid = workflowUuid;
  }

  public ProvisionIntegrationInstanceWorkflowModel workflowUuid(String workflowUuid) {
    this.workflowUuid = workflowUuid;
    return this;
  }

  /**
   * The uuid of the workflow.
   * @return workflowUuid
   */
  @NotNull 
  @Schema(name = "workflowUuid", description = "The uuid of the workflow.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workflowUuid")
  public String getWorkflowUuid() {
    return workflowUuid;
  }

  @JsonProperty("workflowUuid")
  public void setWorkflowUuid(String workflowUuid) {
    this.workflowUuid = workflowUuid;
  }

  public ProvisionIntegrationInstanceWorkflowModel inputs(Map<String, Object> inputs) {
    this.inputs = inputs;
    return this;
  }

  public ProvisionIntegrationInstanceWorkflowModel putInputsItem(String key, Object inputsItem) {
    if (this.inputs == null) {
      this.inputs = new HashMap<>();
    }
    this.inputs.put(key, inputsItem);
    return this;
  }

  /**
   * The inputs of the workflow.
   * @return inputs
   */
  
  @Schema(name = "inputs", description = "The inputs of the workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    ProvisionIntegrationInstanceWorkflowModel provisionIntegrationInstanceWorkflow = (ProvisionIntegrationInstanceWorkflowModel) o;
    return Objects.equals(this.workflowUuid, provisionIntegrationInstanceWorkflow.workflowUuid) &&
        Objects.equals(this.inputs, provisionIntegrationInstanceWorkflow.inputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workflowUuid, inputs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProvisionIntegrationInstanceWorkflowModel {\n");
    sb.append("    workflowUuid: ").append(toIndentedString(workflowUuid)).append("\n");
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

