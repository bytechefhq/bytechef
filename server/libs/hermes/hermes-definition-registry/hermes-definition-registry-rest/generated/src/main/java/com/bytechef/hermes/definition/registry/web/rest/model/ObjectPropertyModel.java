package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.ArrayPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.BooleanPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.DatePropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.DateTimePropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.DisplayOptionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.IntegerPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.NumberPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ObjectPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.OneOfPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyOptionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyTypeModel;
import com.bytechef.hermes.definition.registry.web.rest.model.StringPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ValuePropertyModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
@JsonIgnoreProperties(
  value = "__model_type", // ignore manually set __model_type, it will be automatically generated by Jackson during serialization
  allowSetters = true // allows the __model_type to be set during deserialization
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "__model_type", visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = ArrayPropertyModel.class, name = "ARRAY"),
  @JsonSubTypes.Type(value = BooleanPropertyModel.class, name = "BOOLEAN"),
  @JsonSubTypes.Type(value = DatePropertyModel.class, name = "DATE"),
  @JsonSubTypes.Type(value = DateTimePropertyModel.class, name = "DATE_TIME"),
  @JsonSubTypes.Type(value = IntegerPropertyModel.class, name = "INTEGER"),
  @JsonSubTypes.Type(value = NumberPropertyModel.class, name = "NUMBER"),
  @JsonSubTypes.Type(value = ObjectPropertyModel.class, name = "OBJECT"),
  @JsonSubTypes.Type(value = OneOfPropertyModel.class, name = "ONE_OF"),
  @JsonSubTypes.Type(value = StringPropertyModel.class, name = "STRING")
})

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-02T18:38:21.432374+01:00[Europe/Zagreb]")
public class ObjectPropertyModel extends ValuePropertyModel {

  @JsonProperty("additionalProperties")
  @Valid
  private List<PropertyModel> additionalProperties = null;

  @JsonProperty("multipleValues")
  private Boolean multipleValues;

  @JsonProperty("objectType")
  private String objectType;

  @JsonProperty("properties")
  @Valid
  private List<PropertyModel> properties = null;

  public ObjectPropertyModel additionalProperties(List<PropertyModel> additionalProperties) {
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
  public List<PropertyModel> getAdditionalProperties() {
    return additionalProperties;
  }

  public void setAdditionalProperties(List<PropertyModel> additionalProperties) {
    this.additionalProperties = additionalProperties;
  }

  public ObjectPropertyModel multipleValues(Boolean multipleValues) {
    this.multipleValues = multipleValues;
    return this;
  }

  /**
   * If the object can contain multiple additional properties.
   * @return multipleValues
  */
  
  @Schema(name = "multipleValues", description = "If the object can contain multiple additional properties.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Boolean getMultipleValues() {
    return multipleValues;
  }

  public void setMultipleValues(Boolean multipleValues) {
    this.multipleValues = multipleValues;
  }

  public ObjectPropertyModel objectType(String objectType) {
    this.objectType = objectType;
    return this;
  }

  /**
   * The object type.
   * @return objectType
  */
  
  @Schema(name = "objectType", description = "The object type.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getObjectType() {
    return objectType;
  }

  public void setObjectType(String objectType) {
    this.objectType = objectType;
  }

  public ObjectPropertyModel properties(List<PropertyModel> properties) {
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
  public List<PropertyModel> getProperties() {
    return properties;
  }

  public void setProperties(List<PropertyModel> properties) {
    this.properties = properties;
  }

  public ObjectPropertyModel defaultValue(Object defaultValue) {
    super.setDefaultValue(defaultValue);
    return this;
  }

  public ObjectPropertyModel exampleValue(Object exampleValue) {
    super.setExampleValue(exampleValue);
    return this;
  }

  public ObjectPropertyModel options(List<PropertyOptionModel> options) {
    super.setOptions(options);
    return this;
  }

  public ObjectPropertyModel addOptionsItem(PropertyOptionModel optionsItem) {
    super.addOptionsItem(optionsItem);
    return this;
  }

  public ObjectPropertyModel advancedOption(Boolean advancedOption) {
    super.setAdvancedOption(advancedOption);
    return this;
  }

  public ObjectPropertyModel description(String description) {
    super.setDescription(description);
    return this;
  }

  public ObjectPropertyModel displayOption(DisplayOptionModel displayOption) {
    super.setDisplayOption(displayOption);
    return this;
  }

  public ObjectPropertyModel hidden(Boolean hidden) {
    super.setHidden(hidden);
    return this;
  }

  public ObjectPropertyModel label(String label) {
    super.setLabel(label);
    return this;
  }

  public ObjectPropertyModel metadata(Map<String, Object> metadata) {
    super.setMetadata(metadata);
    return this;
  }

  public ObjectPropertyModel putMetadataItem(String key, Object metadataItem) {
    super.putMetadataItem(key, metadataItem);
    return this;
  }

  public ObjectPropertyModel name(String name) {
    super.setName(name);
    return this;
  }

  public ObjectPropertyModel placeholder(String placeholder) {
    super.setPlaceholder(placeholder);
    return this;
  }

  public ObjectPropertyModel required(Boolean required) {
    super.setRequired(required);
    return this;
  }

  public ObjectPropertyModel type(PropertyTypeModel type) {
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
    ObjectPropertyModel objectProperty = (ObjectPropertyModel) o;
    return Objects.equals(this.additionalProperties, objectProperty.additionalProperties) &&
        Objects.equals(this.multipleValues, objectProperty.multipleValues) &&
        Objects.equals(this.objectType, objectProperty.objectType) &&
        Objects.equals(this.properties, objectProperty.properties) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(additionalProperties, multipleValues, objectType, properties, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ObjectPropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    additionalProperties: ").append(toIndentedString(additionalProperties)).append("\n");
    sb.append("    multipleValues: ").append(toIndentedString(multipleValues)).append("\n");
    sb.append("    objectType: ").append(toIndentedString(objectType)).append("\n");
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

