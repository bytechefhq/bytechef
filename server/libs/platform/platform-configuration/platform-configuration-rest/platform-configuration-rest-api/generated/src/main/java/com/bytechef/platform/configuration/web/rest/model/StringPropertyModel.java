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
 * A string property.
 */

@Schema(name = "StringProperty", description = "A string property.")

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-19T05:46:41.627972+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public class StringPropertyModel extends ValuePropertyModel {

  private @Nullable String languageId;

  private @Nullable String defaultValue;

  private @Nullable String exampleValue;

  private @Nullable Integer maxLength;

  private @Nullable Integer minLength;

  private @Nullable String regex;

  @Valid
  private List<@Valid OptionModel> options = new ArrayList<>();

  private @Nullable OptionsDataSourceModel optionsDataSource;

  private @Nullable Boolean optionsLoadedDynamically;

  public StringPropertyModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public StringPropertyModel(PropertyTypeModel type, ControlTypeModel controlType) {
    super(controlType, type);
  }

  public StringPropertyModel languageId(@Nullable String languageId) {
    this.languageId = languageId;
    return this;
  }

  /**
   * The language id used together with CODE_EDITOR control type.
   * @return languageId
   */
  
  @Schema(name = "languageId", description = "The language id used together with CODE_EDITOR control type.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("languageId")
  public @Nullable String getLanguageId() {
    return languageId;
  }

  public void setLanguageId(@Nullable String languageId) {
    this.languageId = languageId;
  }

  public StringPropertyModel defaultValue(@Nullable String defaultValue) {
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

  public StringPropertyModel exampleValue(@Nullable String exampleValue) {
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

  public StringPropertyModel maxLength(@Nullable Integer maxLength) {
    this.maxLength = maxLength;
    return this;
  }

  /**
   * The maximum string length.
   * @return maxLength
   */
  
  @Schema(name = "maxLength", description = "The maximum string length.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("maxLength")
  public @Nullable Integer getMaxLength() {
    return maxLength;
  }

  public void setMaxLength(@Nullable Integer maxLength) {
    this.maxLength = maxLength;
  }

  public StringPropertyModel minLength(@Nullable Integer minLength) {
    this.minLength = minLength;
    return this;
  }

  /**
   * The minimum string length.
   * @return minLength
   */
  
  @Schema(name = "minLength", description = "The minimum string length.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("minLength")
  public @Nullable Integer getMinLength() {
    return minLength;
  }

  public void setMinLength(@Nullable Integer minLength) {
    this.minLength = minLength;
  }

  public StringPropertyModel regex(@Nullable String regex) {
    this.regex = regex;
    return this;
  }

  /**
   * The regular expression pattern for validation.
   * @return regex
   */
  
  @Schema(name = "regex", description = "The regular expression pattern for validation.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("regex")
  public @Nullable String getRegex() {
    return regex;
  }

  public void setRegex(@Nullable String regex) {
    this.regex = regex;
  }

  public StringPropertyModel options(List<@Valid OptionModel> options) {
    this.options = options;
    return this;
  }

  public StringPropertyModel addOptionsItem(OptionModel optionsItem) {
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

  public StringPropertyModel optionsDataSource(@Nullable OptionsDataSourceModel optionsDataSource) {
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

  public StringPropertyModel optionsLoadedDynamically(@Nullable Boolean optionsLoadedDynamically) {
    this.optionsLoadedDynamically = optionsLoadedDynamically;
    return this;
  }

  /**
   * If the property options should be loaded dynamically.
   * @return optionsLoadedDynamically
   */
  
  @Schema(name = "optionsLoadedDynamically", description = "If the property options should be loaded dynamically.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("optionsLoadedDynamically")
  public @Nullable Boolean getOptionsLoadedDynamically() {
    return optionsLoadedDynamically;
  }

  public void setOptionsLoadedDynamically(@Nullable Boolean optionsLoadedDynamically) {
    this.optionsLoadedDynamically = optionsLoadedDynamically;
  }


  public StringPropertyModel controlType(ControlTypeModel controlType) {
    super.controlType(controlType);
    return this;
  }

  public StringPropertyModel label(String label) {
    super.label(label);
    return this;
  }

  public StringPropertyModel placeholder(String placeholder) {
    super.placeholder(placeholder);
    return this;
  }

  public StringPropertyModel advancedOption(Boolean advancedOption) {
    super.advancedOption(advancedOption);
    return this;
  }

  public StringPropertyModel description(String description) {
    super.description(description);
    return this;
  }

  public StringPropertyModel displayCondition(String displayCondition) {
    super.displayCondition(displayCondition);
    return this;
  }

  public StringPropertyModel expressionEnabled(Boolean expressionEnabled) {
    super.expressionEnabled(expressionEnabled);
    return this;
  }

  public StringPropertyModel hidden(Boolean hidden) {
    super.hidden(hidden);
    return this;
  }

  public StringPropertyModel name(String name) {
    super.name(name);
    return this;
  }

  public StringPropertyModel required(Boolean required) {
    super.required(required);
    return this;
  }

  public StringPropertyModel type(PropertyTypeModel type) {
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
    StringPropertyModel stringProperty = (StringPropertyModel) o;
    return Objects.equals(this.languageId, stringProperty.languageId) &&
        Objects.equals(this.defaultValue, stringProperty.defaultValue) &&
        Objects.equals(this.exampleValue, stringProperty.exampleValue) &&
        Objects.equals(this.maxLength, stringProperty.maxLength) &&
        Objects.equals(this.minLength, stringProperty.minLength) &&
        Objects.equals(this.regex, stringProperty.regex) &&
        Objects.equals(this.options, stringProperty.options) &&
        Objects.equals(this.optionsDataSource, stringProperty.optionsDataSource) &&
        Objects.equals(this.optionsLoadedDynamically, stringProperty.optionsLoadedDynamically) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(languageId, defaultValue, exampleValue, maxLength, minLength, regex, options, optionsDataSource, optionsLoadedDynamically, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StringPropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    languageId: ").append(toIndentedString(languageId)).append("\n");
    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
    sb.append("    exampleValue: ").append(toIndentedString(exampleValue)).append("\n");
    sb.append("    maxLength: ").append(toIndentedString(maxLength)).append("\n");
    sb.append("    minLength: ").append(toIndentedString(minLength)).append("\n");
    sb.append("    regex: ").append(toIndentedString(regex)).append("\n");
    sb.append("    options: ").append(toIndentedString(options)).append("\n");
    sb.append("    optionsDataSource: ").append(toIndentedString(optionsDataSource)).append("\n");
    sb.append("    optionsLoadedDynamically: ").append(toIndentedString(optionsLoadedDynamically)).append("\n");
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

