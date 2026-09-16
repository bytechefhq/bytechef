package com.bytechef.ee.automation.data.table.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CreateRowRequestModel
 */

@JsonTypeName("CreateRowRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-03T11:52:44.578282+02:00[Europe/Zagreb]", comments = "Generator version: 7.24.0")
public class CreateRowRequestModel {

  private Map<String, Object> values = new HashMap<>();

  private @Nullable String externalId;

  public CreateRowRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateRowRequestModel(Map<String, Object> values) {
    this.values = values;
  }

  public CreateRowRequestModel values(Map<String, Object> values) {
    this.values = values;
    return this;
  }

  public CreateRowRequestModel putValuesItem(String key, Object valuesItem) {
    if (this.values == null) {
      this.values = new HashMap<>();
    }
    this.values.put(key, valuesItem);
    return this;
  }

  /**
   * Column name to value. Each value may also be given as a string in the column's format.
   * @return values
   */
  @NotNull 
  @Schema(name = "values", description = "Column name to value. Each value may also be given as a string in the column's format.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("values")
  public Map<String, Object> getValues() {
    return values;
  }

  @JsonProperty("values")
  public void setValues(Map<String, Object> values) {
    this.values = values;
  }

  public CreateRowRequestModel externalId(@Nullable String externalId) {
    this.externalId = externalId;
    return this;
  }

  /**
   * Get externalId
   * @return externalId
   */
  @Size(max = 255) 
  @Schema(name = "externalId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("externalId")
  public @Nullable String getExternalId() {
    return externalId;
  }

  @JsonProperty("externalId")
  public void setExternalId(@Nullable String externalId) {
    this.externalId = externalId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateRowRequestModel createRowRequest = (CreateRowRequestModel) o;
    return Objects.equals(this.values, createRowRequest.values) &&
        Objects.equals(this.externalId, createRowRequest.externalId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(values, externalId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateRowRequestModel {\n");
    sb.append("    values: ").append(toIndentedString(values)).append("\n");
    sb.append("    externalId: ").append(toIndentedString(externalId)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

