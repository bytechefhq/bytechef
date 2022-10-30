package com.bytechef.hermes.task.dispatcher.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.task.dispatcher.web.rest.model.DisplayOptionModel;
import com.bytechef.hermes.task.dispatcher.web.rest.model.PropertyTypeModel;
import com.bytechef.hermes.task.dispatcher.web.rest.model.TypePropertyModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * ArrayPropertyModel
 */

@JsonTypeName("ArrayProperty")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-27T16:28:51.543539+02:00[Europe/Zagreb]")
public class ArrayPropertyModel implements TaskDispatcherDefinitionInputsInnerModel {

  @JsonProperty("defaultValue")
  @Valid
  private List<Object> defaultValue = null;

  @JsonProperty("items")
  @Valid
  private List<TypePropertyModel> items = null;

  @JsonProperty("exampleValue")
  @Valid
  private List<Object> exampleValue = null;

  @JsonProperty("description")
  private String description;

  @JsonProperty("displayOption")
  private DisplayOptionModel displayOption;

  @JsonProperty("label")
  private String label;

  @JsonProperty("name")
  private String name;

  @JsonProperty("placeholder")
  private String placeholder;

  @JsonProperty("type")
  private PropertyTypeModel type;

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
   * Get defaultValue
   * @return defaultValue
  */
  
  @Schema(name = "defaultValue", required = false)
  public List<Object> getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(List<Object> defaultValue) {
    this.defaultValue = defaultValue;
  }

  public ArrayPropertyModel items(List<TypePropertyModel> items) {
    this.items = items;
    return this;
  }

  public ArrayPropertyModel addItemsItem(TypePropertyModel itemsItem) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.add(itemsItem);
    return this;
  }

  /**
   * Get items
   * @return items
  */
  @Valid 
  @Schema(name = "items", required = false)
  public List<TypePropertyModel> getItems() {
    return items;
  }

  public void setItems(List<TypePropertyModel> items) {
    this.items = items;
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
   * Get exampleValue
   * @return exampleValue
  */
  
  @Schema(name = "exampleValue", required = false)
  public List<Object> getExampleValue() {
    return exampleValue;
  }

  public void setExampleValue(List<Object> exampleValue) {
    this.exampleValue = exampleValue;
  }

  public ArrayPropertyModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
  */
  
  @Schema(name = "description", required = false)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ArrayPropertyModel displayOption(DisplayOptionModel displayOption) {
    this.displayOption = displayOption;
    return this;
  }

  /**
   * Get displayOption
   * @return displayOption
  */
  @Valid 
  @Schema(name = "displayOption", required = false)
  public DisplayOptionModel getDisplayOption() {
    return displayOption;
  }

  public void setDisplayOption(DisplayOptionModel displayOption) {
    this.displayOption = displayOption;
  }

  public ArrayPropertyModel label(String label) {
    this.label = label;
    return this;
  }

  /**
   * Get label
   * @return label
  */
  
  @Schema(name = "label", required = false)
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public ArrayPropertyModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
  */
  
  @Schema(name = "name", required = false)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ArrayPropertyModel placeholder(String placeholder) {
    this.placeholder = placeholder;
    return this;
  }

  /**
   * Get placeholder
   * @return placeholder
  */
  
  @Schema(name = "placeholder", required = false)
  public String getPlaceholder() {
    return placeholder;
  }

  public void setPlaceholder(String placeholder) {
    this.placeholder = placeholder;
  }

  public ArrayPropertyModel type(PropertyTypeModel type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
  */
  @Valid 
  @Schema(name = "type", required = false)
  public PropertyTypeModel getType() {
    return type;
  }

  public void setType(PropertyTypeModel type) {
    this.type = type;
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
        Objects.equals(this.items, arrayProperty.items) &&
        Objects.equals(this.exampleValue, arrayProperty.exampleValue) &&
        Objects.equals(this.description, arrayProperty.description) &&
        Objects.equals(this.displayOption, arrayProperty.displayOption) &&
        Objects.equals(this.label, arrayProperty.label) &&
        Objects.equals(this.name, arrayProperty.name) &&
        Objects.equals(this.placeholder, arrayProperty.placeholder) &&
        Objects.equals(this.type, arrayProperty.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaultValue, items, exampleValue, description, displayOption, label, name, placeholder, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ArrayPropertyModel {\n");
    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    exampleValue: ").append(toIndentedString(exampleValue)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    displayOption: ").append(toIndentedString(displayOption)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    placeholder: ").append(toIndentedString(placeholder)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
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

