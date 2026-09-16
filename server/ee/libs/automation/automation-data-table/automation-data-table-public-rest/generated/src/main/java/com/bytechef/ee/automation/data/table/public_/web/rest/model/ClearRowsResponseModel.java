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
 * ClearRowsResponseModel
 */

@JsonTypeName("ClearRowsResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-03T11:52:44.578282+02:00[Europe/Zagreb]", comments = "Generator version: 7.24.0")
public class ClearRowsResponseModel {

  private @Nullable Long deletedCount;

  public ClearRowsResponseModel deletedCount(@Nullable Long deletedCount) {
    this.deletedCount = deletedCount;
    return this;
  }

  /**
   * Get deletedCount
   * @return deletedCount
   */
  
  @Schema(name = "deletedCount", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("deletedCount")
  public @Nullable Long getDeletedCount() {
    return deletedCount;
  }

  @JsonProperty("deletedCount")
  public void setDeletedCount(@Nullable Long deletedCount) {
    this.deletedCount = deletedCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClearRowsResponseModel clearRowsResponse = (ClearRowsResponseModel) o;
    return Objects.equals(this.deletedCount, clearRowsResponse.deletedCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(deletedCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClearRowsResponseModel {\n");
    sb.append("    deletedCount: ").append(toIndentedString(deletedCount)).append("\n");
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

