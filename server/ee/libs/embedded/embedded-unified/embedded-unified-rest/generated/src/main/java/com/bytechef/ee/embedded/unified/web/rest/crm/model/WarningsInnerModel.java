package com.bytechef.ee.embedded.unified.web.rest.crm.model;

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
 * WarningsInnerModel
 */

@JsonTypeName("warnings_inner")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-07T14:49:44.711094+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public class WarningsInnerModel {

  private @Nullable String detail;

  private @Nullable String problemType;

  private @Nullable String title;

  public WarningsInnerModel detail(@Nullable String detail) {
    this.detail = detail;
    return this;
  }

  /**
   * Get detail
   * @return detail
   */
  
  @Schema(name = "detail", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("detail")
  public @Nullable String getDetail() {
    return detail;
  }

  public void setDetail(@Nullable String detail) {
    this.detail = detail;
  }

  public WarningsInnerModel problemType(@Nullable String problemType) {
    this.problemType = problemType;
    return this;
  }

  /**
   * Get problemType
   * @return problemType
   */
  
  @Schema(name = "problem_type", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("problem_type")
  public @Nullable String getProblemType() {
    return problemType;
  }

  public void setProblemType(@Nullable String problemType) {
    this.problemType = problemType;
  }

  public WarningsInnerModel title(@Nullable String title) {
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

  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WarningsInnerModel warningsInner = (WarningsInnerModel) o;
    return Objects.equals(this.detail, warningsInner.detail) &&
        Objects.equals(this.problemType, warningsInner.problemType) &&
        Objects.equals(this.title, warningsInner.title);
  }

  @Override
  public int hashCode() {
    return Objects.hash(detail, problemType, title);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WarningsInnerModel {\n");
    sb.append("    detail: ").append(toIndentedString(detail)).append("\n");
    sb.append("    problemType: ").append(toIndentedString(problemType)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

