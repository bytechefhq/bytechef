package com.bytechef.dione.configuration.web.rest.model;

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
 * InputModel
 */

@JsonTypeName("Input")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-07-12T16:22:17.202030+02:00[Europe/Zagreb]")
public class InputModel {

  private String label;

  private String name;

  private Boolean required = false;

  private String type;

  /**
   * Default constructor
   * @deprecated Use {@link InputModel#InputModel(String)}
   */
  @Deprecated
  public InputModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public InputModel(String name) {
    this.name = name;
  }

  public InputModel label(String label) {
    this.label = label;
    return this;
  }

  /**
   * The string of an input
   * @return label
  */
  
  @Schema(name = "label", description = "The string of an input", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public InputModel name(String name) {
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

  public InputModel required(Boolean required) {
    this.required = required;
    return this;
  }

  /**
   * If an input is required, or not
   * @return required
  */
  
  @Schema(name = "required", description = "If an input is required, or not", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("required")
  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public InputModel type(String type) {
    this.type = type;
    return this;
  }

  /**
   * The type of an input, for example \\\"string\\\"
   * @return type
  */
  
  @Schema(name = "type", description = "The type of an input, for example \\\"string\\\"", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    InputModel input = (InputModel) o;
    return Objects.equals(this.label, input.label) &&
        Objects.equals(this.name, input.name) &&
        Objects.equals(this.required, input.required) &&
        Objects.equals(this.type, input.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, name, required, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InputModel {\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    required: ").append(toIndentedString(required)).append("\n");
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

