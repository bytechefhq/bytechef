package com.bytechef.hermes.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.configuration.web.rest.model.ControlTypeModel;
import com.bytechef.hermes.configuration.web.rest.model.PropertyTypeModel;
import com.bytechef.hermes.configuration.web.rest.model.ValuePropertyModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A boolean property type.
 */

@Schema(name = "BooleanProperty", description = "A boolean property type.")

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-12-04T07:58:17.397618+01:00[Europe/Zagreb]")
public class BooleanPropertyModel extends ValuePropertyModel {


  public BooleanPropertyModel controlType(ControlTypeModel controlType) {
    super.controlType(controlType);
    return this;
  }

  public BooleanPropertyModel defaultValue(Object defaultValue) {
    super.defaultValue(defaultValue);
    return this;
  }

  public BooleanPropertyModel exampleValue(Object exampleValue) {
    super.exampleValue(exampleValue);
    return this;
  }

  public BooleanPropertyModel advancedOption(Boolean advancedOption) {
    super.advancedOption(advancedOption);
    return this;
  }

  public BooleanPropertyModel description(String description) {
    super.description(description);
    return this;
  }

  public BooleanPropertyModel displayCondition(String displayCondition) {
    super.displayCondition(displayCondition);
    return this;
  }

  public BooleanPropertyModel expressionEnabled(Boolean expressionEnabled) {
    super.expressionEnabled(expressionEnabled);
    return this;
  }

  public BooleanPropertyModel hidden(Boolean hidden) {
    super.hidden(hidden);
    return this;
  }

  public BooleanPropertyModel label(String label) {
    super.label(label);
    return this;
  }

  public BooleanPropertyModel name(String name) {
    super.name(name);
    return this;
  }

  public BooleanPropertyModel placeholder(String placeholder) {
    super.placeholder(placeholder);
    return this;
  }

  public BooleanPropertyModel required(Boolean required) {
    super.required(required);
    return this;
  }

  public BooleanPropertyModel type(PropertyTypeModel type) {
    super.type(type);
    return this;
  }
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BooleanPropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
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

