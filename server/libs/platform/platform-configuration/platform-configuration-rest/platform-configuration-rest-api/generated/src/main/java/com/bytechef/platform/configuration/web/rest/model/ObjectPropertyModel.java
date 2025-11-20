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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * An object property type.
 */

@Schema(name = "ObjectProperty", description = "An object property type.")

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:33.876560+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class ObjectPropertyModel extends ValuePropertyModel {

  @Valid
  private List<@Valid PropertyModel> additionalProperties = new ArrayList<>();

  @Valid
  private Map<String, Object> defaultValue = new HashMap<>();

  @Valid
  private Map<String, Object> exampleValue = new HashMap<>();

  private @Nullable Boolean multipleValues;

  @Valid
  private List<@Valid OptionModel> options = new ArrayList<>();

  private @Nullable OptionsDataSourceModel optionsDataSource;

  @Valid
  private List<@Valid PropertyModel> properties = new ArrayList<>();

  public ObjectPropertyModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ObjectPropertyModel(PropertyTypeModel type, ControlTypeModel controlType) {
    super(controlType, type);
  }

  public ObjectPropertyModel additionalProperties(List<@Valid PropertyModel> additionalProperties) {
    this.additionalProperties = additionalProperties;
    return this;
  }

  public ObjectPropertyModel addAdditionalPropertiesItem(PropertyModel additionalPropertiesItem) {
    if (this.additionalProperties == null) {
      this.additionalProperties = new ArrayList<>();
    }
    this.additionalProperties.add(additionalPropertiesItem);
    return this;
  }

  /**
   * Types of dynamically defined properties.
   * @return additionalProperties
   */
  @Valid 
  @Schema(name = "additionalProperties", description = "Types of dynamically defined properties.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("additionalProperties")
  public List<@Valid PropertyModel> getAdditionalProperties() {
    return additionalProperties;
  }

  public void setAdditionalProperties(List<@Valid PropertyModel> additionalProperties) {
    this.additionalProperties = additionalProperties;
  }

  public ObjectPropertyModel defaultValue(Map<String, Object> defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  public ObjectPropertyModel putDefaultValueItem(String key, Object defaultValueItem) {
    if (this.defaultValue == null) {
      this.defaultValue = new HashMap<>();
    }
    this.defaultValue.put(key, defaultValueItem);
    return this;
  }

  /**
   * The property default value.
   * @return defaultValue
   */
  
  @Schema(name = "defaultValue", description = "The property default value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("defaultValue")
  public Map<String, Object> getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(Map<String, Object> defaultValue) {
    this.defaultValue = defaultValue;
  }

  public ObjectPropertyModel exampleValue(Map<String, Object> exampleValue) {
    this.exampleValue = exampleValue;
    return this;
  }

  public ObjectPropertyModel putExampleValueItem(String key, Object exampleValueItem) {
    if (this.exampleValue == null) {
      this.exampleValue = new HashMap<>();
    }
    this.exampleValue.put(key, exampleValueItem);
    return this;
  }

  /**
   * The property sample value.
   * @return exampleValue
   */
  
  @Schema(name = "exampleValue", description = "The property sample value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("exampleValue")
  public Map<String, Object> getExampleValue() {
    return exampleValue;
  }

  public void setExampleValue(Map<String, Object> exampleValue) {
    this.exampleValue = exampleValue;
  }

  public ObjectPropertyModel multipleValues(@Nullable Boolean multipleValues) {
    this.multipleValues = multipleValues;
    return this;
  }

  /**
   * If the object can contain multiple additional properties.
   * @return multipleValues
   */
  
  @Schema(name = "multipleValues", description = "If the object can contain multiple additional properties.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("multipleValues")
  public @Nullable Boolean getMultipleValues() {
    return multipleValues;
  }

  public void setMultipleValues(@Nullable Boolean multipleValues) {
    this.multipleValues = multipleValues;
  }

  public ObjectPropertyModel options(List<@Valid OptionModel> options) {
    this.options = options;
    return this;
  }

  public ObjectPropertyModel addOptionsItem(OptionModel optionsItem) {
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

  public ObjectPropertyModel optionsDataSource(@Nullable OptionsDataSourceModel optionsDataSource) {
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

  public ObjectPropertyModel properties(List<@Valid PropertyModel> properties) {
    this.properties = properties;
    return this;
  }

  public ObjectPropertyModel addPropertiesItem(PropertyModel propertiesItem) {
    if (this.properties == null) {
      this.properties = new ArrayList<>();
    }
    this.properties.add(propertiesItem);
    return this;
  }

  /**
   * The list of valid object property types.
   * @return properties
   */
  @Valid 
  @Schema(name = "properties", description = "The list of valid object property types.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("properties")
  public List<@Valid PropertyModel> getProperties() {
    return properties;
  }

  public void setProperties(List<@Valid PropertyModel> properties) {
    this.properties = properties;
  }


  public ObjectPropertyModel controlType(ControlTypeModel controlType) {
    super.controlType(controlType);
    return this;
  }

  public ObjectPropertyModel label(String label) {
    super.label(label);
    return this;
  }

  public ObjectPropertyModel placeholder(String placeholder) {
    super.placeholder(placeholder);
    return this;
  }

  public ObjectPropertyModel advancedOption(Boolean advancedOption) {
    super.advancedOption(advancedOption);
    return this;
  }

  public ObjectPropertyModel description(String description) {
    super.description(description);
    return this;
  }

  public ObjectPropertyModel displayCondition(String displayCondition) {
    super.displayCondition(displayCondition);
    return this;
  }

  public ObjectPropertyModel expressionEnabled(Boolean expressionEnabled) {
    super.expressionEnabled(expressionEnabled);
    return this;
  }

  public ObjectPropertyModel hidden(Boolean hidden) {
    super.hidden(hidden);
    return this;
  }

  public ObjectPropertyModel name(String name) {
    super.name(name);
    return this;
  }

  public ObjectPropertyModel required(Boolean required) {
    super.required(required);
    return this;
  }

  public ObjectPropertyModel type(PropertyTypeModel type) {
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
    ObjectPropertyModel objectProperty = (ObjectPropertyModel) o;
    return Objects.equals(this.additionalProperties, objectProperty.additionalProperties) &&
        Objects.equals(this.defaultValue, objectProperty.defaultValue) &&
        Objects.equals(this.exampleValue, objectProperty.exampleValue) &&
        Objects.equals(this.multipleValues, objectProperty.multipleValues) &&
        Objects.equals(this.options, objectProperty.options) &&
        Objects.equals(this.optionsDataSource, objectProperty.optionsDataSource) &&
        Objects.equals(this.properties, objectProperty.properties) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(additionalProperties, defaultValue, exampleValue, multipleValues, options, optionsDataSource, properties, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ObjectPropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    additionalProperties: ").append(toIndentedString(additionalProperties)).append("\n");
    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
    sb.append("    exampleValue: ").append(toIndentedString(exampleValue)).append("\n");
    sb.append("    multipleValues: ").append(toIndentedString(multipleValues)).append("\n");
    sb.append("    options: ").append(toIndentedString(options)).append("\n");
    sb.append("    optionsDataSource: ").append(toIndentedString(optionsDataSource)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
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

