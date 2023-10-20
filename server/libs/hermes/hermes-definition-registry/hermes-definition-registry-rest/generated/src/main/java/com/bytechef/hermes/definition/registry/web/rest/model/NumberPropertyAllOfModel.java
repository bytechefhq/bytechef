package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.OptionModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.util.NoSuchElementException;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-05T16:27:34.189599+01:00[Europe/Zagreb]")
public class NumberPropertyAllOfModel {

  @JsonProperty("maxValue")
  private Integer maxValue;

  @JsonProperty("minValue")
  private Integer minValue;

  @JsonProperty("numberPrecision")
  private Integer numberPrecision;

  @JsonProperty("options")
  @Valid
  private List<OptionModel> options = null;

  @JsonProperty("optionsDataSource")
  private JsonNullable<Object> optionsDataSource = JsonNullable.undefined();

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

  public NumberPropertyAllOfModel options(List<OptionModel> options) {
    this.options = options;
    return this;
  }

  public NumberPropertyAllOfModel addOptionsItem(OptionModel optionsItem) {
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
  public List<OptionModel> getOptions() {
    return options;
  }

  public void setOptions(List<OptionModel> options) {
    this.options = options;
  }

  public NumberPropertyAllOfModel optionsDataSource(Object optionsDataSource) {
    this.optionsDataSource = JsonNullable.of(optionsDataSource);
    return this;
  }

  /**
   * Get optionsDataSource
   * @return optionsDataSource
  */
  
  @Schema(name = "optionsDataSource", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public JsonNullable<Object> getOptionsDataSource() {
    return optionsDataSource;
  }

  public void setOptionsDataSource(JsonNullable<Object> optionsDataSource) {
    this.optionsDataSource = optionsDataSource;
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
        Objects.equals(this.numberPrecision, numberPropertyAllOf.numberPrecision) &&
        Objects.equals(this.options, numberPropertyAllOf.options) &&
        equalsNullable(this.optionsDataSource, numberPropertyAllOf.optionsDataSource);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxValue, minValue, numberPrecision, options, hashCodeNullable(optionsDataSource));
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NumberPropertyAllOfModel {\n");
    sb.append("    maxValue: ").append(toIndentedString(maxValue)).append("\n");
    sb.append("    minValue: ").append(toIndentedString(minValue)).append("\n");
    sb.append("    numberPrecision: ").append(toIndentedString(numberPrecision)).append("\n");
    sb.append("    options: ").append(toIndentedString(options)).append("\n");
    sb.append("    optionsDataSource: ").append(toIndentedString(optionsDataSource)).append("\n");
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

