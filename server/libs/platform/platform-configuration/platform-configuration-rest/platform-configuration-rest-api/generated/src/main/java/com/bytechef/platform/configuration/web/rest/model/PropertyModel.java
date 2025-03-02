package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.PropertyTypeModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A base property.
 */

@Schema(name = "Property", description = "A base property.")
@JsonIgnoreProperties(
  value = "type", // ignore manually set type, it will be automatically generated by Jackson during serialization
  allowSetters = true // allows the type to be set during deserialization
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = ArrayPropertyModel.class, name = "ARRAY"),
  @JsonSubTypes.Type(value = BooleanPropertyModel.class, name = "BOOLEAN"),
  @JsonSubTypes.Type(value = DatePropertyModel.class, name = "DATE"),
  @JsonSubTypes.Type(value = DateTimePropertyModel.class, name = "DATE_TIME"),
  @JsonSubTypes.Type(value = DynamicPropertiesPropertyModel.class, name = "DYNAMIC_PROPERTIES"),
  @JsonSubTypes.Type(value = FileEntryPropertyModel.class, name = "FILE_ENTRY"),
  @JsonSubTypes.Type(value = IntegerPropertyModel.class, name = "INTEGER"),
  @JsonSubTypes.Type(value = NullPropertyModel.class, name = "NULL"),
  @JsonSubTypes.Type(value = NumberPropertyModel.class, name = "NUMBER"),
  @JsonSubTypes.Type(value = ObjectPropertyModel.class, name = "OBJECT"),
  @JsonSubTypes.Type(value = StringPropertyModel.class, name = "STRING"),
  @JsonSubTypes.Type(value = TaskPropertyModel.class, name = "TASK"),
  @JsonSubTypes.Type(value = TimePropertyModel.class, name = "TIME"),
  @JsonSubTypes.Type(value = ValuePropertyModel.class, name = "ValueProperty")
})

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-03-10T21:49:31.015728+01:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
public class PropertyModel {

  private Boolean advancedOption = false;

  private @Nullable String description;

  private @Nullable String displayCondition;

  private Boolean expressionEnabled = true;

  private Boolean hidden = false;

  private @Nullable String name;

  private Boolean required = false;

  private PropertyTypeModel type;

  public PropertyModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PropertyModel(PropertyTypeModel type) {
    this.type = type;
  }

  public PropertyModel advancedOption(Boolean advancedOption) {
    this.advancedOption = advancedOption;
    return this;
  }

  /**
   * If the property should be grouped under advanced options.
   * @return advancedOption
   */
  
  @Schema(name = "advancedOption", description = "If the property should be grouped under advanced options.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("advancedOption")
  public Boolean getAdvancedOption() {
    return advancedOption;
  }

  public void setAdvancedOption(Boolean advancedOption) {
    this.advancedOption = advancedOption;
  }

  public PropertyModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The property description.
   * @return description
   */
  
  @Schema(name = "description", description = "The property description.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public PropertyModel displayCondition(String displayCondition) {
    this.displayCondition = displayCondition;
    return this;
  }

  /**
   * Defines rules when the property should be shown or hidden.
   * @return displayCondition
   */
  
  @Schema(name = "displayCondition", description = "Defines rules when the property should be shown or hidden.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("displayCondition")
  public String getDisplayCondition() {
    return displayCondition;
  }

  public void setDisplayCondition(String displayCondition) {
    this.displayCondition = displayCondition;
  }

  public PropertyModel expressionEnabled(Boolean expressionEnabled) {
    this.expressionEnabled = expressionEnabled;
    return this;
  }

  /**
   * Defines if the property can contain expressions or only constant values. Defaults to true.
   * @return expressionEnabled
   */
  
  @Schema(name = "expressionEnabled", description = "Defines if the property can contain expressions or only constant values. Defaults to true.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("expressionEnabled")
  public Boolean getExpressionEnabled() {
    return expressionEnabled;
  }

  public void setExpressionEnabled(Boolean expressionEnabled) {
    this.expressionEnabled = expressionEnabled;
  }

  public PropertyModel hidden(Boolean hidden) {
    this.hidden = hidden;
    return this;
  }

  /**
   * If the property should be visible or not.
   * @return hidden
   */
  
  @Schema(name = "hidden", description = "If the property should be visible or not.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("hidden")
  public Boolean getHidden() {
    return hidden;
  }

  public void setHidden(Boolean hidden) {
    this.hidden = hidden;
  }

  public PropertyModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The property name.
   * @return name
   */
  
  @Schema(name = "name", description = "The property name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public PropertyModel required(Boolean required) {
    this.required = required;
    return this;
  }

  /**
   * If the property is required or not.
   * @return required
   */
  
  @Schema(name = "required", description = "If the property is required or not.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("required")
  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public PropertyModel type(PropertyTypeModel type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull @Valid 
  @Schema(name = "type", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
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
    PropertyModel property = (PropertyModel) o;
    return Objects.equals(this.advancedOption, property.advancedOption) &&
        Objects.equals(this.description, property.description) &&
        Objects.equals(this.displayCondition, property.displayCondition) &&
        Objects.equals(this.expressionEnabled, property.expressionEnabled) &&
        Objects.equals(this.hidden, property.hidden) &&
        Objects.equals(this.name, property.name) &&
        Objects.equals(this.required, property.required) &&
        Objects.equals(this.type, property.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(advancedOption, description, displayCondition, expressionEnabled, hidden, name, required, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PropertyModel {\n");
    sb.append("    advancedOption: ").append(toIndentedString(advancedOption)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    displayCondition: ").append(toIndentedString(displayCondition)).append("\n");
    sb.append("    expressionEnabled: ").append(toIndentedString(expressionEnabled)).append("\n");
    sb.append("    hidden: ").append(toIndentedString(hidden)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    required: ").append(toIndentedString(required)).append("\n");
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

