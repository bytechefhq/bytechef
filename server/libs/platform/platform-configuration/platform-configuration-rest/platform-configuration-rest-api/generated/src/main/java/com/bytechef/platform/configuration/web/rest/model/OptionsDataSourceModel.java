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
 * Defines function that should dynamically load options for the property.
 */

@Schema(name = "OptionsDataSource", description = "Defines function that should dynamically load options for the property.")
@JsonTypeName("OptionsDataSource")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-01-19T11:58:57.058637+01:00[Europe/Zagreb]")
public class OptionsDataSourceModel {

  @Valid
  private List<String> loadOptionsDependsOn;

  public OptionsDataSourceModel loadOptionsDependsOn(List<String> loadOptionsDependsOn) {
    this.loadOptionsDependsOn = loadOptionsDependsOn;
    return this;
  }

  public OptionsDataSourceModel addLoadOptionsDependsOnItem(String loadOptionsDependsOnItem) {
    if (this.loadOptionsDependsOn == null) {
      this.loadOptionsDependsOn = new ArrayList<>();
    }
    this.loadOptionsDependsOn.add(loadOptionsDependsOnItem);
    return this;
  }

  /**
   * The list of property names on which value change the property options should load/reload.
   * @return loadOptionsDependsOn
  */
  
  @Schema(name = "loadOptionsDependsOn", description = "The list of property names on which value change the property options should load/reload.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("loadOptionsDependsOn")
  public List<String> getLoadOptionsDependsOn() {
    return loadOptionsDependsOn;
  }

  public void setLoadOptionsDependsOn(List<String> loadOptionsDependsOn) {
    this.loadOptionsDependsOn = loadOptionsDependsOn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OptionsDataSourceModel optionsDataSource = (OptionsDataSourceModel) o;
    return Objects.equals(this.loadOptionsDependsOn, optionsDataSource.loadOptionsDependsOn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(loadOptionsDependsOn);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OptionsDataSourceModel {\n");
    sb.append("    loadOptionsDependsOn: ").append(toIndentedString(loadOptionsDependsOn)).append("\n");
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

