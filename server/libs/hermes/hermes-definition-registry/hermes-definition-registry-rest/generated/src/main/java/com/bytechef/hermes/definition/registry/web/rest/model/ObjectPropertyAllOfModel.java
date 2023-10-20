package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.OptionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.OptionsDataSourceModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-08T21:31:51.028205+02:00[Europe/Zagreb]")
public class ObjectPropertyAllOfModel {

  @Valid
  private List<@Valid PropertyModel> additionalProperties;

  private Boolean multipleValues;

  private String objectType;

  @Valid
  private List<@Valid OptionModel> options;

  private OptionsDataSourceModel optionsDataSource;

  @Valid
  private List<@Valid PropertyModel> properties;

  public ObjectPropertyAllOfModel additionalProperties(List<@Valid PropertyModel> additionalProperties) {
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
  @JsonProperty("additionalProperties")
  public List<@Valid PropertyModel> getAdditionalProperties() {
    return additionalProperties;
  }

  public void setAdditionalProperties(List<@Valid PropertyModel> additionalProperties) {
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
  @JsonProperty("multipleValues")
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
  @JsonProperty("objectType")
  public String getObjectType() {
    return objectType;
  }

  public void setObjectType(String objectType) {
    this.objectType = objectType;
  }

  public ObjectPropertyAllOfModel options(List<@Valid OptionModel> options) {
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
  @JsonProperty("options")
  public List<@Valid OptionModel> getOptions() {
    return options;
  }

  public void setOptions(List<@Valid OptionModel> options) {
    this.options = options;
  }

  public ObjectPropertyAllOfModel optionsDataSource(OptionsDataSourceModel optionsDataSource) {
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

  public ObjectPropertyAllOfModel properties(List<@Valid PropertyModel> properties) {
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
  @JsonProperty("properties")
  public List<@Valid PropertyModel> getProperties() {
    return properties;
  }

  public void setProperties(List<@Valid PropertyModel> properties) {
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
        Objects.equals(this.optionsDataSource, objectPropertyAllOf.optionsDataSource) &&
        Objects.equals(this.properties, objectPropertyAllOf.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(additionalProperties, multipleValues, objectType, options, optionsDataSource, properties);
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

