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
 * An array property type.
 */

@Schema(name = "ArrayProperty", description = "An array property type.")
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
  @JsonSubTypes.Type(value = NullPropertyModel.class, name = "NULL"),
  @JsonSubTypes.Type(value = NumberPropertyModel.class, name = "NUMBER"),
  @JsonSubTypes.Type(value = ObjectPropertyModel.class, name = "OBJECT"),
  @JsonSubTypes.Type(value = OneOfPropertyModel.class, name = "ONE_OF"),
  @JsonSubTypes.Type(value = StringPropertyModel.class, name = "STRING")
})

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-02-24T18:32:48.786669+01:00[Europe/Zagreb]")
public class ArrayPropertyModel extends ValuePropertyModel {

  @JsonProperty("items")
  @Valid
  private List<PropertyModel> items = null;

  @JsonProperty("multipleValues")
  private Boolean multipleValues;

  public ArrayPropertyModel items(List<PropertyModel> items) {
    this.items = items;
    return this;
  }

  public ArrayPropertyModel addItemsItem(PropertyModel itemsItem) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.add(itemsItem);
    return this;
  }

  /**
   * Types of the array items.
   * @return items
  */
  @Valid 
  @Schema(name = "items", description = "Types of the array items.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public List<PropertyModel> getItems() {
    return items;
  }

  public void setItems(List<PropertyModel> items) {
    this.items = items;
  }

  public ArrayPropertyModel multipleValues(Boolean multipleValues) {
    this.multipleValues = multipleValues;
    return this;
  }

  /**
   * If the array can contain multiple items.
   * @return multipleValues
  */
  
  @Schema(name = "multipleValues", description = "If the array can contain multiple items.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Boolean getMultipleValues() {
    return multipleValues;
  }

  public void setMultipleValues(Boolean multipleValues) {
    this.multipleValues = multipleValues;
  }

  public ArrayPropertyModel defaultValue(Object defaultValue) {
    super.setDefaultValue(defaultValue);
    return this;
  }

  public ArrayPropertyModel exampleValue(Object exampleValue) {
    super.setExampleValue(exampleValue);
    return this;
  }

  public ArrayPropertyModel options(List<PropertyOptionModel> options) {
    super.setOptions(options);
    return this;
  }

  public ArrayPropertyModel addOptionsItem(PropertyOptionModel optionsItem) {
    super.addOptionsItem(optionsItem);
    return this;
  }

  public ArrayPropertyModel advancedOption(Boolean advancedOption) {
    super.setAdvancedOption(advancedOption);
    return this;
  }

  public ArrayPropertyModel description(String description) {
    super.setDescription(description);
    return this;
  }

  public ArrayPropertyModel displayOption(DisplayOptionModel displayOption) {
    super.setDisplayOption(displayOption);
    return this;
  }

  public ArrayPropertyModel hidden(Boolean hidden) {
    super.setHidden(hidden);
    return this;
  }

  public ArrayPropertyModel label(String label) {
    super.setLabel(label);
    return this;
  }

  public ArrayPropertyModel metadata(Map<String, Object> metadata) {
    super.setMetadata(metadata);
    return this;
  }

  public ArrayPropertyModel putMetadataItem(String key, Object metadataItem) {
    super.putMetadataItem(key, metadataItem);
    return this;
  }

  public ArrayPropertyModel name(String name) {
    super.setName(name);
    return this;
  }

  public ArrayPropertyModel placeholder(String placeholder) {
    super.setPlaceholder(placeholder);
    return this;
  }

  public ArrayPropertyModel required(Boolean required) {
    super.setRequired(required);
    return this;
  }

  public ArrayPropertyModel type(PropertyTypeModel type) {
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
    ArrayPropertyModel arrayProperty = (ArrayPropertyModel) o;
    return Objects.equals(this.items, arrayProperty.items) &&
        Objects.equals(this.multipleValues, arrayProperty.multipleValues) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, multipleValues, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ArrayPropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    multipleValues: ").append(toIndentedString(multipleValues)).append("\n");
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

