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
 * ObjectPropertyModel
 */

@JsonTypeName("ObjectProperty")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-27T16:28:51.543539+02:00[Europe/Zagreb]")
public class ObjectPropertyModel implements TaskDispatcherDefinitionInputsInnerModel {

  @JsonProperty("additionalProperties")
  private Boolean additionalProperties;

  @JsonProperty("properties")
  @Valid
  private List<TypePropertyModel> properties = null;

  @JsonProperty("exampleValue")
  private Object exampleValue;

  @JsonProperty("defaultValue")
  private Object defaultValue;

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

  public ObjectPropertyModel additionalProperties(Boolean additionalProperties) {
    this.additionalProperties = additionalProperties;
    return this;
  }

  /**
   * Get additionalProperties
   * @return additionalProperties
  */
  
  @Schema(name = "additionalProperties", required = false)
  public Boolean getAdditionalProperties() {
    return additionalProperties;
  }

  public void setAdditionalProperties(Boolean additionalProperties) {
    this.additionalProperties = additionalProperties;
  }

  public ObjectPropertyModel properties(List<TypePropertyModel> properties) {
    this.properties = properties;
    return this;
  }

  public ObjectPropertyModel addPropertiesItem(TypePropertyModel propertiesItem) {
    if (this.properties == null) {
      this.properties = new ArrayList<>();
    }
    this.properties.add(propertiesItem);
    return this;
  }

  /**
   * Get properties
   * @return properties
  */
  @Valid 
  @Schema(name = "properties", required = false)
  public List<TypePropertyModel> getProperties() {
    return properties;
  }

  public void setProperties(List<TypePropertyModel> properties) {
    this.properties = properties;
  }

  public ObjectPropertyModel exampleValue(Object exampleValue) {
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

  public ObjectPropertyModel defaultValue(Object defaultValue) {
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

  public ObjectPropertyModel description(String description) {
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

  public ObjectPropertyModel displayOption(DisplayOptionModel displayOption) {
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

  public ObjectPropertyModel label(String label) {
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

  public ObjectPropertyModel name(String name) {
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

  public ObjectPropertyModel placeholder(String placeholder) {
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

  public ObjectPropertyModel type(PropertyTypeModel type) {
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
    ObjectPropertyModel objectProperty = (ObjectPropertyModel) o;
    return Objects.equals(this.additionalProperties, objectProperty.additionalProperties) &&
        Objects.equals(this.properties, objectProperty.properties) &&
        Objects.equals(this.exampleValue, objectProperty.exampleValue) &&
        Objects.equals(this.defaultValue, objectProperty.defaultValue) &&
        Objects.equals(this.description, objectProperty.description) &&
        Objects.equals(this.displayOption, objectProperty.displayOption) &&
        Objects.equals(this.label, objectProperty.label) &&
        Objects.equals(this.name, objectProperty.name) &&
        Objects.equals(this.placeholder, objectProperty.placeholder) &&
        Objects.equals(this.type, objectProperty.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(additionalProperties, properties, exampleValue, defaultValue, description, displayOption, label, name, placeholder, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ObjectPropertyModel {\n");
    sb.append("    additionalProperties: ").append(toIndentedString(additionalProperties)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
    sb.append("    exampleValue: ").append(toIndentedString(exampleValue)).append("\n");
    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
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

