package com.bytechef.ee.automation.ai.gateway.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.ToolFunctionModel;
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
 * A tool the model may call.
 */

@Schema(name = "Tool", description = "A tool the model may call.")
@JsonTypeName("Tool")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-07T21:40:02.582901+02:00[Europe/Zagreb]", comments = "Generator version: 7.20.0")
public class ToolModel {

  private @Nullable String type;

  private @Nullable ToolFunctionModel function;

  public ToolModel type(@Nullable String type) {
    this.type = type;
    return this;
  }

  /**
   * The type of the tool.
   * @return type
   */
  
  @Schema(name = "type", description = "The type of the tool.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("type")
  public @Nullable String getType() {
    return type;
  }

  public void setType(@Nullable String type) {
    this.type = type;
  }

  public ToolModel function(@Nullable ToolFunctionModel function) {
    this.function = function;
    return this;
  }

  /**
   * Get function
   * @return function
   */
  @Valid 
  @Schema(name = "function", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("function")
  public @Nullable ToolFunctionModel getFunction() {
    return function;
  }

  public void setFunction(@Nullable ToolFunctionModel function) {
    this.function = function;
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
    return Objects.equals(this.type, tool.type) &&
        Objects.equals(this.function, tool.function);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, function);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ToolModel {\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    function: ").append(toIndentedString(function)).append("\n");
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

