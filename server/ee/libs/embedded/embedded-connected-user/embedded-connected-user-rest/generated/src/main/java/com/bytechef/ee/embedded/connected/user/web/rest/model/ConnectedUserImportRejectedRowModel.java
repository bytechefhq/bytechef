package com.bytechef.ee.embedded.connected.user.web.rest.model;

import java.net.URI;
import java.util.Objects;
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
 * ConnectedUserImportRejectedRowModel
 */

@JsonTypeName("ConnectedUserImportRejectedRow")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-23T23:44:29.402755+02:00[Europe/Zagreb]", comments = "Generator version: 7.25.0")
public class ConnectedUserImportRejectedRowModel {

  private @Nullable String reason;

  private @Nullable Integer rowNumber;

  public ConnectedUserImportRejectedRowModel reason(@Nullable String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * The reason the row was rejected.
   * @return reason
   */
  
  @Schema(name = "reason", description = "The reason the row was rejected.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("reason")
  public @Nullable String getReason() {
    return reason;
  }

  @JsonProperty("reason")
  public void setReason(@Nullable String reason) {
    this.reason = reason;
  }

  public ConnectedUserImportRejectedRowModel rowNumber(@Nullable Integer rowNumber) {
    this.rowNumber = rowNumber;
    return this;
  }

  /**
   * The number of the rejected row.
   * @return rowNumber
   */
  
  @Schema(name = "rowNumber", description = "The number of the rejected row.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("rowNumber")
  public @Nullable Integer getRowNumber() {
    return rowNumber;
  }

  @JsonProperty("rowNumber")
  public void setRowNumber(@Nullable Integer rowNumber) {
    this.rowNumber = rowNumber;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConnectedUserImportRejectedRowModel connectedUserImportRejectedRow = (ConnectedUserImportRejectedRowModel) o;
    return Objects.equals(this.reason, connectedUserImportRejectedRow.reason) &&
        Objects.equals(this.rowNumber, connectedUserImportRejectedRow.rowNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reason, rowNumber);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectedUserImportRejectedRowModel {\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
    sb.append("    rowNumber: ").append(toIndentedString(rowNumber)).append("\n");
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

