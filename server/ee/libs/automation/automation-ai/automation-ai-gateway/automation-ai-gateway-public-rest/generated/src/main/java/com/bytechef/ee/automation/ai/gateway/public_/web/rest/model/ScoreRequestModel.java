package com.bytechef.ee.automation.ai.gateway.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.math.BigDecimal;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A request to create a score for a trace or span.
 */

@Schema(name = "ScoreRequest", description = "A request to create a score for a trace or span.")
@JsonTypeName("ScoreRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-03T17:34:58.575661+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class ScoreRequestModel {

  private Long traceId;

  private @Nullable Long spanId;

  private String name;

  private String dataType;

  private @Nullable BigDecimal value;

  private @Nullable String stringValue;

  private @Nullable String comment;

  public ScoreRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ScoreRequestModel(Long traceId, String name, String dataType) {
    this.traceId = traceId;
    this.name = name;
    this.dataType = dataType;
  }

  public ScoreRequestModel traceId(Long traceId) {
    this.traceId = traceId;
    return this;
  }

  /**
   * The ID of the trace to score.
   * @return traceId
   */
  @NotNull 
  @Schema(name = "traceId", description = "The ID of the trace to score.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("traceId")
  public Long getTraceId() {
    return traceId;
  }

  @JsonProperty("traceId")
  public void setTraceId(Long traceId) {
    this.traceId = traceId;
  }

  public ScoreRequestModel spanId(@Nullable Long spanId) {
    this.spanId = spanId;
    return this;
  }

  /**
   * Optional ID of a specific span within the trace. If omitted the score applies to the whole trace.
   * @return spanId
   */
  
  @Schema(name = "spanId", description = "Optional ID of a specific span within the trace. If omitted the score applies to the whole trace.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("spanId")
  public @Nullable Long getSpanId() {
    return spanId;
  }

  @JsonProperty("spanId")
  public void setSpanId(@Nullable Long spanId) {
    this.spanId = spanId;
  }

  public ScoreRequestModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The score name (e.g. 'helpfulness', 'hallucination').
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The score name (e.g. 'helpfulness', 'hallucination').", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public ScoreRequestModel dataType(String dataType) {
    this.dataType = dataType;
    return this;
  }

  /**
   * The score data type: NUMERIC, CATEGORICAL, or BOOLEAN.
   * @return dataType
   */
  @NotNull 
  @Schema(name = "dataType", description = "The score data type: NUMERIC, CATEGORICAL, or BOOLEAN.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("dataType")
  public String getDataType() {
    return dataType;
  }

  @JsonProperty("dataType")
  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  public ScoreRequestModel value(@Nullable BigDecimal value) {
    this.value = value;
    return this;
  }

  /**
   * Numeric value, required when dataType is NUMERIC or BOOLEAN (0/1).
   * @return value
   */
  @Valid 
  @Schema(name = "value", description = "Numeric value, required when dataType is NUMERIC or BOOLEAN (0/1).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("value")
  public @Nullable BigDecimal getValue() {
    return value;
  }

  @JsonProperty("value")
  public void setValue(@Nullable BigDecimal value) {
    this.value = value;
  }

  public ScoreRequestModel stringValue(@Nullable String stringValue) {
    this.stringValue = stringValue;
    return this;
  }

  /**
   * String value, required when dataType is CATEGORICAL.
   * @return stringValue
   */
  
  @Schema(name = "stringValue", description = "String value, required when dataType is CATEGORICAL.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("stringValue")
  public @Nullable String getStringValue() {
    return stringValue;
  }

  @JsonProperty("stringValue")
  public void setStringValue(@Nullable String stringValue) {
    this.stringValue = stringValue;
  }

  public ScoreRequestModel comment(@Nullable String comment) {
    this.comment = comment;
    return this;
  }

  /**
   * Optional free-text comment explaining the score.
   * @return comment
   */
  
  @Schema(name = "comment", description = "Optional free-text comment explaining the score.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("comment")
  public @Nullable String getComment() {
    return comment;
  }

  @JsonProperty("comment")
  public void setComment(@Nullable String comment) {
    this.comment = comment;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScoreRequestModel scoreRequest = (ScoreRequestModel) o;
    return Objects.equals(this.traceId, scoreRequest.traceId) &&
        Objects.equals(this.spanId, scoreRequest.spanId) &&
        Objects.equals(this.name, scoreRequest.name) &&
        Objects.equals(this.dataType, scoreRequest.dataType) &&
        Objects.equals(this.value, scoreRequest.value) &&
        Objects.equals(this.stringValue, scoreRequest.stringValue) &&
        Objects.equals(this.comment, scoreRequest.comment);
  }

  @Override
  public int hashCode() {
    return Objects.hash(traceId, spanId, name, dataType, value, stringValue, comment);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ScoreRequestModel {\n");
    sb.append("    traceId: ").append(toIndentedString(traceId)).append("\n");
    sb.append("    spanId: ").append(toIndentedString(spanId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    dataType: ").append(toIndentedString(dataType)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    stringValue: ").append(toIndentedString(stringValue)).append("\n");
    sb.append("    comment: ").append(toIndentedString(comment)).append("\n");
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

