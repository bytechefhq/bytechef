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
 * An RFC 7807 problem detail. Branch on errorKey, never on detail.
 */

@Schema(name = "Error", description = "An RFC 7807 problem detail. Branch on errorKey, never on detail.")
@JsonTypeName("Error")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-03T11:52:44.578282+02:00[Europe/Zagreb]", comments = "Generator version: 7.24.0")
public class ErrorModel {

  private @Nullable String type;

  private @Nullable String title;

  private @Nullable Integer status;

  private @Nullable String detail;

  private @Nullable Integer errorKey;

  private @Nullable String entityClass;

  public ErrorModel type(@Nullable String type) {
    this.type = type;
    return this;
  }

  /**
   * A URI identifying the problem type.
   * @return type
   */
  
  @Schema(name = "type", description = "A URI identifying the problem type.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("type")
  public @Nullable String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(@Nullable String type) {
    this.type = type;
  }

  public ErrorModel title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Get title
   * @return title
   */
  
  @Schema(name = "title", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public ErrorModel status(@Nullable Integer status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  
  @Schema(name = "status", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("status")
  public @Nullable Integer getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable Integer status) {
    this.status = status;
  }

  public ErrorModel detail(@Nullable String detail) {
    this.detail = detail;
    return this;
  }

  /**
   * A human-readable explanation of this occurrence.
   * @return detail
   */
  
  @Schema(name = "detail", description = "A human-readable explanation of this occurrence.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("detail")
  public @Nullable String getDetail() {
    return detail;
  }

  @JsonProperty("detail")
  public void setDetail(@Nullable String detail) {
    this.detail = detail;
  }

  public ErrorModel errorKey(@Nullable Integer errorKey) {
    this.errorKey = errorKey;
    return this;
  }

  /**
   * The stable machine-readable key. 100 DATA_TABLE_NOT_FOUND, 103 DATA_TABLE_NAME_INVALID, 104 DATA_TABLE_ALREADY_EXISTS, 105 COLUMN_NOT_FOUND, 106 COLUMN_ALREADY_EXISTS, 107 COLUMN_NAME_INVALID, 108 ROW_NOT_FOUND, 109 ROW_VALUE_INVALID, 110 ROW_EXTERNAL_ID_CONFLICT, 111 ROW_EXTERNAL_ID_REQUIRED, 112 FILTER_INVALID, 113 SORT_INVALID, 114 BATCH_TOO_LARGE, 115 CSV_INVALID, 116 STORAGE_LIMIT_EXCEEDED. Absent on schema-validation failures.
   * @return errorKey
   */
  
  @Schema(name = "errorKey", description = "The stable machine-readable key. 100 DATA_TABLE_NOT_FOUND, 103 DATA_TABLE_NAME_INVALID, 104 DATA_TABLE_ALREADY_EXISTS, 105 COLUMN_NOT_FOUND, 106 COLUMN_ALREADY_EXISTS, 107 COLUMN_NAME_INVALID, 108 ROW_NOT_FOUND, 109 ROW_VALUE_INVALID, 110 ROW_EXTERNAL_ID_CONFLICT, 111 ROW_EXTERNAL_ID_REQUIRED, 112 FILTER_INVALID, 113 SORT_INVALID, 114 BATCH_TOO_LARGE, 115 CSV_INVALID, 116 STORAGE_LIMIT_EXCEEDED. Absent on schema-validation failures.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("errorKey")
  public @Nullable Integer getErrorKey() {
    return errorKey;
  }

  @JsonProperty("errorKey")
  public void setErrorKey(@Nullable Integer errorKey) {
    this.errorKey = errorKey;
  }

  public ErrorModel entityClass(@Nullable String entityClass) {
    this.entityClass = entityClass;
    return this;
  }

  /**
   * Get entityClass
   * @return entityClass
   */
  
  @Schema(name = "entityClass", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("entityClass")
  public @Nullable String getEntityClass() {
    return entityClass;
  }

  @JsonProperty("entityClass")
  public void setEntityClass(@Nullable String entityClass) {
    this.entityClass = entityClass;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ErrorModel error = (ErrorModel) o;
    return Objects.equals(this.type, error.type) &&
        Objects.equals(this.title, error.title) &&
        Objects.equals(this.status, error.status) &&
        Objects.equals(this.detail, error.detail) &&
        Objects.equals(this.errorKey, error.errorKey) &&
        Objects.equals(this.entityClass, error.entityClass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, title, status, detail, errorKey, entityClass);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ErrorModel {\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    detail: ").append(toIndentedString(detail)).append("\n");
    sb.append("    errorKey: ").append(toIndentedString(errorKey)).append("\n");
    sb.append("    entityClass: ").append(toIndentedString(entityClass)).append("\n");
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

