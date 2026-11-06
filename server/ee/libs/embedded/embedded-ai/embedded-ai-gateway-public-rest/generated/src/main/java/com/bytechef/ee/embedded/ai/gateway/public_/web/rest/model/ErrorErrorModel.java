package com.bytechef.ee.embedded.ai.gateway.public_.web.rest.model;

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
 * ErrorErrorModel
 */

@JsonTypeName("Error_error")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-07T00:41:20.907024+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ErrorErrorModel {

  private @Nullable String type;

  private @Nullable String message;

  private @Nullable BigDecimal budgetUsd;

  private @Nullable BigDecimal spentUsd;

  public ErrorErrorModel type(@Nullable String type) {
    this.type = type;
    return this;
  }

  /**
   * A stable machine-readable error type, e.g. invalid_request_error or budget_exceeded.
   * @return type
   */
  
  @Schema(name = "type", description = "A stable machine-readable error type, e.g. invalid_request_error or budget_exceeded.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("type")
  public @Nullable String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(@Nullable String type) {
    this.type = type;
  }

  public ErrorErrorModel message(@Nullable String message) {
    this.message = message;
    return this;
  }

  /**
   * Get message
   * @return message
   */
  
  @Schema(name = "message", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("message")
  public @Nullable String getMessage() {
    return message;
  }

  @JsonProperty("message")
  public void setMessage(@Nullable String message) {
    this.message = message;
  }

  public ErrorErrorModel budgetUsd(@Nullable BigDecimal budgetUsd) {
    this.budgetUsd = budgetUsd;
    return this;
  }

  /**
   * Present on 402 only, when known.
   * @return budgetUsd
   */
  @Valid 
  @Schema(name = "budgetUsd", description = "Present on 402 only, when known.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("budgetUsd")
  public @Nullable BigDecimal getBudgetUsd() {
    return budgetUsd;
  }

  @JsonProperty("budgetUsd")
  public void setBudgetUsd(@Nullable BigDecimal budgetUsd) {
    this.budgetUsd = budgetUsd;
  }

  public ErrorErrorModel spentUsd(@Nullable BigDecimal spentUsd) {
    this.spentUsd = spentUsd;
    return this;
  }

  /**
   * Present on 402 only, when known.
   * @return spentUsd
   */
  @Valid 
  @Schema(name = "spentUsd", description = "Present on 402 only, when known.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("spentUsd")
  public @Nullable BigDecimal getSpentUsd() {
    return spentUsd;
  }

  @JsonProperty("spentUsd")
  public void setSpentUsd(@Nullable BigDecimal spentUsd) {
    this.spentUsd = spentUsd;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ErrorErrorModel errorError = (ErrorErrorModel) o;
    return Objects.equals(this.type, errorError.type) &&
        Objects.equals(this.message, errorError.message) &&
        Objects.equals(this.budgetUsd, errorError.budgetUsd) &&
        Objects.equals(this.spentUsd, errorError.spentUsd);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, message, budgetUsd, spentUsd);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ErrorErrorModel {\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    budgetUsd: ").append(toIndentedString(budgetUsd)).append("\n");
    sb.append("    spentUsd: ").append(toIndentedString(spentUsd)).append("\n");
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

