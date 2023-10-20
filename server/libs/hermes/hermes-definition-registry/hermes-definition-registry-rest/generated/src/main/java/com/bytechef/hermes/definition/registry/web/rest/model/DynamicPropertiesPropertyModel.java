package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.DynamicPropertiesDataSourceModel;
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
 * A dynamic properties property type.
 */

@Schema(name = "DynamicPropertiesProperty", description = "A dynamic properties property type.")

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-08T21:31:51.028205+02:00[Europe/Zagreb]")
public class DynamicPropertiesPropertyModel extends PropertyModel {

  private DynamicPropertiesDataSourceModel dynamicPropertiesDataSource;

  public DynamicPropertiesPropertyModel dynamicPropertiesDataSource(DynamicPropertiesDataSourceModel dynamicPropertiesDataSource) {
    this.dynamicPropertiesDataSource = dynamicPropertiesDataSource;
    return this;
  }

  /**
   * Get dynamicPropertiesDataSource
   * @return dynamicPropertiesDataSource
  */
  @Valid 
  @Schema(name = "dynamicPropertiesDataSource", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("dynamicPropertiesDataSource")
  public DynamicPropertiesDataSourceModel getDynamicPropertiesDataSource() {
    return dynamicPropertiesDataSource;
  }

  public void setDynamicPropertiesDataSource(DynamicPropertiesDataSourceModel dynamicPropertiesDataSource) {
    this.dynamicPropertiesDataSource = dynamicPropertiesDataSource;
  }

  public DynamicPropertiesPropertyModel advancedOption(Boolean advancedOption) {
    super.setAdvancedOption(advancedOption);
    return this;
  }

  public DynamicPropertiesPropertyModel description(String description) {
    super.setDescription(description);
    return this;
  }

  public DynamicPropertiesPropertyModel displayCondition(String displayCondition) {
    super.setDisplayCondition(displayCondition);
    return this;
  }

  public DynamicPropertiesPropertyModel expressionDisabled(Boolean expressionDisabled) {
    super.setExpressionDisabled(expressionDisabled);
    return this;
  }

  public DynamicPropertiesPropertyModel hidden(Boolean hidden) {
    super.setHidden(hidden);
    return this;
  }

  public DynamicPropertiesPropertyModel label(String label) {
    super.setLabel(label);
    return this;
  }

  public DynamicPropertiesPropertyModel name(String name) {
    super.setName(name);
    return this;
  }

  public DynamicPropertiesPropertyModel placeholder(String placeholder) {
    super.setPlaceholder(placeholder);
    return this;
  }

  public DynamicPropertiesPropertyModel required(Boolean required) {
    super.setRequired(required);
    return this;
  }

  public DynamicPropertiesPropertyModel type(PropertyTypeModel type) {
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
    DynamicPropertiesPropertyModel dynamicPropertiesProperty = (DynamicPropertiesPropertyModel) o;
    return Objects.equals(this.dynamicPropertiesDataSource, dynamicPropertiesProperty.dynamicPropertiesDataSource) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dynamicPropertiesDataSource, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DynamicPropertiesPropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    dynamicPropertiesDataSource: ").append(toIndentedString(dynamicPropertiesDataSource)).append("\n");
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

