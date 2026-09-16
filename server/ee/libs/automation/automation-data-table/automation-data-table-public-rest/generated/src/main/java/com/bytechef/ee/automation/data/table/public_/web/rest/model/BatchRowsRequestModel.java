package com.bytechef.ee.automation.data.table.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.BatchRowModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.CreateStrategyModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * BatchRowsRequestModel
 */

@JsonTypeName("BatchRowsRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-03T11:52:44.578282+02:00[Europe/Zagreb]", comments = "Generator version: 7.24.0")
public class BatchRowsRequestModel {

  private List<@Valid BatchRowModel> rows = new ArrayList<>();

  private @Nullable CreateStrategyModel createStrategy;

  public BatchRowsRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BatchRowsRequestModel(List<@Valid BatchRowModel> rows) {
    this.rows = rows;
  }

  public BatchRowsRequestModel rows(List<@Valid BatchRowModel> rows) {
    this.rows = rows;
    return this;
  }

  public BatchRowsRequestModel addRowsItem(BatchRowModel rowsItem) {
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
  @NotNull @Valid @Size(min = 1, max = 1000) 
  @Schema(name = "rows", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("rows")
  public List<@Valid BatchRowModel> getRows() {
    return rows;
  }

  @JsonProperty("rows")
  public void setRows(List<@Valid BatchRowModel> rows) {
    this.rows = rows;
  }

  public BatchRowsRequestModel createStrategy(@Nullable CreateStrategyModel createStrategy) {
    this.createStrategy = createStrategy;
    return this;
  }

  /**
   * Get createStrategy
   * @return createStrategy
   */
  @Valid 
  @Schema(name = "createStrategy", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createStrategy")
  public @Nullable CreateStrategyModel getCreateStrategy() {
    return createStrategy;
  }

  @JsonProperty("createStrategy")
  public void setCreateStrategy(@Nullable CreateStrategyModel createStrategy) {
    this.createStrategy = createStrategy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BatchRowsRequestModel batchRowsRequest = (BatchRowsRequestModel) o;
    return Objects.equals(this.rows, batchRowsRequest.rows) &&
        Objects.equals(this.createStrategy, batchRowsRequest.createStrategy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rows, createStrategy);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BatchRowsRequestModel {\n");
    sb.append("    rows: ").append(toIndentedString(rows)).append("\n");
    sb.append("    createStrategy: ").append(toIndentedString(createStrategy)).append("\n");
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

