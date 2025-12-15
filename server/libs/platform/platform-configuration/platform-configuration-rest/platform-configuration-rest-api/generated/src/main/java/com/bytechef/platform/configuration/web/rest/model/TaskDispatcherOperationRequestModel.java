package com.bytechef.platform.configuration.web.rest.model;

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
 * TaskDispatcherOperationRequestModel
 */

@JsonTypeName("TaskDispatcherOperationRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-15T09:52:48.574632+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class TaskDispatcherOperationRequestModel {

  @Valid
  private Map<String, Object> parameters = new HashMap<>();

  public TaskDispatcherOperationRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TaskDispatcherOperationRequestModel(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  public TaskDispatcherOperationRequestModel parameters(Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  public TaskDispatcherOperationRequestModel putParametersItem(String key, Object parametersItem) {
    if (this.parameters == null) {
      this.parameters = new HashMap<>();
    }
    this.parameters.put(key, parametersItem);
    return this;
  }

  /**
   * The parameters of an action.
   * @return parameters
   */
  @NotNull 
  @Schema(name = "parameters", description = "The parameters of an action.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("parameters")
  public Map<String, Object> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TaskDispatcherOperationRequestModel taskDispatcherOperationRequest = (TaskDispatcherOperationRequestModel) o;
    return Objects.equals(this.parameters, taskDispatcherOperationRequest.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TaskDispatcherOperationRequestModel {\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
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

