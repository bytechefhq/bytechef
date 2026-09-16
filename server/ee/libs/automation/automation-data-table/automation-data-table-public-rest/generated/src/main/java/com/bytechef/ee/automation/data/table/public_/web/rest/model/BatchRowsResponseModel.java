package com.bytechef.ee.automation.data.table.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.DataTableRowModel;
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
 * BatchRowsResponseModel
 */

@JsonTypeName("BatchRowsResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-03T11:52:44.578282+02:00[Europe/Zagreb]", comments = "Generator version: 7.24.0")
public class BatchRowsResponseModel {

  private List<@Valid DataTableRowModel> rows = new ArrayList<>();

  public BatchRowsResponseModel rows(List<@Valid DataTableRowModel> rows) {
    this.rows = rows;
    return this;
  }

  public BatchRowsResponseModel addRowsItem(DataTableRowModel rowsItem) {
    if (this.rows == null) {
      this.rows = new ArrayList<>();
    }
    this.rows.add(rowsItem);
    return this;
  }

  /**
   * Get rows
   * @return rows
   */
  @Valid 
  @Schema(name = "rows", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("rows")
  public List<@Valid DataTableRowModel> getRows() {
    return rows;
  }

  @JsonProperty("rows")
  public void setRows(List<@Valid DataTableRowModel> rows) {
    this.rows = rows;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BatchRowsResponseModel batchRowsResponse = (BatchRowsResponseModel) o;
    return Objects.equals(this.rows, batchRowsResponse.rows);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rows);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BatchRowsResponseModel {\n");
    sb.append("    rows: ").append(toIndentedString(rows)).append("\n");
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

