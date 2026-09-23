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
 * The input values a connected user supplied for a workflow.
 */

@Schema(name = "UpdateWorkflowInputsRequest", description = "The input values a connected user supplied for a workflow.")
@JsonTypeName("UpdateWorkflowInputsRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-23T21:34:31.743535+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class UpdateWorkflowInputsRequestModel {

  @Valid
  private Map<String, Object> inputs = new HashMap<>();

  public UpdateWorkflowInputsRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UpdateWorkflowInputsRequestModel(Map<String, Object> inputs) {
    this.inputs = inputs;
  }

  public UpdateWorkflowInputsRequestModel inputs(Map<String, Object> inputs) {
    this.inputs = inputs;
    return this;
  }

  public UpdateWorkflowInputsRequestModel putInputsItem(String key, Object inputsItem) {
    if (this.inputs == null) {
      this.inputs = new HashMap<>();
    }
    this.inputs.put(key, inputsItem);
    return this;
  }

  /**
   * Input values keyed by input name. An absent key clears that input.
   * @return inputs
   */
  @NotNull 
  @Schema(name = "inputs", description = "Input values keyed by input name. An absent key clears that input.", requiredMode = Schema.RequiredMode.REQUIRED)
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
    UpdateWorkflowInputsRequestModel updateWorkflowInputsRequest = (UpdateWorkflowInputsRequestModel) o;
    return Objects.equals(this.inputs, updateWorkflowInputsRequest.inputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(inputs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateWorkflowInputsRequestModel {\n");
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

