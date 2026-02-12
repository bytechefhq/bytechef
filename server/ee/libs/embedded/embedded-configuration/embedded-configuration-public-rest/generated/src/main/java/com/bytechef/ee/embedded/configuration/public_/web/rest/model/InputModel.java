package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.InputTypeModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-12T08:42:40.890500807+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public class InputModel {

  private @Nullable String label;

  private String name;

  private Boolean required = false;

  private InputTypeModel type = InputTypeModel.STRING;

  public InputModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public InputModel(String name, InputTypeModel type) {
    this.name = name;
    this.type = type;
  }

  public InputModel label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * The descriptive name of an input
   * @return label
   */
  
  @Schema(name = "label", description = "The descriptive name of an input", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  public void setLabel(@Nullable String label) {
    this.label = label;
  }

  public InputModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of an input
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The name of an input", requiredMode = Schema.RequiredMode.REQUIRED)
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

  public InputModel type(InputTypeModel type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull @Valid 
  @Schema(name = "type", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public InputTypeModel getType() {
    return type;
  }

  public void setType(InputTypeModel type) {
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
  private String toIndentedString(@Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

