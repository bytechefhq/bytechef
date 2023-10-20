package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyOptionModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ValuePropertyAllOfModel
 */

@JsonTypeName("ValueProperty_allOf")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-02T18:38:21.432374+01:00[Europe/Zagreb]")
public class ValuePropertyAllOfModel {

  @JsonProperty("defaultValue")
  private Object defaultValue;

  @JsonProperty("exampleValue")
  private Object exampleValue;

  @JsonProperty("options")
  @Valid
  private List<PropertyOptionModel> options = null;

  public ValuePropertyAllOfModel defaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  /**
   * The property default value.
   * @return defaultValue
  */
  
  @Schema(name = "defaultValue", description = "The property default value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Object getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
  }

  public ValuePropertyAllOfModel exampleValue(Object exampleValue) {
    this.exampleValue = exampleValue;
    return this;
  }

  /**
   * The property example value.
   * @return exampleValue
  */
  
  @Schema(name = "exampleValue", description = "The property example value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Object getExampleValue() {
    return exampleValue;
  }

  public void setExampleValue(Object exampleValue) {
    this.exampleValue = exampleValue;
  }

  public ValuePropertyAllOfModel options(List<PropertyOptionModel> options) {
    this.options = options;
    return this;
  }

  public ValuePropertyAllOfModel addOptionsItem(PropertyOptionModel optionsItem) {
    if (this.options == null) {
      this.options = new ArrayList<>();
    }
    this.options.add(optionsItem);
    return this;
  }

  /**
   * The list of valid property options.
   * @return options
  */
  @Valid 
  @Schema(name = "options", description = "The list of valid property options.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public List<PropertyOptionModel> getOptions() {
    return options;
  }

  public void setOptions(List<PropertyOptionModel> options) {
    this.options = options;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ValuePropertyAllOfModel valuePropertyAllOf = (ValuePropertyAllOfModel) o;
    return Objects.equals(this.defaultValue, valuePropertyAllOf.defaultValue) &&
        Objects.equals(this.exampleValue, valuePropertyAllOf.exampleValue) &&
        Objects.equals(this.options, valuePropertyAllOf.options);
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaultValue, exampleValue, options);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ValuePropertyAllOfModel {\n");
    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
    sb.append("    exampleValue: ").append(toIndentedString(exampleValue)).append("\n");
    sb.append("    options: ").append(toIndentedString(options)).append("\n");
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

