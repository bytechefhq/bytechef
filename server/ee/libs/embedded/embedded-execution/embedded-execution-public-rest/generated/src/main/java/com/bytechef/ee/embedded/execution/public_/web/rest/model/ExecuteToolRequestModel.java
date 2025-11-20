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
 * ExecuteToolRequestModel
 */

@JsonTypeName("executeTool_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:31.978624+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class ExecuteToolRequestModel {

  private String name;

  @Valid
  private Map<String, Object> parameters = new HashMap<>();

  public ExecuteToolRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ExecuteToolRequestModel(String name, Map<String, Object> parameters) {
    this.name = name;
    this.parameters = parameters;
  }

  public ExecuteToolRequestModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the tool to call.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The name of the tool to call.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ExecuteToolRequestModel parameters(Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  public ExecuteToolRequestModel putParametersItem(String key, Object parametersItem) {
    if (this.parameters == null) {
      this.parameters = new HashMap<>();
    }
    this.parameters.put(key, parametersItem);
    return this;
  }

  /**
   * The input parameters for the tool.
   * @return parameters
   */
  @NotNull 
  @Schema(name = "parameters", description = "The input parameters for the tool.", requiredMode = Schema.RequiredMode.REQUIRED)
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
    ExecuteToolRequestModel executeToolRequest = (ExecuteToolRequestModel) o;
    return Objects.equals(this.name, executeToolRequest.name) &&
        Objects.equals(this.parameters, executeToolRequest.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, parameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExecuteToolRequestModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

