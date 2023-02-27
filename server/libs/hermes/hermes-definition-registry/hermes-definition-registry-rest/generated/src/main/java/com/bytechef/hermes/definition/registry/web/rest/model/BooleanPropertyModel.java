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
 * A boolean property type.
 */

@Schema(name = "BooleanProperty", description = "A boolean property type.")
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-02-27T08:02:46.343401+01:00[Europe/Zagreb]")
public class BooleanPropertyModel extends ValuePropertyModel {

  public BooleanPropertyModel defaultValue(Object defaultValue) {
    super.setDefaultValue(defaultValue);
    return this;
  }

  public BooleanPropertyModel exampleValue(Object exampleValue) {
    super.setExampleValue(exampleValue);
    return this;
  }

  public BooleanPropertyModel options(List<PropertyOptionModel> options) {
    super.setOptions(options);
    return this;
  }

  public BooleanPropertyModel addOptionsItem(PropertyOptionModel optionsItem) {
    super.addOptionsItem(optionsItem);
    return this;
  }

  public BooleanPropertyModel advancedOption(Boolean advancedOption) {
    super.setAdvancedOption(advancedOption);
    return this;
  }

  public BooleanPropertyModel description(String description) {
    super.setDescription(description);
    return this;
  }

  public BooleanPropertyModel displayOption(DisplayOptionModel displayOption) {
    super.setDisplayOption(displayOption);
    return this;
  }

  public BooleanPropertyModel hidden(Boolean hidden) {
    super.setHidden(hidden);
    return this;
  }

  public BooleanPropertyModel label(String label) {
    super.setLabel(label);
    return this;
  }

  public BooleanPropertyModel metadata(Map<String, Object> metadata) {
    super.setMetadata(metadata);
    return this;
  }

  public BooleanPropertyModel putMetadataItem(String key, Object metadataItem) {
    super.putMetadataItem(key, metadataItem);
    return this;
  }

  public BooleanPropertyModel name(String name) {
    super.setName(name);
    return this;
  }

  public BooleanPropertyModel placeholder(String placeholder) {
    super.setPlaceholder(placeholder);
    return this;
  }

  public BooleanPropertyModel required(Boolean required) {
    super.setRequired(required);
    return this;
  }

  public BooleanPropertyModel type(PropertyTypeModel type) {
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
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BooleanPropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
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

