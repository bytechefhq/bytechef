package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertiesDataSourceModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * DynamicPropertiesPropertyAllOfModel
 */

@JsonTypeName("DynamicPropertiesProperty_allOf")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-16T21:36:57.501651+02:00[Europe/Zagreb]")
public class DynamicPropertiesPropertyAllOfModel {

  private PropertiesDataSourceModel propertiesDataSource;

  public DynamicPropertiesPropertyAllOfModel propertiesDataSource(PropertiesDataSourceModel propertiesDataSource) {
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
  public PropertiesDataSourceModel getPropertiesDataSource() {
    return propertiesDataSource;
  }

  public void setPropertiesDataSource(PropertiesDataSourceModel propertiesDataSource) {
    this.propertiesDataSource = propertiesDataSource;
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
    return Objects.equals(this.propertiesDataSource, dynamicPropertiesPropertyAllOf.propertiesDataSource);
  }

  @Override
  public int hashCode() {
    return Objects.hash(propertiesDataSource);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DynamicPropertiesPropertyAllOfModel {\n");
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

