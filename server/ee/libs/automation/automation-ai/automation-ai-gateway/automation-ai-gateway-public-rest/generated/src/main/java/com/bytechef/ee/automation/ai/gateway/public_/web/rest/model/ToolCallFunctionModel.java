package com.bytechef.ee.automation.ai.gateway.public_.web.rest.model;

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
 * The function the model called.
 */

@Schema(name = "ToolCall_function", description = "The function the model called.")
@JsonTypeName("ToolCall_function")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-07T21:40:02.582901+02:00[Europe/Zagreb]", comments = "Generator version: 7.20.0")
public class ToolCallFunctionModel {

  private @Nullable String name;

  private @Nullable String arguments;

  public ToolCallFunctionModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  
  @Schema(name = "name", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }

  public ToolCallFunctionModel arguments(@Nullable String arguments) {
    this.arguments = arguments;
    return this;
  }

  /**
   * Get arguments
   * @return arguments
   */
  
  @Schema(name = "arguments", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("arguments")
  public @Nullable String getArguments() {
    return arguments;
  }

  public void setArguments(@Nullable String arguments) {
    this.arguments = arguments;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ToolCallFunctionModel toolCallFunction = (ToolCallFunctionModel) o;
    return Objects.equals(this.name, toolCallFunction.name) &&
        Objects.equals(this.arguments, toolCallFunction.arguments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, arguments);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ToolCallFunctionModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    arguments: ").append(toIndentedString(arguments)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

