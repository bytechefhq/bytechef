package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.ControlTypeModel;
import com.bytechef.hermes.definition.registry.web.rest.model.OptionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.OptionsDataSourceModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyTypeModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ValuePropertyModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.List;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-27T10:19:33.855801+02:00[Europe/Zagreb]")
public class TimePropertyModel extends ValuePropertyModel {

  @Valid
  private List<@Valid OptionModel> options;

  private OptionsDataSourceModel optionsDataSource;

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

  public TimePropertyModel optionsDataSource(OptionsDataSourceModel optionsDataSource) {
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

  public TimePropertyModel controlType(ControlTypeModel controlType) {
    super.setControlType(controlType);
    return this;
  }

  public TimePropertyModel defaultValue(Object defaultValue) {
    super.setDefaultValue(defaultValue);
    return this;
  }

  public TimePropertyModel exampleValue(Object exampleValue) {
    super.setExampleValue(exampleValue);
    return this;
  }

  public TimePropertyModel advancedOption(Boolean advancedOption) {
    super.setAdvancedOption(advancedOption);
    return this;
  }

  public TimePropertyModel description(String description) {
    super.setDescription(description);
    return this;
  }

  public TimePropertyModel displayCondition(String displayCondition) {
    super.setDisplayCondition(displayCondition);
    return this;
  }

  public TimePropertyModel expressionEnabled(Boolean expressionEnabled) {
    super.setExpressionEnabled(expressionEnabled);
    return this;
  }

  public TimePropertyModel hidden(Boolean hidden) {
    super.setHidden(hidden);
    return this;
  }

  public TimePropertyModel label(String label) {
    super.setLabel(label);
    return this;
  }

  public TimePropertyModel name(String name) {
    super.setName(name);
    return this;
  }

  public TimePropertyModel placeholder(String placeholder) {
    super.setPlaceholder(placeholder);
    return this;
  }

  public TimePropertyModel required(Boolean required) {
    super.setRequired(required);
    return this;
  }

  public TimePropertyModel type(PropertyTypeModel type) {
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
    TimePropertyModel timeProperty = (TimePropertyModel) o;
    return Objects.equals(this.options, timeProperty.options) &&
        Objects.equals(this.optionsDataSource, timeProperty.optionsDataSource) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(options, optionsDataSource, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TimePropertyModel {\n");
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

