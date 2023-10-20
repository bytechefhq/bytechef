package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyTypeModel;
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
 * A one of property type.
 */

@Schema(name = "OneOfProperty", description = "A one of property type.")

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-25T07:55:32.360326+02:00[Europe/Zagreb]")
public class OneOfPropertyModel extends PropertyModel {

  @Valid
  private List<@Valid PropertyModel> types;

  public OneOfPropertyModel types(List<@Valid PropertyModel> types) {
    this.types = types;
    return this;
  }

  public OneOfPropertyModel addTypesItem(PropertyModel typesItem) {
    if (this.types == null) {
      this.types = new ArrayList<>();
    }
    this.types.add(typesItem);
    return this;
  }

  /**
   * Possible types of properties that can be used.
   * @return types
  */
  @Valid 
  @Schema(name = "types", description = "Possible types of properties that can be used.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("types")
  public List<@Valid PropertyModel> getTypes() {
    return types;
  }

  public void setTypes(List<@Valid PropertyModel> types) {
    this.types = types;
  }

  public OneOfPropertyModel advancedOption(Boolean advancedOption) {
    super.setAdvancedOption(advancedOption);
    return this;
  }

  public OneOfPropertyModel description(String description) {
    super.setDescription(description);
    return this;
  }

  public OneOfPropertyModel displayCondition(String displayCondition) {
    super.setDisplayCondition(displayCondition);
    return this;
  }

  public OneOfPropertyModel expressionDisabled(Boolean expressionDisabled) {
    super.setExpressionDisabled(expressionDisabled);
    return this;
  }

  public OneOfPropertyModel hidden(Boolean hidden) {
    super.setHidden(hidden);
    return this;
  }

  public OneOfPropertyModel label(String label) {
    super.setLabel(label);
    return this;
  }

  public OneOfPropertyModel metadata(Map<String, Object> metadata) {
    super.setMetadata(metadata);
    return this;
  }

  public OneOfPropertyModel putMetadataItem(String key, Object metadataItem) {
    super.putMetadataItem(key, metadataItem);
    return this;
  }

  public OneOfPropertyModel name(String name) {
    super.setName(name);
    return this;
  }

  public OneOfPropertyModel placeholder(String placeholder) {
    super.setPlaceholder(placeholder);
    return this;
  }

  public OneOfPropertyModel required(Boolean required) {
    super.setRequired(required);
    return this;
  }

  public OneOfPropertyModel type(PropertyTypeModel type) {
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
    OneOfPropertyModel oneOfProperty = (OneOfPropertyModel) o;
    return Objects.equals(this.types, oneOfProperty.types) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(types, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OneOfPropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    types: ").append(toIndentedString(types)).append("\n");
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

