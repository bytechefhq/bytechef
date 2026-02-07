package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.ControlTypeModel;
import com.bytechef.platform.configuration.web.rest.model.OptionModel;
import com.bytechef.platform.configuration.web.rest.model.OptionsDataSourceModel;
import com.bytechef.platform.configuration.web.rest.model.PropertyTypeModel;
import com.bytechef.platform.configuration.web.rest.model.ValuePropertyModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A number property type.
 */

@Schema(name = "NumberProperty", description = "A number property type.")

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-07T09:52:01.007100+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public class NumberPropertyModel extends ValuePropertyModel {

  private @Nullable Double defaultValue;

  private @Nullable Double exampleValue;

  private @Nullable Integer maxNumberPrecision;

  private @Nullable Double maxValue;

  private @Nullable Integer minNumberPrecision;

  private @Nullable Double minValue;

  private @Nullable Integer numberPrecision;

  @Valid
  private List<@Valid OptionModel> options = new ArrayList<>();

  private @Nullable OptionsDataSourceModel optionsDataSource;

  public NumberPropertyModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public NumberPropertyModel(PropertyTypeModel type, ControlTypeModel controlType) {
    super(controlType, type);
  }

  public NumberPropertyModel defaultValue(@Nullable Double defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  /**
   * The property default value.
   * @return defaultValue
   */
  
  @Schema(name = "defaultValue", description = "The property default value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("defaultValue")
  public @Nullable Double getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(@Nullable Double defaultValue) {
    this.defaultValue = defaultValue;
  }

  public NumberPropertyModel exampleValue(@Nullable Double exampleValue) {
    this.exampleValue = exampleValue;
    return this;
  }

  /**
   * The property sample value.
   * @return exampleValue
   */
  
  @Schema(name = "exampleValue", description = "The property sample value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("exampleValue")
  public @Nullable Double getExampleValue() {
    return exampleValue;
  }

  public void setExampleValue(@Nullable Double exampleValue) {
    this.exampleValue = exampleValue;
  }

  public NumberPropertyModel maxNumberPrecision(@Nullable Integer maxNumberPrecision) {
    this.maxNumberPrecision = maxNumberPrecision;
    return this;
  }

  /**
   * The number value precision.
   * @return maxNumberPrecision
   */
  
  @Schema(name = "maxNumberPrecision", description = "The number value precision.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("maxNumberPrecision")
  public @Nullable Integer getMaxNumberPrecision() {
    return maxNumberPrecision;
  }

  public void setMaxNumberPrecision(@Nullable Integer maxNumberPrecision) {
    this.maxNumberPrecision = maxNumberPrecision;
  }

  public NumberPropertyModel maxValue(@Nullable Double maxValue) {
    this.maxValue = maxValue;
    return this;
  }

  /**
   * The maximum property value.
   * @return maxValue
   */
  
  @Schema(name = "maxValue", description = "The maximum property value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("maxValue")
  public @Nullable Double getMaxValue() {
    return maxValue;
  }

  public void setMaxValue(@Nullable Double maxValue) {
    this.maxValue = maxValue;
  }

  public NumberPropertyModel minNumberPrecision(@Nullable Integer minNumberPrecision) {
    this.minNumberPrecision = minNumberPrecision;
    return this;
  }

  /**
   * The number value precision.
   * @return minNumberPrecision
   */
  
  @Schema(name = "minNumberPrecision", description = "The number value precision.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("minNumberPrecision")
  public @Nullable Integer getMinNumberPrecision() {
    return minNumberPrecision;
  }

  public void setMinNumberPrecision(@Nullable Integer minNumberPrecision) {
    this.minNumberPrecision = minNumberPrecision;
  }

  public NumberPropertyModel minValue(@Nullable Double minValue) {
    this.minValue = minValue;
    return this;
  }

  /**
   * The minimum property value.
   * @return minValue
   */
  
  @Schema(name = "minValue", description = "The minimum property value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("minValue")
  public @Nullable Double getMinValue() {
    return minValue;
  }

  public void setMinValue(@Nullable Double minValue) {
    this.minValue = minValue;
  }

  public NumberPropertyModel numberPrecision(@Nullable Integer numberPrecision) {
    this.numberPrecision = numberPrecision;
    return this;
  }

  /**
   * The number value precision.
   * @return numberPrecision
   */
  
  @Schema(name = "numberPrecision", description = "The number value precision.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("numberPrecision")
  public @Nullable Integer getNumberPrecision() {
    return numberPrecision;
  }

  public void setNumberPrecision(@Nullable Integer numberPrecision) {
    this.numberPrecision = numberPrecision;
  }

  public NumberPropertyModel options(List<@Valid OptionModel> options) {
    this.options = options;
    return this;
  }

  public NumberPropertyModel addOptionsItem(OptionModel optionsItem) {
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
  @JsonProperty("options")
  public List<@Valid OptionModel> getOptions() {
    return options;
  }

  public void setOptions(List<@Valid OptionModel> options) {
    this.options = options;
  }

  public NumberPropertyModel optionsDataSource(@Nullable OptionsDataSourceModel optionsDataSource) {
    this.optionsDataSource = optionsDataSource;
    return this;
  }

  /**
   * Get optionsDataSource
   * @return optionsDataSource
   */
  @Valid 
  @Schema(name = "optionsDataSource", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("optionsDataSource")
  public @Nullable OptionsDataSourceModel getOptionsDataSource() {
    return optionsDataSource;
  }

  public void setOptionsDataSource(@Nullable OptionsDataSourceModel optionsDataSource) {
    this.optionsDataSource = optionsDataSource;
  }


  public NumberPropertyModel controlType(ControlTypeModel controlType) {
    super.controlType(controlType);
    return this;
  }

  public NumberPropertyModel label(String label) {
    super.label(label);
    return this;
  }

  public NumberPropertyModel placeholder(String placeholder) {
    super.placeholder(placeholder);
    return this;
  }

  public NumberPropertyModel advancedOption(Boolean advancedOption) {
    super.advancedOption(advancedOption);
    return this;
  }

  public NumberPropertyModel description(String description) {
    super.description(description);
    return this;
  }

  public NumberPropertyModel displayCondition(String displayCondition) {
    super.displayCondition(displayCondition);
    return this;
  }

  public NumberPropertyModel expressionEnabled(Boolean expressionEnabled) {
    super.expressionEnabled(expressionEnabled);
    return this;
  }

  public NumberPropertyModel hidden(Boolean hidden) {
    super.hidden(hidden);
    return this;
  }

  public NumberPropertyModel name(String name) {
    super.name(name);
    return this;
  }

  public NumberPropertyModel required(Boolean required) {
    super.required(required);
    return this;
  }

  public NumberPropertyModel type(PropertyTypeModel type) {
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
    NumberPropertyModel numberProperty = (NumberPropertyModel) o;
    return Objects.equals(this.defaultValue, numberProperty.defaultValue) &&
        Objects.equals(this.exampleValue, numberProperty.exampleValue) &&
        Objects.equals(this.maxNumberPrecision, numberProperty.maxNumberPrecision) &&
        Objects.equals(this.maxValue, numberProperty.maxValue) &&
        Objects.equals(this.minNumberPrecision, numberProperty.minNumberPrecision) &&
        Objects.equals(this.minValue, numberProperty.minValue) &&
        Objects.equals(this.numberPrecision, numberProperty.numberPrecision) &&
        Objects.equals(this.options, numberProperty.options) &&
        Objects.equals(this.optionsDataSource, numberProperty.optionsDataSource) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaultValue, exampleValue, maxNumberPrecision, maxValue, minNumberPrecision, minValue, numberPrecision, options, optionsDataSource, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NumberPropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
    sb.append("    exampleValue: ").append(toIndentedString(exampleValue)).append("\n");
    sb.append("    maxNumberPrecision: ").append(toIndentedString(maxNumberPrecision)).append("\n");
    sb.append("    maxValue: ").append(toIndentedString(maxValue)).append("\n");
    sb.append("    minNumberPrecision: ").append(toIndentedString(minNumberPrecision)).append("\n");
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
  private String toIndentedString(@Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

