package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-15T09:52:48.574632+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class PropertiesDataSourceModel {

  @Valid
  private List<String> propertiesLookupDependsOn = new ArrayList<>();

  public PropertiesDataSourceModel propertiesLookupDependsOn(List<String> propertiesLookupDependsOn) {
    this.propertiesLookupDependsOn = propertiesLookupDependsOn;
    return this;
  }

  public PropertiesDataSourceModel addPropertiesLookupDependsOnItem(String propertiesLookupDependsOnItem) {
    if (this.propertiesLookupDependsOn == null) {
      this.propertiesLookupDependsOn = new ArrayList<>();
    }
    this.propertiesLookupDependsOn.add(propertiesLookupDependsOnItem);
    return this;
  }

  /**
   * The list of property names on which value change the properties should load/reload.
   * @return propertiesLookupDependsOn
   */
  
  @Schema(name = "propertiesLookupDependsOn", description = "The list of property names on which value change the properties should load/reload.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("propertiesLookupDependsOn")
  public List<String> getPropertiesLookupDependsOn() {
    return propertiesLookupDependsOn;
  }

  public void setPropertiesLookupDependsOn(List<String> propertiesLookupDependsOn) {
    this.propertiesLookupDependsOn = propertiesLookupDependsOn;
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
    return Objects.equals(this.propertiesLookupDependsOn, propertiesDataSource.propertiesLookupDependsOn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(propertiesLookupDependsOn);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PropertiesDataSourceModel {\n");
    sb.append("    propertiesLookupDependsOn: ").append(toIndentedString(propertiesLookupDependsOn)).append("\n");
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

