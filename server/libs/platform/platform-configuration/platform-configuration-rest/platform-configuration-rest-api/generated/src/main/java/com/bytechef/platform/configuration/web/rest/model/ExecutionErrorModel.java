package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
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
 * Contains information about an error that happened during execution.
 */

@Schema(name = "ExecutionError", description = "Contains information about an error that happened during execution.")
@JsonTypeName("ExecutionError")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-12T08:43:42.870402320+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public class ExecutionErrorModel {

  private @Nullable String message;

  @Valid
  private List<String> stackTrace = new ArrayList<>();

  public ExecutionErrorModel message(@Nullable String message) {
    this.message = message;
    return this;
  }

  /**
   * The error message.
   * @return message
   */
  
  @Schema(name = "message", description = "The error message.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("message")
  public @Nullable String getMessage() {
    return message;
  }

  public void setMessage(@Nullable String message) {
    this.message = message;
  }

  public ExecutionErrorModel stackTrace(List<String> stackTrace) {
    this.stackTrace = stackTrace;
    return this;
  }

  public ExecutionErrorModel addStackTraceItem(String stackTraceItem) {
    if (this.stackTrace == null) {
      this.stackTrace = new ArrayList<>();
    }
    this.stackTrace.add(stackTraceItem);
    return this;
  }

  /**
   * The error stacktrace.
   * @return stackTrace
   */
  
  @Schema(name = "stackTrace", description = "The error stacktrace.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("stackTrace")
  public List<String> getStackTrace() {
    return stackTrace;
  }

  public void setStackTrace(List<String> stackTrace) {
    this.stackTrace = stackTrace;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExecutionErrorModel executionError = (ExecutionErrorModel) o;
    return Objects.equals(this.message, executionError.message) &&
        Objects.equals(this.stackTrace, executionError.stackTrace);
  }

  @Override
  public int hashCode() {
    return Objects.hash(message, stackTrace);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExecutionErrorModel {\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    stackTrace: ").append(toIndentedString(stackTrace)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

