package com.bytechef.ee.embedded.execution.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.FunctionModel;
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
 * ToolModel
 */

@JsonTypeName("Tool")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:31.978624+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class ToolModel {

  private FunctionModel function;

  private String type = "function";

  public ToolModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ToolModel(FunctionModel function, String type) {
    this.function = function;
    this.type = type;
  }

  public ToolModel function(FunctionModel function) {
    this.function = function;
    return this;
  }

  /**
   * Get function
   * @return function
   */
  @NotNull @Valid 
  @Schema(name = "function", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("function")
  public FunctionModel getFunction() {
    return function;
  }

  public void setFunction(FunctionModel function) {
    this.function = function;
  }

  public ToolModel type(String type) {
    this.type = type;
    return this;
  }

  /**
   * The type of the tool
   * @return type
   */
  @NotNull 
  @Schema(name = "type", description = "The type of the tool", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ToolModel tool = (ToolModel) o;
    return Objects.equals(this.function, tool.function) &&
        Objects.equals(this.type, tool.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(function, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ToolModel {\n");
    sb.append("    function: ").append(toIndentedString(function)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
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

