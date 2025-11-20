package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.ControlTypeModel;
import com.bytechef.platform.configuration.web.rest.model.PropertyTypeModel;
import com.bytechef.platform.configuration.web.rest.model.ValuePropertyModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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
 * A null property type.
 */

@Schema(name = "NullProperty", description = "A null property type.")

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:33.876560+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class NullPropertyModel extends ValuePropertyModel {

  public NullPropertyModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public NullPropertyModel(PropertyTypeModel type, ControlTypeModel controlType) {
    super(controlType, type);
  }


  public NullPropertyModel controlType(ControlTypeModel controlType) {
    super.controlType(controlType);
    return this;
  }

  public NullPropertyModel label(String label) {
    super.label(label);
    return this;
  }

  public NullPropertyModel placeholder(String placeholder) {
    super.placeholder(placeholder);
    return this;
  }

  public NullPropertyModel advancedOption(Boolean advancedOption) {
    super.advancedOption(advancedOption);
    return this;
  }

  public NullPropertyModel description(String description) {
    super.description(description);
    return this;
  }

  public NullPropertyModel displayCondition(String displayCondition) {
    super.displayCondition(displayCondition);
    return this;
  }

  public NullPropertyModel expressionEnabled(Boolean expressionEnabled) {
    super.expressionEnabled(expressionEnabled);
    return this;
  }

  public NullPropertyModel hidden(Boolean hidden) {
    super.hidden(hidden);
    return this;
  }

  public NullPropertyModel name(String name) {
    super.name(name);
    return this;
  }

  public NullPropertyModel required(Boolean required) {
    super.required(required);
    return this;
  }

  public NullPropertyModel type(PropertyTypeModel type) {
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
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NullPropertyModel {\n");
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

