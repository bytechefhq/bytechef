package com.bytechef.ee.embedded.execution.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A function property type.
 */

@Schema(name = "Function", description = "A function property type.")
@JsonTypeName("Function")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-23T12:36:59.390850+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class FunctionModel {

  private String name;

  private String description;

  private String parameters;

  public FunctionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public FunctionModel(String name, String description, String parameters) {
    this.name = name;
    this.description = description;
    this.parameters = parameters;
  }

  public FunctionModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The function name.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The function name.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public FunctionModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The function description.
   * @return description
   */
  @NotNull 
  @Schema(name = "description", description = "The function description.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public FunctionModel parameters(String parameters) {
    this.parameters = parameters;
    return this;
  }

  /**
   * JSON Schema for the function parameters.
   * @return parameters
   */
  @NotNull 
  @Schema(name = "parameters", description = "JSON Schema for the function parameters.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("parameters")
  public String getParameters() {
    return parameters;
  }

  public void setParameters(String parameters) {
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
    FunctionModel function = (FunctionModel) o;
    return Objects.equals(this.name, function.name) &&
        Objects.equals(this.description, function.description) &&
        Objects.equals(this.parameters, function.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, parameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FunctionModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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

