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
 * IntegrationInstanceAllOfWorkflowsModel
 */

@JsonTypeName("IntegrationInstance_allOf_workflows")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-06-24T21:06:35.139368+02:00[Europe/Zagreb]", comments = "Generator version: 7.13.0")
public class IntegrationInstanceAllOfWorkflowsModel {

  private @Nullable Boolean enabled;

  @Valid
  private Map<String, Object> inputs = new HashMap<>();

  private @Nullable String workflowReferenceCode;

  public IntegrationInstanceAllOfWorkflowsModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If a workflow is enabled or not.
   * @return enabled
   */
  
  @Schema(name = "enabled", description = "If a workflow is enabled or not.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public IntegrationInstanceAllOfWorkflowsModel inputs(Map<String, Object> inputs) {
    this.inputs = inputs;
    return this;
  }

  public IntegrationInstanceAllOfWorkflowsModel putInputsItem(String key, Object inputsItem) {
    if (this.inputs == null) {
      this.inputs = new HashMap<>();
    }
    this.inputs.put(key, inputsItem);
    return this;
  }

  /**
   * Get inputs
   * @return inputs
   */
  
  @Schema(name = "inputs", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
  public Map<String, Object> getInputs() {
    return inputs;
  }

  public void setInputs(Map<String, Object> inputs) {
    this.inputs = inputs;
  }

  public IntegrationInstanceAllOfWorkflowsModel workflowReferenceCode(String workflowReferenceCode) {
    this.workflowReferenceCode = workflowReferenceCode;
    return this;
  }

  /**
   * The reference code of a workflow.
   * @return workflowReferenceCode
   */
  
  @Schema(name = "workflowReferenceCode", description = "The reference code of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowReferenceCode")
  public String getWorkflowReferenceCode() {
    return workflowReferenceCode;
  }

  public void setWorkflowReferenceCode(String workflowReferenceCode) {
    this.workflowReferenceCode = workflowReferenceCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IntegrationInstanceAllOfWorkflowsModel integrationInstanceAllOfWorkflows = (IntegrationInstanceAllOfWorkflowsModel) o;
    return Objects.equals(this.enabled, integrationInstanceAllOfWorkflows.enabled) &&
        Objects.equals(this.inputs, integrationInstanceAllOfWorkflows.inputs) &&
        Objects.equals(this.workflowReferenceCode, integrationInstanceAllOfWorkflows.workflowReferenceCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, inputs, workflowReferenceCode);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationInstanceAllOfWorkflowsModel {\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    workflowReferenceCode: ").append(toIndentedString(workflowReferenceCode)).append("\n");
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

