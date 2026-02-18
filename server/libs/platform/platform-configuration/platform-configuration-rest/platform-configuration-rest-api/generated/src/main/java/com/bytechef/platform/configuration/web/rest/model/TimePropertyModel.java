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
 * A time property.
 */

@Schema(name = "TimeProperty", description = "A time property.")

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-19T05:53:32.886377+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public class TimePropertyModel extends ValuePropertyModel {

  private @Nullable String defaultValue;

  private @Nullable String exampleValue;

  @Valid
  private List<@Valid OptionModel> options = new ArrayList<>();

  private @Nullable OptionsDataSourceModel optionsDataSource;

  public TimePropertyModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TimePropertyModel(PropertyTypeModel type, ControlTypeModel controlType) {
    super(controlType, type);
  }

  public TimePropertyModel defaultValue(@Nullable String defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  /**
   * The property default value.
   * @return defaultValue
   */
  
  @Schema(name = "defaultValue", description = "The property default value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("defaultValue")
  public @Nullable String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(@Nullable String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public TimePropertyModel exampleValue(@Nullable String exampleValue) {
    this.exampleValue = exampleValue;
    return this;
  }

  /**
   * The property sample value.
   * @return exampleValue
   */
  
  @Schema(name = "exampleValue", description = "The property sample value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("exampleValue")
  public @Nullable String getExampleValue() {
    return exampleValue;
  }

  public void setExampleValue(@Nullable String exampleValue) {
    this.exampleValue = exampleValue;
  }

  public TimePropertyModel options(List<@Valid OptionModel> options) {
    this.options = options;
    return this;
  }

  public TimePropertyModel addOptionsItem(OptionModel optionsItem) {
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

  public TimePropertyModel optionsDataSource(@Nullable OptionsDataSourceModel optionsDataSource) {
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


  public TimePropertyModel controlType(ControlTypeModel controlType) {
    super.controlType(controlType);
    return this;
  }

  public TimePropertyModel label(String label) {
    super.label(label);
    return this;
  }

  public TimePropertyModel placeholder(String placeholder) {
    super.placeholder(placeholder);
    return this;
  }

  public TimePropertyModel advancedOption(Boolean advancedOption) {
    super.advancedOption(advancedOption);
    return this;
  }

  public TimePropertyModel description(String description) {
    super.description(description);
    return this;
  }

  public TimePropertyModel displayCondition(String displayCondition) {
    super.displayCondition(displayCondition);
    return this;
  }

  public TimePropertyModel expressionEnabled(Boolean expressionEnabled) {
    super.expressionEnabled(expressionEnabled);
    return this;
  }

  public TimePropertyModel hidden(Boolean hidden) {
    super.hidden(hidden);
    return this;
  }

  public TimePropertyModel name(String name) {
    super.name(name);
    return this;
  }

  public TimePropertyModel required(Boolean required) {
    super.required(required);
    return this;
  }

  public TimePropertyModel type(PropertyTypeModel type) {
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
    TimePropertyModel timeProperty = (TimePropertyModel) o;
    return Objects.equals(this.defaultValue, timeProperty.defaultValue) &&
        Objects.equals(this.exampleValue, timeProperty.exampleValue) &&
        Objects.equals(this.options, timeProperty.options) &&
        Objects.equals(this.optionsDataSource, timeProperty.optionsDataSource) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaultValue, exampleValue, options, optionsDataSource, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TimePropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
    sb.append("    exampleValue: ").append(toIndentedString(exampleValue)).append("\n");
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

