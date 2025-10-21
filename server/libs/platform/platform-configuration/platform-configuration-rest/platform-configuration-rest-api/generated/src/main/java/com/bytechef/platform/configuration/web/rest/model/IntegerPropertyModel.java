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
 * An integer property type.
 */

@Schema(name = "IntegerProperty", description = "An integer property type.")

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-21T12:06:41.161145+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public class IntegerPropertyModel extends ValuePropertyModel {

  private @Nullable Long defaultValue;

  private @Nullable Long exampleValue;

  private @Nullable Long maxValue;

  private @Nullable Long minValue;

  @Valid
  private List<@Valid OptionModel> options = new ArrayList<>();

  private @Nullable OptionsDataSourceModel optionsDataSource;

  public IntegerPropertyModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public IntegerPropertyModel(PropertyTypeModel type, ControlTypeModel controlType) {
    super(controlType, type);
  }

  public IntegerPropertyModel defaultValue(@Nullable Long defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  /**
   * The property default value.
   * @return defaultValue
   */
  
  @Schema(name = "defaultValue", description = "The property default value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("defaultValue")
  public @Nullable Long getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(@Nullable Long defaultValue) {
    this.defaultValue = defaultValue;
  }

  public IntegerPropertyModel exampleValue(@Nullable Long exampleValue) {
    this.exampleValue = exampleValue;
    return this;
  }

  /**
   * The property sample value.
   * @return exampleValue
   */
  
  @Schema(name = "exampleValue", description = "The property sample value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("exampleValue")
  public @Nullable Long getExampleValue() {
    return exampleValue;
  }

  public void setExampleValue(@Nullable Long exampleValue) {
    this.exampleValue = exampleValue;
  }

  public IntegerPropertyModel maxValue(@Nullable Long maxValue) {
    this.maxValue = maxValue;
    return this;
  }

  /**
   * The maximum property value.
   * @return maxValue
   */
  
  @Schema(name = "maxValue", description = "The maximum property value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("maxValue")
  public @Nullable Long getMaxValue() {
    return maxValue;
  }

  public void setMaxValue(@Nullable Long maxValue) {
    this.maxValue = maxValue;
  }

  public IntegerPropertyModel minValue(@Nullable Long minValue) {
    this.minValue = minValue;
    return this;
  }

  /**
   * The minimum property value.
   * @return minValue
   */
  
  @Schema(name = "minValue", description = "The minimum property value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("minValue")
  public @Nullable Long getMinValue() {
    return minValue;
  }

  public void setMinValue(@Nullable Long minValue) {
    this.minValue = minValue;
  }

  public IntegerPropertyModel options(List<@Valid OptionModel> options) {
    this.options = options;
    return this;
  }

  public IntegerPropertyModel addOptionsItem(OptionModel optionsItem) {
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

  public IntegerPropertyModel optionsDataSource(@Nullable OptionsDataSourceModel optionsDataSource) {
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


  public IntegerPropertyModel controlType(ControlTypeModel controlType) {
    super.controlType(controlType);
    return this;
  }

  public IntegerPropertyModel label(String label) {
    super.label(label);
    return this;
  }

  public IntegerPropertyModel placeholder(String placeholder) {
    super.placeholder(placeholder);
    return this;
  }

  public IntegerPropertyModel advancedOption(Boolean advancedOption) {
    super.advancedOption(advancedOption);
    return this;
  }

  public IntegerPropertyModel description(String description) {
    super.description(description);
    return this;
  }

  public IntegerPropertyModel displayCondition(String displayCondition) {
    super.displayCondition(displayCondition);
    return this;
  }

  public IntegerPropertyModel expressionEnabled(Boolean expressionEnabled) {
    super.expressionEnabled(expressionEnabled);
    return this;
  }

  public IntegerPropertyModel hidden(Boolean hidden) {
    super.hidden(hidden);
    return this;
  }

  public IntegerPropertyModel name(String name) {
    super.name(name);
    return this;
  }

  public IntegerPropertyModel required(Boolean required) {
    super.required(required);
    return this;
  }

  public IntegerPropertyModel type(PropertyTypeModel type) {
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
    IntegerPropertyModel integerProperty = (IntegerPropertyModel) o;
    return Objects.equals(this.defaultValue, integerProperty.defaultValue) &&
        Objects.equals(this.exampleValue, integerProperty.exampleValue) &&
        Objects.equals(this.maxValue, integerProperty.maxValue) &&
        Objects.equals(this.minValue, integerProperty.minValue) &&
        Objects.equals(this.options, integerProperty.options) &&
        Objects.equals(this.optionsDataSource, integerProperty.optionsDataSource) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaultValue, exampleValue, maxValue, minValue, options, optionsDataSource, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegerPropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
    sb.append("    exampleValue: ").append(toIndentedString(exampleValue)).append("\n");
    sb.append("    maxValue: ").append(toIndentedString(maxValue)).append("\n");
    sb.append("    minValue: ").append(toIndentedString(minValue)).append("\n");
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

