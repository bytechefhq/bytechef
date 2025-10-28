package com.bytechef.ee.embedded.execution.public_.web.rest.model;

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
 * ExecuteActionRequestModel
 */

@JsonTypeName("executeAction_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-07T14:49:44.122259+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public class ExecuteActionRequestModel {

  @Valid
  private Map<String, Object> input = new HashMap<>();

  public ExecuteActionRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ExecuteActionRequestModel(Map<String, Object> input) {
    this.input = input;
  }

  public ExecuteActionRequestModel input(Map<String, Object> input) {
    this.input = input;
    return this;
  }

  public ExecuteActionRequestModel putInputItem(String key, Object inputItem) {
    if (this.input == null) {
      this.input = new HashMap<>();
    }
    this.input.put(key, inputItem);
    return this;
  }

  /**
   * The input parameters for the action.
   * @return input
   */
  @NotNull 
  @Schema(name = "input", description = "The input parameters for the action.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("input")
  public Map<String, Object> getInput() {
    return input;
  }

  public void setInput(Map<String, Object> input) {
    this.input = input;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExecuteActionRequestModel executeActionRequest = (ExecuteActionRequestModel) o;
    return Objects.equals(this.input, executeActionRequest.input);
  }

  @Override
  public int hashCode() {
    return Objects.hash(input);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExecuteActionRequestModel {\n");
    sb.append("    input: ").append(toIndentedString(input)).append("\n");
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

