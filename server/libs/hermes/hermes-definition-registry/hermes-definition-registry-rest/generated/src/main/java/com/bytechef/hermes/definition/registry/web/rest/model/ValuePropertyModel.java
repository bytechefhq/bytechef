package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.ControlTypeModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyTypeModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A base property for all value based properties.
 */

@Schema(name = "ValueProperty", description = "A base property for all value based properties.")
@JsonIgnoreProperties(
  value = "type", // ignore manually set type, it will be automatically generated by Jackson during serialization
  allowSetters = true // allows the type to be set during deserialization
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AnyPropertyModel.class, name = "ANY"),
  @JsonSubTypes.Type(value = ArrayPropertyModel.class, name = "ARRAY"),
  @JsonSubTypes.Type(value = BooleanPropertyModel.class, name = "BOOLEAN"),
  @JsonSubTypes.Type(value = DatePropertyModel.class, name = "DATE"),
  @JsonSubTypes.Type(value = DateTimePropertyModel.class, name = "DATE_TIME"),
  @JsonSubTypes.Type(value = DynamicPropertiesPropertyModel.class, name = "DYNAMIC_PROPERTIES"),
  @JsonSubTypes.Type(value = IntegerPropertyModel.class, name = "INTEGER"),
  @JsonSubTypes.Type(value = NullPropertyModel.class, name = "NULL"),
  @JsonSubTypes.Type(value = NumberPropertyModel.class, name = "NUMBER"),
  @JsonSubTypes.Type(value = ObjectPropertyModel.class, name = "OBJECT"),
  @JsonSubTypes.Type(value = StringPropertyModel.class, name = "STRING"),
  @JsonSubTypes.Type(value = TimePropertyModel.class, name = "TIME")
})

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-06-07T12:23:14.195693+02:00[Europe/Zagreb]")
public class ValuePropertyModel extends PropertyModel {

  private ControlTypeModel controlType;

  private Object defaultValue;

  private Object exampleValue;

  public ValuePropertyModel controlType(ControlTypeModel controlType) {
    this.controlType = controlType;
    return this;
  }

  /**
   * Get controlType
   * @return controlType
  */
  @Valid 
  @Schema(name = "controlType", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("controlType")
  public ControlTypeModel getControlType() {
    return controlType;
  }

  public void setControlType(ControlTypeModel controlType) {
    this.controlType = controlType;
  }

  public ValuePropertyModel defaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  /**
   * The property default value.
   * @return defaultValue
  */
  
  @Schema(name = "defaultValue", description = "The property default value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("defaultValue")
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
   * The property sample value.
   * @return exampleValue
  */
  
  @Schema(name = "exampleValue", description = "The property sample value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("exampleValue")
  public Object getExampleValue() {
    return exampleValue;
  }

  public void setExampleValue(Object exampleValue) {
    this.exampleValue = exampleValue;
  }

  public ValuePropertyModel advancedOption(Boolean advancedOption) {
    super.setAdvancedOption(advancedOption);
    return this;
  }

  public ValuePropertyModel description(String description) {
    super.setDescription(description);
    return this;
  }

  public ValuePropertyModel displayCondition(String displayCondition) {
    super.setDisplayCondition(displayCondition);
    return this;
  }

  public ValuePropertyModel expressionEnabled(Boolean expressionEnabled) {
    super.setExpressionEnabled(expressionEnabled);
    return this;
  }

  public ValuePropertyModel hidden(Boolean hidden) {
    super.setHidden(hidden);
    return this;
  }

  public ValuePropertyModel label(String label) {
    super.setLabel(label);
    return this;
  }

  public ValuePropertyModel name(String name) {
    super.setName(name);
    return this;
  }

  public ValuePropertyModel placeholder(String placeholder) {
    super.setPlaceholder(placeholder);
    return this;
  }

  public ValuePropertyModel required(Boolean required) {
    super.setRequired(required);
    return this;
  }

  public ValuePropertyModel type(PropertyTypeModel type) {
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
    ValuePropertyModel valueProperty = (ValuePropertyModel) o;
    return Objects.equals(this.controlType, valueProperty.controlType) &&
        Objects.equals(this.defaultValue, valueProperty.defaultValue) &&
        Objects.equals(this.exampleValue, valueProperty.exampleValue) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(controlType, defaultValue, exampleValue, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ValuePropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    controlType: ").append(toIndentedString(controlType)).append("\n");
    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
    sb.append("    exampleValue: ").append(toIndentedString(exampleValue)).append("\n");
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

