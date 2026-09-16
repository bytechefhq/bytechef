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
 * DataTableRowModel
 */

@JsonTypeName("DataTableRow")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-03T11:52:44.578282+02:00[Europe/Zagreb]", comments = "Generator version: 7.24.0")
public class DataTableRowModel {

  private @Nullable Long id;

  private @Nullable String externalId;

  private Map<String, Object> values = new HashMap<>();

  public DataTableRowModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  
  @Schema(name = "id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public DataTableRowModel externalId(@Nullable String externalId) {
    this.externalId = externalId;
    return this;
  }

  /**
   * The caller-supplied key, or null.
   * @return externalId
   */
  
  @Schema(name = "externalId", description = "The caller-supplied key, or null.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("externalId")
  public @Nullable String getExternalId() {
    return externalId;
  }

  @JsonProperty("externalId")
  public void setExternalId(@Nullable String externalId) {
    this.externalId = externalId;
  }

  public DataTableRowModel values(Map<String, Object> values) {
    this.values = values;
    return this;
  }

  public DataTableRowModel putValuesItem(String key, Object valuesItem) {
    if (this.values == null) {
      this.values = new HashMap<>();
    }
    this.values.put(key, valuesItem);
    return this;
  }

  /**
   * Column name to value. STRING→string, NUMBER→number, INTEGER→integer, BOOLEAN→boolean, DATE→YYYY-MM-DD, DATE_TIME→ISO-8601 UTC.
   * @return values
   */
  
  @Schema(name = "values", description = "Column name to value. STRING→string, NUMBER→number, INTEGER→integer, BOOLEAN→boolean, DATE→YYYY-MM-DD, DATE_TIME→ISO-8601 UTC.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("values")
  public Map<String, Object> getValues() {
    return values;
  }

  @JsonProperty("values")
  public void setValues(Map<String, Object> values) {
    this.values = values;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DataTableRowModel dataTableRow = (DataTableRowModel) o;
    return Objects.equals(this.id, dataTableRow.id) &&
        Objects.equals(this.externalId, dataTableRow.externalId) &&
        Objects.equals(this.values, dataTableRow.values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, externalId, values);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DataTableRowModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    externalId: ").append(toIndentedString(externalId)).append("\n");
    sb.append("    values: ").append(toIndentedString(values)).append("\n");
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

