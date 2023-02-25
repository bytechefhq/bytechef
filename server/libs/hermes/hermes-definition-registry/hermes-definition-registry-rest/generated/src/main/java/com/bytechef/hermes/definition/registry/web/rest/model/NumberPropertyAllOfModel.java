package com.bytechef.hermes.definition.registry.web.rest.model;

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
 * NumberPropertyAllOfModel
 */

@JsonTypeName("NumberProperty_allOf")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-02-24T18:32:48.786669+01:00[Europe/Zagreb]")
public class NumberPropertyAllOfModel {

  @JsonProperty("maxValue")
  private Integer maxValue;

  @JsonProperty("minValue")
  private Integer minValue;

  @JsonProperty("numberPrecision")
  private Integer numberPrecision;

  public NumberPropertyAllOfModel maxValue(Integer maxValue) {
    this.maxValue = maxValue;
    return this;
  }

  /**
   * The maximum property value.
   * @return maxValue
  */
  
  @Schema(name = "maxValue", description = "The maximum property value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Integer getMaxValue() {
    return maxValue;
  }

  public void setMaxValue(Integer maxValue) {
    this.maxValue = maxValue;
  }

  public NumberPropertyAllOfModel minValue(Integer minValue) {
    this.minValue = minValue;
    return this;
  }

  /**
   * The minimum property value.
   * @return minValue
  */
  
  @Schema(name = "minValue", description = "The minimum property value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Integer getMinValue() {
    return minValue;
  }

  public void setMinValue(Integer minValue) {
    this.minValue = minValue;
  }

  public NumberPropertyAllOfModel numberPrecision(Integer numberPrecision) {
    this.numberPrecision = numberPrecision;
    return this;
  }

  /**
   * The number value precision.
   * @return numberPrecision
  */
  
  @Schema(name = "numberPrecision", description = "The number value precision.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Integer getNumberPrecision() {
    return numberPrecision;
  }

  public void setNumberPrecision(Integer numberPrecision) {
    this.numberPrecision = numberPrecision;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NumberPropertyAllOfModel numberPropertyAllOf = (NumberPropertyAllOfModel) o;
    return Objects.equals(this.maxValue, numberPropertyAllOf.maxValue) &&
        Objects.equals(this.minValue, numberPropertyAllOf.minValue) &&
        Objects.equals(this.numberPrecision, numberPropertyAllOf.numberPrecision);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxValue, minValue, numberPrecision);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NumberPropertyAllOfModel {\n");
    sb.append("    maxValue: ").append(toIndentedString(maxValue)).append("\n");
    sb.append("    minValue: ").append(toIndentedString(minValue)).append("\n");
    sb.append("    numberPrecision: ").append(toIndentedString(numberPrecision)).append("\n");
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

