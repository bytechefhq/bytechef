package com.bytechef.helios.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * OutputModel
 */

@JsonTypeName("Output")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-12-11T16:20:12.810736+01:00[Europe/Zagreb]")
public class OutputModel {

  private String name;

  private Object value;

  public OutputModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public OutputModel(String name, Object value) {
    this.name = name;
    this.value = value;
  }

  public OutputModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of an output
   * @return name
  */
  @NotNull 
  @Schema(name = "name", description = "The name of an output", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public OutputModel value(Object value) {
    this.value = value;
    return this;
  }

  /**
   * The value of an output
   * @return value
  */
  @NotNull 
  @Schema(name = "value", description = "The value of an output", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("value")
  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OutputModel output = (OutputModel) o;
    return Objects.equals(this.name, output.name) &&
        Objects.equals(this.value, output.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OutputModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
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

