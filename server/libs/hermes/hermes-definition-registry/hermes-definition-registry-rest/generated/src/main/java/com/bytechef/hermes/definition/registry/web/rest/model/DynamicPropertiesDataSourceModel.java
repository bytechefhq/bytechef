package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
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
 * Defines function that should load properties.
 */

@Schema(name = "DynamicPropertiesDataSource", description = "Defines function that should load properties.")
@JsonTypeName("DynamicPropertiesDataSource")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-10T17:56:08.674559+02:00[Europe/Zagreb]")
public class DynamicPropertiesDataSourceModel {

  @Valid
  private List<String> loadPropertiesDependsOn;

  public DynamicPropertiesDataSourceModel loadPropertiesDependsOn(List<String> loadPropertiesDependsOn) {
    this.loadPropertiesDependsOn = loadPropertiesDependsOn;
    return this;
  }

  public DynamicPropertiesDataSourceModel addLoadPropertiesDependsOnItem(String loadPropertiesDependsOnItem) {
    if (this.loadPropertiesDependsOn == null) {
      this.loadPropertiesDependsOn = new ArrayList<>();
    }
    this.loadPropertiesDependsOn.add(loadPropertiesDependsOnItem);
    return this;
  }

  /**
   * The list of property names on which value change the properties should load/reload.
   * @return loadPropertiesDependsOn
  */
  
  @Schema(name = "loadPropertiesDependsOn", description = "The list of property names on which value change the properties should load/reload.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("loadPropertiesDependsOn")
  public List<String> getLoadPropertiesDependsOn() {
    return loadPropertiesDependsOn;
  }

  public void setLoadPropertiesDependsOn(List<String> loadPropertiesDependsOn) {
    this.loadPropertiesDependsOn = loadPropertiesDependsOn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DynamicPropertiesDataSourceModel dynamicPropertiesDataSource = (DynamicPropertiesDataSourceModel) o;
    return Objects.equals(this.loadPropertiesDependsOn, dynamicPropertiesDataSource.loadPropertiesDependsOn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(loadPropertiesDependsOn);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DynamicPropertiesDataSourceModel {\n");
    sb.append("    loadPropertiesDependsOn: ").append(toIndentedString(loadPropertiesDependsOn)).append("\n");
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

