package com.bytechef.hermes.definition.registry.web.rest.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.media.Schema;


import jakarta.annotation.Generated;

/**
 * DynamicPropertiesPropertyAllOfModel
 */

@JsonTypeName("DynamicPropertiesProperty_allOf")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-12T08:51:58.173412+02:00[Europe/Zagreb]")
public class DynamicPropertiesPropertyAllOfModel {

  private PropertiesDataSourceModel dynamicPropertiesDataSource;

  public DynamicPropertiesPropertyAllOfModel dynamicPropertiesDataSource(PropertiesDataSourceModel dynamicPropertiesDataSource) {
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
  public PropertiesDataSourceModel getDynamicPropertiesDataSource() {
    return dynamicPropertiesDataSource;
  }

  public void setDynamicPropertiesDataSource(PropertiesDataSourceModel dynamicPropertiesDataSource) {
    this.dynamicPropertiesDataSource = dynamicPropertiesDataSource;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DynamicPropertiesPropertyAllOfModel dynamicPropertiesPropertyAllOf = (DynamicPropertiesPropertyAllOfModel) o;
    return Objects.equals(this.dynamicPropertiesDataSource, dynamicPropertiesPropertyAllOf.dynamicPropertiesDataSource);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dynamicPropertiesDataSource);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DynamicPropertiesPropertyAllOfModel {\n");
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

