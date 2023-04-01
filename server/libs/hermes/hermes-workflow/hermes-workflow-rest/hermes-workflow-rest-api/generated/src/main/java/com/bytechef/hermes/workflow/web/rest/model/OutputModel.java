package com.bytechef.hermes.workflow.web.rest.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import jakarta.annotation.Generated;

/**
 * OutputModel
 */

@JsonTypeName("Output")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-31T07:18:30.644746+02:00[Europe/Zagreb]")
public class OutputModel {

  @JsonProperty("name")
  private String name;

  @JsonProperty("value")
  private Object value;

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

