package com.bytechef.ee.automation.data.table.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ImportRowsResponseModel
 */

@JsonTypeName("ImportRowsResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-03T11:52:44.578282+02:00[Europe/Zagreb]", comments = "Generator version: 7.24.0")
public class ImportRowsResponseModel {

  private @Nullable Integer importedCount;

  public ImportRowsResponseModel importedCount(@Nullable Integer importedCount) {
    this.importedCount = importedCount;
    return this;
  }

  /**
   * Get importedCount
   * @return importedCount
   */
  
  @Schema(name = "importedCount", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("importedCount")
  public @Nullable Integer getImportedCount() {
    return importedCount;
  }

  @JsonProperty("importedCount")
  public void setImportedCount(@Nullable Integer importedCount) {
    this.importedCount = importedCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ImportRowsResponseModel importRowsResponse = (ImportRowsResponseModel) o;
    return Objects.equals(this.importedCount, importRowsResponse.importedCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(importedCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ImportRowsResponseModel {\n");
    sb.append("    importedCount: ").append(toIndentedString(importedCount)).append("\n");
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

