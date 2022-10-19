package com.bytechef.hermes.component.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.component.web.rest.model.PropertyOptionModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * IntegerPropertyAllOfModel
 */

@JsonTypeName("IntegerProperty_allOf")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-19T12:56:09.815841+02:00[Europe/Zagreb]")
public class IntegerPropertyAllOfModel {

  @JsonProperty("required")
  private Boolean required;

  @JsonProperty("defaultValue")
  private Integer defaultValue;

  @JsonProperty("exampleValue")
  private Integer exampleValue;

  @JsonProperty("loadOptionsDependsOn")
  @Valid
  private List<String> loadOptionsDependsOn = null;

  @JsonProperty("loadOptionsMethod")
  private String loadOptionsMethod;

  @JsonProperty("options")
  @Valid
  private List<PropertyOptionModel> options = null;

  @JsonProperty("maxValue")
  private Integer maxValue;

  @JsonProperty("minValue")
  private Integer minValue;

  public IntegerPropertyAllOfModel required(Boolean required) {
    this.required = required;
    return this;
  }

  /**
   * Get required
   * @return required
  */
  
  @Schema(name = "required", required = false)
  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public IntegerPropertyAllOfModel defaultValue(Integer defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  /**
   * Get defaultValue
   * @return defaultValue
  */
  
  @Schema(name = "defaultValue", required = false)
  public Integer getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(Integer defaultValue) {
    this.defaultValue = defaultValue;
  }

  public IntegerPropertyAllOfModel exampleValue(Integer exampleValue) {
    this.exampleValue = exampleValue;
    return this;
  }

  /**
   * Get exampleValue
   * @return exampleValue
  */
  
  @Schema(name = "exampleValue", required = false)
  public Integer getExampleValue() {
    return exampleValue;
  }

  public void setExampleValue(Integer exampleValue) {
    this.exampleValue = exampleValue;
  }

  public IntegerPropertyAllOfModel loadOptionsDependsOn(List<String> loadOptionsDependsOn) {
    this.loadOptionsDependsOn = loadOptionsDependsOn;
    return this;
  }

  public IntegerPropertyAllOfModel addLoadOptionsDependsOnItem(String loadOptionsDependsOnItem) {
    if (this.loadOptionsDependsOn == null) {
      this.loadOptionsDependsOn = new ArrayList<>();
    }
    this.loadOptionsDependsOn.add(loadOptionsDependsOnItem);
    return this;
  }

  /**
   * Get loadOptionsDependsOn
   * @return loadOptionsDependsOn
  */
  
  @Schema(name = "loadOptionsDependsOn", required = false)
  public List<String> getLoadOptionsDependsOn() {
    return loadOptionsDependsOn;
  }

  public void setLoadOptionsDependsOn(List<String> loadOptionsDependsOn) {
    this.loadOptionsDependsOn = loadOptionsDependsOn;
  }

  public IntegerPropertyAllOfModel loadOptionsMethod(String loadOptionsMethod) {
    this.loadOptionsMethod = loadOptionsMethod;
    return this;
  }

  /**
   * Get loadOptionsMethod
   * @return loadOptionsMethod
  */
  
  @Schema(name = "loadOptionsMethod", required = false)
  public String getLoadOptionsMethod() {
    return loadOptionsMethod;
  }

  public void setLoadOptionsMethod(String loadOptionsMethod) {
    this.loadOptionsMethod = loadOptionsMethod;
  }

  public IntegerPropertyAllOfModel options(List<PropertyOptionModel> options) {
    this.options = options;
    return this;
  }

  public IntegerPropertyAllOfModel addOptionsItem(PropertyOptionModel optionsItem) {
    if (this.options == null) {
      this.options = new ArrayList<>();
    }
    this.options.add(optionsItem);
    return this;
  }

  /**
   * Get options
   * @return options
  */
  @Valid 
  @Schema(name = "options", required = false)
  public List<PropertyOptionModel> getOptions() {
    return options;
  }

  public void setOptions(List<PropertyOptionModel> options) {
    this.options = options;
  }

  public IntegerPropertyAllOfModel maxValue(Integer maxValue) {
    this.maxValue = maxValue;
    return this;
  }

  /**
   * Get maxValue
   * @return maxValue
  */
  
  @Schema(name = "maxValue", required = false)
  public Integer getMaxValue() {
    return maxValue;
  }

  public void setMaxValue(Integer maxValue) {
    this.maxValue = maxValue;
  }

  public IntegerPropertyAllOfModel minValue(Integer minValue) {
    this.minValue = minValue;
    return this;
  }

  /**
   * Get minValue
   * @return minValue
  */
  
  @Schema(name = "minValue", required = false)
  public Integer getMinValue() {
    return minValue;
  }

  public void setMinValue(Integer minValue) {
    this.minValue = minValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IntegerPropertyAllOfModel integerPropertyAllOf = (IntegerPropertyAllOfModel) o;
    return Objects.equals(this.required, integerPropertyAllOf.required) &&
        Objects.equals(this.defaultValue, integerPropertyAllOf.defaultValue) &&
        Objects.equals(this.exampleValue, integerPropertyAllOf.exampleValue) &&
        Objects.equals(this.loadOptionsDependsOn, integerPropertyAllOf.loadOptionsDependsOn) &&
        Objects.equals(this.loadOptionsMethod, integerPropertyAllOf.loadOptionsMethod) &&
        Objects.equals(this.options, integerPropertyAllOf.options) &&
        Objects.equals(this.maxValue, integerPropertyAllOf.maxValue) &&
        Objects.equals(this.minValue, integerPropertyAllOf.minValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(required, defaultValue, exampleValue, loadOptionsDependsOn, loadOptionsMethod, options, maxValue, minValue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegerPropertyAllOfModel {\n");
    sb.append("    required: ").append(toIndentedString(required)).append("\n");
    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
    sb.append("    exampleValue: ").append(toIndentedString(exampleValue)).append("\n");
    sb.append("    loadOptionsDependsOn: ").append(toIndentedString(loadOptionsDependsOn)).append("\n");
    sb.append("    loadOptionsMethod: ").append(toIndentedString(loadOptionsMethod)).append("\n");
    sb.append("    options: ").append(toIndentedString(options)).append("\n");
    sb.append("    maxValue: ").append(toIndentedString(maxValue)).append("\n");
    sb.append("    minValue: ").append(toIndentedString(minValue)).append("\n");
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

