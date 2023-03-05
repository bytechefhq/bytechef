package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.ControlTypeModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
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
 * ValuePropertyAllOfModel
 */

@JsonTypeName("ValueProperty_allOf")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-05T16:27:34.189599+01:00[Europe/Zagreb]")
public class ValuePropertyAllOfModel {

  @JsonProperty("controlType")
  private ControlTypeModel controlType;

  @JsonProperty("defaultValue")
  private Object defaultValue;

  @JsonProperty("sampleValue")
  private Object sampleValue;

  public ValuePropertyAllOfModel controlType(ControlTypeModel controlType) {
    this.controlType = controlType;
    return this;
  }

  /**
   * Get controlType
   * @return controlType
  */
  @Valid 
  @Schema(name = "controlType", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public ControlTypeModel getControlType() {
    return controlType;
  }

  public void setControlType(ControlTypeModel controlType) {
    this.controlType = controlType;
  }

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

  public ValuePropertyAllOfModel sampleValue(Object sampleValue) {
    this.sampleValue = sampleValue;
    return this;
  }

  /**
   * The property sample value.
   * @return sampleValue
  */
  
  @Schema(name = "sampleValue", description = "The property sample value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Object getSampleValue() {
    return sampleValue;
  }

  public void setSampleValue(Object sampleValue) {
    this.sampleValue = sampleValue;
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
    return Objects.equals(this.controlType, valuePropertyAllOf.controlType) &&
        Objects.equals(this.defaultValue, valuePropertyAllOf.defaultValue) &&
        Objects.equals(this.sampleValue, valuePropertyAllOf.sampleValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(controlType, defaultValue, sampleValue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ValuePropertyAllOfModel {\n");
    sb.append("    controlType: ").append(toIndentedString(controlType)).append("\n");
    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
    sb.append("    sampleValue: ").append(toIndentedString(sampleValue)).append("\n");
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

