package com.bytechef.ee.embedded.execution.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ErrorsInnerMetaModel;
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
 * ErrorsInnerModel
 */

@JsonTypeName("Errors_inner")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-07T14:49:44.122259+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public class ErrorsInnerModel {

  private String id;

  private String detail;

  @Deprecated
  private String problemType;

  private String title;

  private String code;

  private String status;

  private ErrorsInnerMetaModel meta;

  public ErrorsInnerModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ErrorsInnerModel(String id, String detail, String problemType, String title, String code, String status, ErrorsInnerMetaModel meta) {
    this.id = id;
    this.detail = detail;
    this.problemType = problemType;
    this.title = title;
    this.code = code;
    this.status = status;
    this.meta = meta;
  }

  public ErrorsInnerModel id(String id) {
    this.id = id;
    return this;
  }

  /**
   * A unique identifier for the instance of the error. Provide this to support when contacting ByteChef.
   * @return id
   */
  @NotNull 
  @Schema(name = "id", example = "9366efb4-8fb1-4a28-bfb0-8d6f9cc6b5c5", description = "A unique identifier for the instance of the error. Provide this to support when contacting ByteChef.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public ErrorsInnerModel detail(String detail) {
    this.detail = detail;
    return this;
  }

  /**
   * A detailed description of the error.
   * @return detail
   */
  @NotNull 
  @Schema(name = "detail", description = "A detailed description of the error.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("detail")
  public String getDetail() {
    return detail;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }

  public ErrorsInnerModel problemType(String problemType) {
    this.problemType = problemType;
    return this;
  }

  /**
   * The ByteChef error code associated with the error.
   * @return problemType
   * @deprecated
   */
  @NotNull 
  @Schema(name = "problem_type", example = "MISSING_REQUIRED_FIELD", description = "The ByteChef error code associated with the error.", deprecated = true, requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("problem_type")
  @Deprecated
  public String getProblemType() {
    return problemType;
  }

  /**
   * @deprecated
   */
  @Deprecated
  public void setProblemType(String problemType) {
    this.problemType = problemType;
  }

  public ErrorsInnerModel title(String title) {
    this.title = title;
    return this;
  }

  /**
   * A brief description of the error. The schema and type of message will vary by Provider.
   * @return title
   */
  @NotNull 
  @Schema(name = "title", example = "Property values were not valid ", description = "A brief description of the error. The schema and type of message will vary by Provider.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public ErrorsInnerModel code(String code) {
    this.code = code;
    return this;
  }

  /**
   * The ByteChef error code associated with the error.
   * @return code
   */
  @NotNull 
  @Schema(name = "code", example = "MISSING_REQUIRED_FIELD", description = "The ByteChef error code associated with the error.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("code")
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public ErrorsInnerModel status(String status) {
    this.status = status;
    return this;
  }

  /**
   * The HTTP status code associated with the error.
   * @return status
   */
  @NotNull 
  @Schema(name = "status", example = "400", description = "The HTTP status code associated with the error.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public ErrorsInnerModel meta(ErrorsInnerMetaModel meta) {
    this.meta = meta;
    return this;
  }

  /**
   * Get meta
   * @return meta
   */
  @NotNull @Valid 
  @Schema(name = "meta", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("meta")
  public ErrorsInnerMetaModel getMeta() {
    return meta;
  }

  public void setMeta(ErrorsInnerMetaModel meta) {
    this.meta = meta;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ErrorsInnerModel errorsInner = (ErrorsInnerModel) o;
    return Objects.equals(this.id, errorsInner.id) &&
        Objects.equals(this.detail, errorsInner.detail) &&
        Objects.equals(this.problemType, errorsInner.problemType) &&
        Objects.equals(this.title, errorsInner.title) &&
        Objects.equals(this.code, errorsInner.code) &&
        Objects.equals(this.status, errorsInner.status) &&
        Objects.equals(this.meta, errorsInner.meta);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, detail, problemType, title, code, status, meta);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ErrorsInnerModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    detail: ").append(toIndentedString(detail)).append("\n");
    sb.append("    problemType: ").append(toIndentedString(problemType)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    meta: ").append(toIndentedString(meta)).append("\n");
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

