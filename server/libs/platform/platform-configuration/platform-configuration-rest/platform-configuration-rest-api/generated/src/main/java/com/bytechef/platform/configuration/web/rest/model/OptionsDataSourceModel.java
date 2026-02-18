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
 * Defines function that should dynamically load options for the property.
 */

@Schema(name = "OptionsDataSource", description = "Defines function that should dynamically load options for the property.")
@JsonTypeName("OptionsDataSource")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-19T05:53:32.886377+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public class OptionsDataSourceModel {

  @Valid
  private List<String> optionsLookupDependsOn = new ArrayList<>();

  public OptionsDataSourceModel optionsLookupDependsOn(List<String> optionsLookupDependsOn) {
    this.optionsLookupDependsOn = optionsLookupDependsOn;
    return this;
  }

  public OptionsDataSourceModel addOptionsLookupDependsOnItem(String optionsLookupDependsOnItem) {
    if (this.optionsLookupDependsOn == null) {
      this.optionsLookupDependsOn = new ArrayList<>();
    }
    this.optionsLookupDependsOn.add(optionsLookupDependsOnItem);
    return this;
  }

  /**
   * The list of property names on which value change the property options should load/reload.
   * @return optionsLookupDependsOn
   */
  
  @Schema(name = "optionsLookupDependsOn", description = "The list of property names on which value change the property options should load/reload.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("optionsLookupDependsOn")
  public List<String> getOptionsLookupDependsOn() {
    return optionsLookupDependsOn;
  }

  public void setOptionsLookupDependsOn(List<String> optionsLookupDependsOn) {
    this.optionsLookupDependsOn = optionsLookupDependsOn;
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
    return Objects.equals(this.optionsLookupDependsOn, optionsDataSource.optionsLookupDependsOn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(optionsLookupDependsOn);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OptionsDataSourceModel {\n");
    sb.append("    optionsLookupDependsOn: ").append(toIndentedString(optionsLookupDependsOn)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

