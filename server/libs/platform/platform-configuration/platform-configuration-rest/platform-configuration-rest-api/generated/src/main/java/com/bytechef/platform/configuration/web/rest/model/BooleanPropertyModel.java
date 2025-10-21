package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.ControlTypeModel;
import com.bytechef.platform.configuration.web.rest.model.OptionModel;
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
 * A boolean property type.
 */

@Schema(name = "BooleanProperty", description = "A boolean property type.")

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-21T12:06:41.161145+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public class BooleanPropertyModel extends ValuePropertyModel {

  private @Nullable Boolean defaultValue;

  private @Nullable Boolean exampleValue;

  @Valid
  private List<@Valid OptionModel> options = new ArrayList<>();

  public BooleanPropertyModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BooleanPropertyModel(PropertyTypeModel type, ControlTypeModel controlType) {
    super(controlType, type);
  }

  public BooleanPropertyModel defaultValue(@Nullable Boolean defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  /**
   * The property default value.
   * @return defaultValue
   */
  
  @Schema(name = "defaultValue", description = "The property default value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("defaultValue")
  public @Nullable Boolean getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(@Nullable Boolean defaultValue) {
    this.defaultValue = defaultValue;
  }

  public BooleanPropertyModel exampleValue(@Nullable Boolean exampleValue) {
    this.exampleValue = exampleValue;
    return this;
  }

  /**
   * The property sample value.
   * @return exampleValue
   */
  
  @Schema(name = "exampleValue", description = "The property sample value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("exampleValue")
  public @Nullable Boolean getExampleValue() {
    return exampleValue;
  }

  public void setExampleValue(@Nullable Boolean exampleValue) {
    this.exampleValue = exampleValue;
  }

  public BooleanPropertyModel options(List<@Valid OptionModel> options) {
    this.options = options;
    return this;
  }

  public BooleanPropertyModel addOptionsItem(OptionModel optionsItem) {
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


  public BooleanPropertyModel controlType(ControlTypeModel controlType) {
    super.controlType(controlType);
    return this;
  }

  public BooleanPropertyModel label(String label) {
    super.label(label);
    return this;
  }

  public BooleanPropertyModel placeholder(String placeholder) {
    super.placeholder(placeholder);
    return this;
  }

  public BooleanPropertyModel advancedOption(Boolean advancedOption) {
    super.advancedOption(advancedOption);
    return this;
  }

  public BooleanPropertyModel description(String description) {
    super.description(description);
    return this;
  }

  public BooleanPropertyModel displayCondition(String displayCondition) {
    super.displayCondition(displayCondition);
    return this;
  }

  public BooleanPropertyModel expressionEnabled(Boolean expressionEnabled) {
    super.expressionEnabled(expressionEnabled);
    return this;
  }

  public BooleanPropertyModel hidden(Boolean hidden) {
    super.hidden(hidden);
    return this;
  }

  public BooleanPropertyModel name(String name) {
    super.name(name);
    return this;
  }

  public BooleanPropertyModel required(Boolean required) {
    super.required(required);
    return this;
  }

  public BooleanPropertyModel type(PropertyTypeModel type) {
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
    BooleanPropertyModel booleanProperty = (BooleanPropertyModel) o;
    return Objects.equals(this.defaultValue, booleanProperty.defaultValue) &&
        Objects.equals(this.exampleValue, booleanProperty.exampleValue) &&
        Objects.equals(this.options, booleanProperty.options) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaultValue, exampleValue, options, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BooleanPropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
    sb.append("    exampleValue: ").append(toIndentedString(exampleValue)).append("\n");
    sb.append("    options: ").append(toIndentedString(options)).append("\n");
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

