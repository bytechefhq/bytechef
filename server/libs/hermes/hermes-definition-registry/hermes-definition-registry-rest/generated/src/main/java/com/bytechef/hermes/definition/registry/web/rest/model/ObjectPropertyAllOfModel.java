package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.OptionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ObjectPropertyAllOfModel
 */

@JsonTypeName("ObjectProperty_allOf")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-05T16:27:34.189599+01:00[Europe/Zagreb]")
public class ObjectPropertyAllOfModel {

  @JsonProperty("additionalProperties")
  @Valid
  private List<PropertyModel> additionalProperties = null;

  @JsonProperty("multipleValues")
  private Boolean multipleValues;

  @JsonProperty("objectType")
  private String objectType;

  @JsonProperty("options")
  @Valid
  private List<OptionModel> options = null;

  @JsonProperty("optionsDataSource")
  private JsonNullable<Object> optionsDataSource = JsonNullable.undefined();

  @JsonProperty("properties")
  @Valid
  private List<PropertyModel> properties = null;

  public ObjectPropertyAllOfModel additionalProperties(List<PropertyModel> additionalProperties) {
    this.additionalProperties = additionalProperties;
    return this;
  }

  public ObjectPropertyAllOfModel addAdditionalPropertiesItem(PropertyModel additionalPropertiesItem) {
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

  public ObjectPropertyAllOfModel multipleValues(Boolean multipleValues) {
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

  public ObjectPropertyAllOfModel objectType(String objectType) {
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

  public ObjectPropertyAllOfModel options(List<OptionModel> options) {
    this.options = options;
    return this;
  }

  public ObjectPropertyAllOfModel addOptionsItem(OptionModel optionsItem) {
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
  public List<OptionModel> getOptions() {
    return options;
  }

  public void setOptions(List<OptionModel> options) {
    this.options = options;
  }

  public ObjectPropertyAllOfModel optionsDataSource(Object optionsDataSource) {
    this.optionsDataSource = JsonNullable.of(optionsDataSource);
    return this;
  }

  /**
   * Get optionsDataSource
   * @return optionsDataSource
  */
  
  @Schema(name = "optionsDataSource", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public JsonNullable<Object> getOptionsDataSource() {
    return optionsDataSource;
  }

  public void setOptionsDataSource(JsonNullable<Object> optionsDataSource) {
    this.optionsDataSource = optionsDataSource;
  }

  public ObjectPropertyAllOfModel properties(List<PropertyModel> properties) {
    this.properties = properties;
    return this;
  }

  public ObjectPropertyAllOfModel addPropertiesItem(PropertyModel propertiesItem) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ObjectPropertyAllOfModel objectPropertyAllOf = (ObjectPropertyAllOfModel) o;
    return Objects.equals(this.additionalProperties, objectPropertyAllOf.additionalProperties) &&
        Objects.equals(this.multipleValues, objectPropertyAllOf.multipleValues) &&
        Objects.equals(this.objectType, objectPropertyAllOf.objectType) &&
        Objects.equals(this.options, objectPropertyAllOf.options) &&
        equalsNullable(this.optionsDataSource, objectPropertyAllOf.optionsDataSource) &&
        Objects.equals(this.properties, objectPropertyAllOf.properties);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(additionalProperties, multipleValues, objectType, options, hashCodeNullable(optionsDataSource), properties);
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ObjectPropertyAllOfModel {\n");
    sb.append("    additionalProperties: ").append(toIndentedString(additionalProperties)).append("\n");
    sb.append("    multipleValues: ").append(toIndentedString(multipleValues)).append("\n");
    sb.append("    objectType: ").append(toIndentedString(objectType)).append("\n");
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

