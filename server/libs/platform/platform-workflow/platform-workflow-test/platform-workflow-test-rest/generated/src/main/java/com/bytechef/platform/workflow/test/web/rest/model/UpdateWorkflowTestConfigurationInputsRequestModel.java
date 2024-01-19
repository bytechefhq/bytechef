package com.bytechef.platform.workflow.test.web.rest.model;

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
 * UpdateWorkflowTestConfigurationInputsRequestModel
 */

@JsonTypeName("updateWorkflowTestConfigurationInputs_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-01-20T08:48:13.801332+01:00[Europe/Zagreb]")
public class UpdateWorkflowTestConfigurationInputsRequestModel {

  @Valid
  private Map<String, String> inputs = new HashMap<>();

  public UpdateWorkflowTestConfigurationInputsRequestModel inputs(Map<String, String> inputs) {
    this.inputs = inputs;
    return this;
  }

  public UpdateWorkflowTestConfigurationInputsRequestModel putInputsItem(String key, String inputsItem) {
    if (this.inputs == null) {
      this.inputs = new HashMap<>();
    }
    this.inputs.put(key, inputsItem);
    return this;
  }

  /**
   * The input parameters used as workflow input values.
   * @return inputs
  */
  
  @Schema(name = "inputs", description = "The input parameters used as workflow input values.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
  public Map<String, String> getInputs() {
    return inputs;
  }

  public void setInputs(Map<String, String> inputs) {
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
    UpdateWorkflowTestConfigurationInputsRequestModel updateWorkflowTestConfigurationInputsRequest = (UpdateWorkflowTestConfigurationInputsRequestModel) o;
    return Objects.equals(this.inputs, updateWorkflowTestConfigurationInputsRequest.inputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(inputs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateWorkflowTestConfigurationInputsRequestModel {\n");
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

