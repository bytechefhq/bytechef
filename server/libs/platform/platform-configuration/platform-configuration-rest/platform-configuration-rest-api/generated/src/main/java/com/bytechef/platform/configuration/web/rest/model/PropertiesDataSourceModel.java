package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
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

@Schema(name = "PropertiesDataSource", description = "Defines function that should load properties.")
@JsonTypeName("PropertiesDataSource")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-02-02T18:49:47.325391+01:00[Europe/Zagreb]")
public class PropertiesDataSourceModel {

  @Valid
  private List<String> loadPropertiesDependsOn;

  public PropertiesDataSourceModel loadPropertiesDependsOn(List<String> loadPropertiesDependsOn) {
    this.loadPropertiesDependsOn = loadPropertiesDependsOn;
    return this;
  }

  public PropertiesDataSourceModel addLoadPropertiesDependsOnItem(String loadPropertiesDependsOnItem) {
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
    PropertiesDataSourceModel propertiesDataSource = (PropertiesDataSourceModel) o;
    return Objects.equals(this.loadPropertiesDependsOn, propertiesDataSource.loadPropertiesDependsOn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(loadPropertiesDependsOn);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PropertiesDataSourceModel {\n");
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

