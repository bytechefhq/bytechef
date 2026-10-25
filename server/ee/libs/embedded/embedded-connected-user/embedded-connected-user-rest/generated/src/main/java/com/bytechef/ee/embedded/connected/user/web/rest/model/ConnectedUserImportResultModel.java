package com.bytechef.ee.embedded.connected.user.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.connected.user.web.rest.model.ConnectedUserImportRejectedRowModel;
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
 * ConnectedUserImportResultModel
 */

@JsonTypeName("ConnectedUserImportResult")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-23T23:44:29.402755+02:00[Europe/Zagreb]", comments = "Generator version: 7.25.0")
public class ConnectedUserImportResultModel {

  private @Nullable Integer createdCount;

  private List<@Valid ConnectedUserImportRejectedRowModel> rejectedRows = new ArrayList<>();

  private List<String> skippedExternalIds = new ArrayList<>();

  public ConnectedUserImportResultModel createdCount(@Nullable Integer createdCount) {
    this.createdCount = createdCount;
    return this;
  }

  /**
   * The number of created connected users.
   * @return createdCount
   */
  
  @Schema(name = "createdCount", description = "The number of created connected users.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdCount")
  public @Nullable Integer getCreatedCount() {
    return createdCount;
  }

  @JsonProperty("createdCount")
  public void setCreatedCount(@Nullable Integer createdCount) {
    this.createdCount = createdCount;
  }

  public ConnectedUserImportResultModel rejectedRows(List<@Valid ConnectedUserImportRejectedRowModel> rejectedRows) {
    this.rejectedRows = rejectedRows;
    return this;
  }

  public ConnectedUserImportResultModel addRejectedRowsItem(ConnectedUserImportRejectedRowModel rejectedRowsItem) {
    if (this.rejectedRows == null) {
      this.rejectedRows = new ArrayList<>();
    }
    this.rejectedRows.add(rejectedRowsItem);
    return this;
  }

  /**
   * The rows that were rejected.
   * @return rejectedRows
   */
  @Valid 
  @Schema(name = "rejectedRows", description = "The rows that were rejected.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("rejectedRows")
  public List<@Valid ConnectedUserImportRejectedRowModel> getRejectedRows() {
    return rejectedRows;
  }

  @JsonProperty("rejectedRows")
  public void setRejectedRows(List<@Valid ConnectedUserImportRejectedRowModel> rejectedRows) {
    this.rejectedRows = rejectedRows;
  }

  public ConnectedUserImportResultModel skippedExternalIds(List<String> skippedExternalIds) {
    this.skippedExternalIds = skippedExternalIds;
    return this;
  }

  public ConnectedUserImportResultModel addSkippedExternalIdsItem(String skippedExternalIdsItem) {
    if (this.skippedExternalIds == null) {
      this.skippedExternalIds = new ArrayList<>();
    }
    this.skippedExternalIds.add(skippedExternalIdsItem);
    return this;
  }

  /**
   * The external ids that already existed or were repeated.
   * @return skippedExternalIds
   */
  
  @Schema(name = "skippedExternalIds", description = "The external ids that already existed or were repeated.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("skippedExternalIds")
  public List<String> getSkippedExternalIds() {
    return skippedExternalIds;
  }

  @JsonProperty("skippedExternalIds")
  public void setSkippedExternalIds(List<String> skippedExternalIds) {
    this.skippedExternalIds = skippedExternalIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConnectedUserImportResultModel connectedUserImportResult = (ConnectedUserImportResultModel) o;
    return Objects.equals(this.createdCount, connectedUserImportResult.createdCount) &&
        Objects.equals(this.rejectedRows, connectedUserImportResult.rejectedRows) &&
        Objects.equals(this.skippedExternalIds, connectedUserImportResult.skippedExternalIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdCount, rejectedRows, skippedExternalIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectedUserImportResultModel {\n");
    sb.append("    createdCount: ").append(toIndentedString(createdCount)).append("\n");
    sb.append("    rejectedRows: ").append(toIndentedString(rejectedRows)).append("\n");
    sb.append("    skippedExternalIds: ").append(toIndentedString(skippedExternalIds)).append("\n");
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

