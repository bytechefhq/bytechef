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
 * A workflow data for particular integration instance.
 */

@Schema(name = "IntegrationInstanceWorkflow", description = "A workflow data for particular integration instance.")
@JsonTypeName("IntegrationInstanceWorkflow")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-02T08:44:56.374523+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class IntegrationInstanceWorkflowModel {

  private @Nullable Boolean enabled;

  @Valid
  private Map<String, Object> inputs = new HashMap<>();

  private @Nullable String workflowUuid;

  public IntegrationInstanceWorkflowModel enabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If a workflow is enabled or not.
   * @return enabled
   */
  
  @Schema(name = "enabled", description = "If a workflow is enabled or not.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public @Nullable Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
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

  public IntegrationInstanceWorkflowModel workflowUuid(@Nullable String workflowUuid) {
    this.workflowUuid = workflowUuid;
    return this;
  }

  /**
   * The reference code of a workflow.
   * @return workflowUuid
   */
  
  @Schema(name = "workflowUuid", description = "The reference code of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowUuid")
  public @Nullable String getWorkflowUuid() {
    return workflowUuid;
  }

  public void setWorkflowUuid(@Nullable String workflowUuid) {
    this.workflowUuid = workflowUuid;
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
    return Objects.equals(this.enabled, integrationInstanceWorkflow.enabled) &&
        Objects.equals(this.inputs, integrationInstanceWorkflow.inputs) &&
        Objects.equals(this.workflowUuid, integrationInstanceWorkflow.workflowUuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, inputs, workflowUuid);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationInstanceWorkflowModel {\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    workflowUuid: ").append(toIndentedString(workflowUuid)).append("\n");
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

