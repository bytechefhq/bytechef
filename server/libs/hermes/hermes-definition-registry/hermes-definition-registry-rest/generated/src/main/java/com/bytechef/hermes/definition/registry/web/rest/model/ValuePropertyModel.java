package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.ArrayPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.BooleanPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.DatePropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.DateTimePropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.DisplayOptionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.IntegerPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.NullPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.NumberPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ObjectPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.OneOfPropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyOptionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.StringPropertyModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
 * A base property for all value based properties.
 */

@Schema(name = "ValueProperty", description = "A base property for all value based properties.")
@JsonIgnoreProperties(
  value = "type", // ignore manually set type, it will be automatically generated by Jackson during serialization
  allowSetters = true // allows the type to be set during deserialization
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = ArrayPropertyModel.class, name = "ARRAY"),
  @JsonSubTypes.Type(value = ArrayPropertyModel.class, name = "ArrayProperty"),
  @JsonSubTypes.Type(value = BooleanPropertyModel.class, name = "BOOLEAN"),
  @JsonSubTypes.Type(value = BooleanPropertyModel.class, name = "BooleanProperty"),
  @JsonSubTypes.Type(value = DatePropertyModel.class, name = "DATE"),
  @JsonSubTypes.Type(value = DateTimePropertyModel.class, name = "DATE_TIME"),
  @JsonSubTypes.Type(value = DatePropertyModel.class, name = "DateProperty"),
  @JsonSubTypes.Type(value = DateTimePropertyModel.class, name = "DateTimeProperty"),
  @JsonSubTypes.Type(value = IntegerPropertyModel.class, name = "INTEGER"),
  @JsonSubTypes.Type(value = IntegerPropertyModel.class, name = "IntegerProperty"),
  @JsonSubTypes.Type(value = NullPropertyModel.class, name = "NULL"),
  @JsonSubTypes.Type(value = NumberPropertyModel.class, name = "NUMBER"),
  @JsonSubTypes.Type(value = NumberPropertyModel.class, name = "NumberProperty"),
  @JsonSubTypes.Type(value = ObjectPropertyModel.class, name = "OBJECT"),
  @JsonSubTypes.Type(value = OneOfPropertyModel.class, name = "ONE_OF"),
  @JsonSubTypes.Type(value = ObjectPropertyModel.class, name = "ObjectProperty"),
  @JsonSubTypes.Type(value = StringPropertyModel.class, name = "STRING"),
  @JsonSubTypes.Type(value = StringPropertyModel.class, name = "StringProperty")
})

@JsonTypeName("ValueProperty")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-02-20T11:29:33.968820+01:00[Europe/Zagreb]")
public class ValuePropertyModel extends PropertyModel {

  @JsonProperty("defaultValue")
  private Object defaultValue;

  @JsonProperty("exampleValue")
  private Object exampleValue;

  @JsonProperty("options")
  @Valid
  private List<PropertyOptionModel> options = null;

  public ValuePropertyModel defaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  /**
   * The property default value.
   * @return defaultValue
  */
  
  @Schema(name = "defaultValue", description = "The property default value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
   * The property example value.
   * @return exampleValue
  */
  
  @Schema(name = "exampleValue", description = "The property example value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Object getExampleValue() {
    return exampleValue;
  }

  public void setExampleValue(Object exampleValue) {
    this.exampleValue = exampleValue;
  }

  public ValuePropertyModel options(List<PropertyOptionModel> options) {
    this.options = options;
    return this;
  }

  public ValuePropertyModel addOptionsItem(PropertyOptionModel optionsItem) {
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
  public List<PropertyOptionModel> getOptions() {
    return options;
  }

  public void setOptions(List<PropertyOptionModel> options) {
    this.options = options;
  }

  public ValuePropertyModel advancedOption(Boolean advancedOption) {
    super.setAdvancedOption(advancedOption);
    return this;
  }

  public ValuePropertyModel description(String description) {
    super.setDescription(description);
    return this;
  }

  public ValuePropertyModel displayOption(DisplayOptionModel displayOption) {
    super.setDisplayOption(displayOption);
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

  public ValuePropertyModel metadata(Map<String, Object> metadata) {
    super.setMetadata(metadata);
    return this;
  }

  public ValuePropertyModel putMetadataItem(String key, Object metadataItem) {
    super.putMetadataItem(key, metadataItem);
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
        Objects.equals(this.options, valueProperty.options) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaultValue, exampleValue, options, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ValuePropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
    sb.append("    exampleValue: ").append(toIndentedString(exampleValue)).append("\n");
    sb.append("    options: ").append(toIndentedString(options)).append("\n");
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

