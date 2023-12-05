package com.bytechef.hermes.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.configuration.web.rest.model.ControlTypeModel;
import com.bytechef.hermes.configuration.web.rest.model.OptionModel;
import com.bytechef.hermes.configuration.web.rest.model.OptionsDataSourceModel;
import com.bytechef.hermes.configuration.web.rest.model.PropertyModel;
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
 * An array property type.
 */

@Schema(name = "ArrayProperty", description = "An array property type.")

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-12-04T07:58:17.397618+01:00[Europe/Zagreb]")
public class ArrayPropertyModel extends ValuePropertyModel {

  @Valid
  private List<@Valid PropertyModel> items;

  private Boolean multipleValues;

  @Valid
  private List<@Valid OptionModel> options;

  private OptionsDataSourceModel optionsDataSource;

  public ArrayPropertyModel items(List<@Valid PropertyModel> items) {
    this.items = items;
    return this;
  }

  public ArrayPropertyModel addItemsItem(PropertyModel itemsItem) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.add(itemsItem);
    return this;
  }

  /**
   * Types of the array items.
   * @return items
  */
  @Valid 
  @Schema(name = "items", description = "Types of the array items.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("items")
  public List<@Valid PropertyModel> getItems() {
    return items;
  }

  public void setItems(List<@Valid PropertyModel> items) {
    this.items = items;
  }

  public ArrayPropertyModel multipleValues(Boolean multipleValues) {
    this.multipleValues = multipleValues;
    return this;
  }

  /**
   * If the array can contain multiple items.
   * @return multipleValues
  */
  
  @Schema(name = "multipleValues", description = "If the array can contain multiple items.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("multipleValues")
  public Boolean getMultipleValues() {
    return multipleValues;
  }

  public void setMultipleValues(Boolean multipleValues) {
    this.multipleValues = multipleValues;
  }

  public ArrayPropertyModel options(List<@Valid OptionModel> options) {
    this.options = options;
    return this;
  }

  public ArrayPropertyModel addOptionsItem(OptionModel optionsItem) {
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

  public ArrayPropertyModel optionsDataSource(OptionsDataSourceModel optionsDataSource) {
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


  public ArrayPropertyModel controlType(ControlTypeModel controlType) {
    super.controlType(controlType);
    return this;
  }

  public ArrayPropertyModel defaultValue(Object defaultValue) {
    super.defaultValue(defaultValue);
    return this;
  }

  public ArrayPropertyModel exampleValue(Object exampleValue) {
    super.exampleValue(exampleValue);
    return this;
  }

  public ArrayPropertyModel advancedOption(Boolean advancedOption) {
    super.advancedOption(advancedOption);
    return this;
  }

  public ArrayPropertyModel description(String description) {
    super.description(description);
    return this;
  }

  public ArrayPropertyModel displayCondition(String displayCondition) {
    super.displayCondition(displayCondition);
    return this;
  }

  public ArrayPropertyModel expressionEnabled(Boolean expressionEnabled) {
    super.expressionEnabled(expressionEnabled);
    return this;
  }

  public ArrayPropertyModel hidden(Boolean hidden) {
    super.hidden(hidden);
    return this;
  }

  public ArrayPropertyModel label(String label) {
    super.label(label);
    return this;
  }

  public ArrayPropertyModel name(String name) {
    super.name(name);
    return this;
  }

  public ArrayPropertyModel placeholder(String placeholder) {
    super.placeholder(placeholder);
    return this;
  }

  public ArrayPropertyModel required(Boolean required) {
    super.required(required);
    return this;
  }

  public ArrayPropertyModel type(PropertyTypeModel type) {
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
    ArrayPropertyModel arrayProperty = (ArrayPropertyModel) o;
    return Objects.equals(this.items, arrayProperty.items) &&
        Objects.equals(this.multipleValues, arrayProperty.multipleValues) &&
        Objects.equals(this.options, arrayProperty.options) &&
        Objects.equals(this.optionsDataSource, arrayProperty.optionsDataSource) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, multipleValues, options, optionsDataSource, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ArrayPropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    multipleValues: ").append(toIndentedString(multipleValues)).append("\n");
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

