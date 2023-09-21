package com.bytechef.hermes.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.configuration.web.rest.model.OptionModel;
import com.bytechef.hermes.configuration.web.rest.model.OptionsDataSourceModel;
import com.bytechef.hermes.configuration.web.rest.model.PropertyModel;
import com.bytechef.hermes.configuration.web.rest.model.PropertyTypeModel;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-09-21T12:30:40.155708+02:00[Europe/Zagreb]")
public class NumberPropertyModel extends PropertyModel {

  private Integer maxValue;

  private Integer minValue;

  private Integer numberPrecision;

  @Valid
  private List<@Valid OptionModel> options;

  private OptionsDataSourceModel optionsDataSource;

  public NumberPropertyModel maxValue(Integer maxValue) {
    this.maxValue = maxValue;
    return this;
  }

  /**
   * The maximum property value.
   * @return maxValue
  */
  
  @Schema(name = "maxValue", description = "The maximum property value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("maxValue")
  public Integer getMaxValue() {
    return maxValue;
  }

  public void setMaxValue(Integer maxValue) {
    this.maxValue = maxValue;
  }

  public NumberPropertyModel minValue(Integer minValue) {
    this.minValue = minValue;
    return this;
  }

  /**
   * The minimum property value.
   * @return minValue
  */
  
  @Schema(name = "minValue", description = "The minimum property value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("minValue")
  public Integer getMinValue() {
    return minValue;
  }

  public void setMinValue(Integer minValue) {
    this.minValue = minValue;
  }

  public NumberPropertyModel numberPrecision(Integer numberPrecision) {
    this.numberPrecision = numberPrecision;
    return this;
  }

  /**
   * The number value precision.
   * @return numberPrecision
  */
  
  @Schema(name = "numberPrecision", description = "The number value precision.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("numberPrecision")
  public Integer getNumberPrecision() {
    return numberPrecision;
  }

  public void setNumberPrecision(Integer numberPrecision) {
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

  public NumberPropertyModel optionsDataSource(OptionsDataSourceModel optionsDataSource) {
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
  public OptionsDataSourceModel getOptionsDataSource() {
    return optionsDataSource;
  }

  public void setOptionsDataSource(OptionsDataSourceModel optionsDataSource) {
    this.optionsDataSource = optionsDataSource;
  }


  public NumberPropertyModel advancedOption(Boolean advancedOption) {
    super.setAdvancedOption(advancedOption);
    return this;
  }

  public NumberPropertyModel description(String description) {
    super.setDescription(description);
    return this;
  }

  public NumberPropertyModel displayCondition(String displayCondition) {
    super.setDisplayCondition(displayCondition);
    return this;
  }

  public NumberPropertyModel expressionEnabled(Boolean expressionEnabled) {
    super.setExpressionEnabled(expressionEnabled);
    return this;
  }

  public NumberPropertyModel hidden(Boolean hidden) {
    super.setHidden(hidden);
    return this;
  }

  public NumberPropertyModel label(String label) {
    super.setLabel(label);
    return this;
  }

  public NumberPropertyModel name(String name) {
    super.setName(name);
    return this;
  }

  public NumberPropertyModel placeholder(String placeholder) {
    super.setPlaceholder(placeholder);
    return this;
  }

  public NumberPropertyModel required(Boolean required) {
    super.setRequired(required);
    return this;
  }

  public NumberPropertyModel type(PropertyTypeModel type) {
    super.setType(type);
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
    return Objects.equals(this.maxValue, numberProperty.maxValue) &&
        Objects.equals(this.minValue, numberProperty.minValue) &&
        Objects.equals(this.numberPrecision, numberProperty.numberPrecision) &&
        Objects.equals(this.options, numberProperty.options) &&
        Objects.equals(this.optionsDataSource, numberProperty.optionsDataSource) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxValue, minValue, numberPrecision, options, optionsDataSource, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NumberPropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
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

