package com.bytechef.hermes.component.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.component.web.rest.model.DisplayOptionModel;
import com.bytechef.hermes.component.web.rest.model.PropertyTypeModel;
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
 * ValuePropertyModel
 */

@JsonTypeName("ValueProperty")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-27T16:22:37.776273+02:00[Europe/Zagreb]")
public class ValuePropertyModel {

  @JsonProperty("defaultValue")
  private Object defaultValue;

  @JsonProperty("exampleValue")
  private Object exampleValue;

  @JsonProperty("loadOptionsDependsOn")
  @Valid
  private List<String> loadOptionsDependsOn = null;

  @JsonProperty("loadOptionsMethod")
  private String loadOptionsMethod;

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

  public ValuePropertyModel defaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  /**
   * Get defaultValue
   * @return defaultValue
  */
  
  @Schema(name = "defaultValue", required = false)
  public Object getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
  }

  public ValuePropertyModel exampleValue(Object exampleValue) {
    this.exampleValue = exampleValue;
    return this;
  }

  /**
   * Get exampleValue
   * @return exampleValue
  */
  
  @Schema(name = "exampleValue", required = false)
  public Object getExampleValue() {
    return exampleValue;
  }

  public void setExampleValue(Object exampleValue) {
    this.exampleValue = exampleValue;
  }

  public ValuePropertyModel loadOptionsDependsOn(List<String> loadOptionsDependsOn) {
    this.loadOptionsDependsOn = loadOptionsDependsOn;
    return this;
  }

  public ValuePropertyModel addLoadOptionsDependsOnItem(String loadOptionsDependsOnItem) {
    if (this.loadOptionsDependsOn == null) {
      this.loadOptionsDependsOn = new ArrayList<>();
    }
    this.loadOptionsDependsOn.add(loadOptionsDependsOnItem);
    return this;
  }

  /**
   * Get loadOptionsDependsOn
   * @return loadOptionsDependsOn
  */
  
  @Schema(name = "loadOptionsDependsOn", required = false)
  public List<String> getLoadOptionsDependsOn() {
    return loadOptionsDependsOn;
  }

  public void setLoadOptionsDependsOn(List<String> loadOptionsDependsOn) {
    this.loadOptionsDependsOn = loadOptionsDependsOn;
  }

  public ValuePropertyModel loadOptionsMethod(String loadOptionsMethod) {
    this.loadOptionsMethod = loadOptionsMethod;
    return this;
  }

  /**
   * Get loadOptionsMethod
   * @return loadOptionsMethod
  */
  
  @Schema(name = "loadOptionsMethod", required = false)
  public String getLoadOptionsMethod() {
    return loadOptionsMethod;
  }

  public void setLoadOptionsMethod(String loadOptionsMethod) {
    this.loadOptionsMethod = loadOptionsMethod;
  }

  public ValuePropertyModel description(String description) {
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

  public ValuePropertyModel displayOption(DisplayOptionModel displayOption) {
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

  public ValuePropertyModel label(String label) {
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

  public ValuePropertyModel name(String name) {
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

  public ValuePropertyModel placeholder(String placeholder) {
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

  public ValuePropertyModel type(PropertyTypeModel type) {
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
    ValuePropertyModel valueProperty = (ValuePropertyModel) o;
    return Objects.equals(this.defaultValue, valueProperty.defaultValue) &&
        Objects.equals(this.exampleValue, valueProperty.exampleValue) &&
        Objects.equals(this.loadOptionsDependsOn, valueProperty.loadOptionsDependsOn) &&
        Objects.equals(this.loadOptionsMethod, valueProperty.loadOptionsMethod) &&
        Objects.equals(this.description, valueProperty.description) &&
        Objects.equals(this.displayOption, valueProperty.displayOption) &&
        Objects.equals(this.label, valueProperty.label) &&
        Objects.equals(this.name, valueProperty.name) &&
        Objects.equals(this.placeholder, valueProperty.placeholder) &&
        Objects.equals(this.type, valueProperty.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaultValue, exampleValue, loadOptionsDependsOn, loadOptionsMethod, description, displayOption, label, name, placeholder, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ValuePropertyModel {\n");
    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
    sb.append("    exampleValue: ").append(toIndentedString(exampleValue)).append("\n");
    sb.append("    loadOptionsDependsOn: ").append(toIndentedString(loadOptionsDependsOn)).append("\n");
    sb.append("    loadOptionsMethod: ").append(toIndentedString(loadOptionsMethod)).append("\n");
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

