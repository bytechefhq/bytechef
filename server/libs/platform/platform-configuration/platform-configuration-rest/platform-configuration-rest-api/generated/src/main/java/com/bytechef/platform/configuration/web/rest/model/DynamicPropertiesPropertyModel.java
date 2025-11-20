package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.PropertiesDataSourceModel;
import com.bytechef.platform.configuration.web.rest.model.PropertyModel;
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
 * A dynamic properties property type.
 */

@Schema(name = "DynamicPropertiesProperty", description = "A dynamic properties property type.")

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:33.876560+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class DynamicPropertiesPropertyModel extends PropertyModel {

  private @Nullable String header;

  private @Nullable PropertiesDataSourceModel propertiesDataSource;

  public DynamicPropertiesPropertyModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DynamicPropertiesPropertyModel(PropertyTypeModel type) {
    super(type);
  }

  public DynamicPropertiesPropertyModel header(@Nullable String header) {
    this.header = header;
    return this;
  }

  /**
   * The dynamic property header.
   * @return header
   */
  
  @Schema(name = "header", description = "The dynamic property header.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("header")
  public @Nullable String getHeader() {
    return header;
  }

  public void setHeader(@Nullable String header) {
    this.header = header;
  }

  public DynamicPropertiesPropertyModel propertiesDataSource(@Nullable PropertiesDataSourceModel propertiesDataSource) {
    this.propertiesDataSource = propertiesDataSource;
    return this;
  }

  /**
   * Get propertiesDataSource
   * @return propertiesDataSource
   */
  @Valid 
  @Schema(name = "propertiesDataSource", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("propertiesDataSource")
  public @Nullable PropertiesDataSourceModel getPropertiesDataSource() {
    return propertiesDataSource;
  }

  public void setPropertiesDataSource(@Nullable PropertiesDataSourceModel propertiesDataSource) {
    this.propertiesDataSource = propertiesDataSource;
  }


  public DynamicPropertiesPropertyModel advancedOption(Boolean advancedOption) {
    super.advancedOption(advancedOption);
    return this;
  }

  public DynamicPropertiesPropertyModel description(String description) {
    super.description(description);
    return this;
  }

  public DynamicPropertiesPropertyModel displayCondition(String displayCondition) {
    super.displayCondition(displayCondition);
    return this;
  }

  public DynamicPropertiesPropertyModel expressionEnabled(Boolean expressionEnabled) {
    super.expressionEnabled(expressionEnabled);
    return this;
  }

  public DynamicPropertiesPropertyModel hidden(Boolean hidden) {
    super.hidden(hidden);
    return this;
  }

  public DynamicPropertiesPropertyModel name(String name) {
    super.name(name);
    return this;
  }

  public DynamicPropertiesPropertyModel required(Boolean required) {
    super.required(required);
    return this;
  }

  public DynamicPropertiesPropertyModel type(PropertyTypeModel type) {
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
    DynamicPropertiesPropertyModel dynamicPropertiesProperty = (DynamicPropertiesPropertyModel) o;
    return Objects.equals(this.header, dynamicPropertiesProperty.header) &&
        Objects.equals(this.propertiesDataSource, dynamicPropertiesProperty.propertiesDataSource) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(header, propertiesDataSource, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DynamicPropertiesPropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    header: ").append(toIndentedString(header)).append("\n");
    sb.append("    propertiesDataSource: ").append(toIndentedString(propertiesDataSource)).append("\n");
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

