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
 * UpdateFrontendIntegrationInstanceWorkflowRequestModel
 */

@JsonTypeName("updateFrontendIntegrationInstanceWorkflow_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-07T14:49:43.557695+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public class UpdateFrontendIntegrationInstanceWorkflowRequestModel {

  @Valid
  private Map<String, Object> inputs = new HashMap<>();

  public UpdateFrontendIntegrationInstanceWorkflowRequestModel inputs(Map<String, Object> inputs) {
    this.inputs = inputs;
    return this;
  }

  public UpdateFrontendIntegrationInstanceWorkflowRequestModel putInputsItem(String key, Object inputsItem) {
    if (this.inputs == null) {
      this.inputs = new HashMap<>();
    }
    this.inputs.put(key, inputsItem);
    return this;
  }

  /**
   * The workflow inputs
   * @return inputs
   */
  
  @Schema(name = "inputs", description = "The workflow inputs", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
  public Map<String, Object> getInputs() {
    return inputs;
  }

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
    UpdateFrontendIntegrationInstanceWorkflowRequestModel updateFrontendIntegrationInstanceWorkflowRequest = (UpdateFrontendIntegrationInstanceWorkflowRequestModel) o;
    return Objects.equals(this.inputs, updateFrontendIntegrationInstanceWorkflowRequest.inputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(inputs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateFrontendIntegrationInstanceWorkflowRequestModel {\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
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

