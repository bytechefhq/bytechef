package com.bytechef.ee.automation.data.table.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
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
 * DeleteRowsResponseModel
 */

@JsonTypeName("DeleteRowsResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-03T11:52:44.578282+02:00[Europe/Zagreb]", comments = "Generator version: 7.24.0")
public class DeleteRowsResponseModel {

  private @Nullable Integer deletedCount;

  private List<Long> deletedIds = new ArrayList<>();

  public DeleteRowsResponseModel deletedCount(@Nullable Integer deletedCount) {
    this.deletedCount = deletedCount;
    return this;
  }

  /**
   * Get deletedCount
   * @return deletedCount
   */
  
  @Schema(name = "deletedCount", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("deletedCount")
  public @Nullable Integer getDeletedCount() {
    return deletedCount;
  }

  @JsonProperty("deletedCount")
  public void setDeletedCount(@Nullable Integer deletedCount) {
    this.deletedCount = deletedCount;
  }

  public DeleteRowsResponseModel deletedIds(List<Long> deletedIds) {
    this.deletedIds = deletedIds;
    return this;
  }

  public DeleteRowsResponseModel addDeletedIdsItem(Long deletedIdsItem) {
    if (this.deletedIds == null) {
      this.deletedIds = new ArrayList<>();
    }
    this.deletedIds.add(deletedIdsItem);
    return this;
  }

  /**
   * Get deletedIds
   * @return deletedIds
   */
  
  @Schema(name = "deletedIds", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("deletedIds")
  public List<Long> getDeletedIds() {
    return deletedIds;
  }

  @JsonProperty("deletedIds")
  public void setDeletedIds(List<Long> deletedIds) {
    this.deletedIds = deletedIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DeleteRowsResponseModel deleteRowsResponse = (DeleteRowsResponseModel) o;
    return Objects.equals(this.deletedCount, deleteRowsResponse.deletedCount) &&
        Objects.equals(this.deletedIds, deleteRowsResponse.deletedIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(deletedCount, deletedIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DeleteRowsResponseModel {\n");
    sb.append("    deletedCount: ").append(toIndentedString(deletedCount)).append("\n");
    sb.append("    deletedIds: ").append(toIndentedString(deletedIds)).append("\n");
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

