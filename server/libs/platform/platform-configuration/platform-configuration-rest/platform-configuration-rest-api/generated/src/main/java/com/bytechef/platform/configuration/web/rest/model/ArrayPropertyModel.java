package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.ControlTypeModel;
import com.bytechef.platform.configuration.web.rest.model.OptionModel;
import com.bytechef.platform.configuration.web.rest.model.OptionsDataSourceModel;
import com.bytechef.platform.configuration.web.rest.model.PropertyModel;
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
 * An array property type.
 */

@Schema(name = "ArrayProperty", description = "An array property type.")

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-22T13:57:27.830042+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class ArrayPropertyModel extends ValuePropertyModel {

  @Valid
  private List<Object> defaultValue = new ArrayList<>();

  @Valid
  private List<Object> exampleValue = new ArrayList<>();

  @Valid
  private List<@Valid PropertyModel> items = new ArrayList<>();

  private @Nullable Long maxItems;

  private @Nullable Long minItems;

  private @Nullable Boolean multipleValues;

  @Valid
  private List<@Valid OptionModel> options = new ArrayList<>();

  private @Nullable OptionsDataSourceModel optionsDataSource;

  public ArrayPropertyModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ArrayPropertyModel(PropertyTypeModel type, ControlTypeModel controlType) {
    super(controlType, type);
  }

  public ArrayPropertyModel defaultValue(List<Object> defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  public ArrayPropertyModel addDefaultValueItem(Object defaultValueItem) {
    if (this.defaultValue == null) {
      this.defaultValue = new ArrayList<>();
    }
    this.defaultValue.add(defaultValueItem);
    return this;
  }

  /**
   * The property default value.
   * @return defaultValue
   */
  
  @Schema(name = "defaultValue", description = "The property default value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("defaultValue")
  public List<Object> getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(List<Object> defaultValue) {
    this.defaultValue = defaultValue;
  }

  public ArrayPropertyModel exampleValue(List<Object> exampleValue) {
    this.exampleValue = exampleValue;
    return this;
  }

  public ArrayPropertyModel addExampleValueItem(Object exampleValueItem) {
    if (this.exampleValue == null) {
      this.exampleValue = new ArrayList<>();
    }
    this.exampleValue.add(exampleValueItem);
    return this;
  }

  /**
   * The property sample value.
   * @return exampleValue
   */
  
  @Schema(name = "exampleValue", description = "The property sample value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("exampleValue")
  public List<Object> getExampleValue() {
    return exampleValue;
  }

  public void setExampleValue(List<Object> exampleValue) {
    this.exampleValue = exampleValue;
  }

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

  public ArrayPropertyModel maxItems(@Nullable Long maxItems) {
    this.maxItems = maxItems;
    return this;
  }

  /**
   * Get maxItems
   * @return maxItems
   */
  
  @Schema(name = "maxItems", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("maxItems")
  public @Nullable Long getMaxItems() {
    return maxItems;
  }

  public void setMaxItems(@Nullable Long maxItems) {
    this.maxItems = maxItems;
  }

  public ArrayPropertyModel minItems(@Nullable Long minItems) {
    this.minItems = minItems;
    return this;
  }

  /**
   * Get minItems
   * @return minItems
   */
  
  @Schema(name = "minItems", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("minItems")
  public @Nullable Long getMinItems() {
    return minItems;
  }

  public void setMinItems(@Nullable Long minItems) {
    this.minItems = minItems;
  }

  public ArrayPropertyModel multipleValues(@Nullable Boolean multipleValues) {
    this.multipleValues = multipleValues;
    return this;
  }

  /**
   * If the array can contain multiple items.
   * @return multipleValues
   */
  
  @Schema(name = "multipleValues", description = "If the array can contain multiple items.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("multipleValues")
  public @Nullable Boolean getMultipleValues() {
    return multipleValues;
  }

  public void setMultipleValues(@Nullable Boolean multipleValues) {
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

  public ArrayPropertyModel optionsDataSource(@Nullable OptionsDataSourceModel optionsDataSource) {
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


  public ArrayPropertyModel controlType(ControlTypeModel controlType) {
    super.controlType(controlType);
    return this;
  }

  public ArrayPropertyModel label(String label) {
    super.label(label);
    return this;
  }

  public ArrayPropertyModel placeholder(String placeholder) {
    super.placeholder(placeholder);
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

  public ArrayPropertyModel name(String name) {
    super.name(name);
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
    return Objects.equals(this.defaultValue, arrayProperty.defaultValue) &&
        Objects.equals(this.exampleValue, arrayProperty.exampleValue) &&
        Objects.equals(this.items, arrayProperty.items) &&
        Objects.equals(this.maxItems, arrayProperty.maxItems) &&
        Objects.equals(this.minItems, arrayProperty.minItems) &&
        Objects.equals(this.multipleValues, arrayProperty.multipleValues) &&
        Objects.equals(this.options, arrayProperty.options) &&
        Objects.equals(this.optionsDataSource, arrayProperty.optionsDataSource) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaultValue, exampleValue, items, maxItems, minItems, multipleValues, options, optionsDataSource, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ArrayPropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
    sb.append("    exampleValue: ").append(toIndentedString(exampleValue)).append("\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    maxItems: ").append(toIndentedString(maxItems)).append("\n");
    sb.append("    minItems: ").append(toIndentedString(minItems)).append("\n");
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

