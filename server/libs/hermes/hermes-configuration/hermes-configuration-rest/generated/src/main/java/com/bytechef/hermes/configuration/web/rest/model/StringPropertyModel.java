package com.bytechef.hermes.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.configuration.web.rest.model.ControlTypeModel;
import com.bytechef.hermes.configuration.web.rest.model.OptionModel;
import com.bytechef.hermes.configuration.web.rest.model.OptionsDataSourceModel;
import com.bytechef.hermes.configuration.web.rest.model.PropertyTypeModel;
import com.bytechef.hermes.configuration.web.rest.model.ValuePropertyModel;
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
 * A string property.
 */

@Schema(name = "StringProperty", description = "A string property.")

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-11-21T21:27:09.819358+01:00[Europe/Zagreb]")
public class StringPropertyModel extends ValuePropertyModel {

  @Valid
  private List<@Valid OptionModel> options;

  private OptionsDataSourceModel optionsDataSource;

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

  public StringPropertyModel optionsDataSource(OptionsDataSourceModel optionsDataSource) {
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


  public StringPropertyModel controlType(ControlTypeModel controlType) {
    super.controlType(controlType);
    return this;
  }

  public StringPropertyModel defaultValue(Object defaultValue) {
    super.defaultValue(defaultValue);
    return this;
  }

  public StringPropertyModel exampleValue(Object exampleValue) {
    super.exampleValue(exampleValue);
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

  public StringPropertyModel label(String label) {
    super.label(label);
    return this;
  }

  public StringPropertyModel name(String name) {
    super.name(name);
    return this;
  }

  public StringPropertyModel placeholder(String placeholder) {
    super.placeholder(placeholder);
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
    return Objects.equals(this.options, stringProperty.options) &&
        Objects.equals(this.optionsDataSource, stringProperty.optionsDataSource) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(options, optionsDataSource, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StringPropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
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

