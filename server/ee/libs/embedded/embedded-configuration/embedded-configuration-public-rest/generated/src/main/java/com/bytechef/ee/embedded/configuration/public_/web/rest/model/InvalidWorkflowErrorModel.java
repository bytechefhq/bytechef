package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

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
 * Returned when an automation cannot be enabled because the workflow that would run has validation errors.
 */

@Schema(name = "InvalidWorkflowError", description = "Returned when an automation cannot be enabled because the workflow that would run has validation errors.")
@JsonTypeName("InvalidWorkflowError")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-18T21:40:49.864360+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class InvalidWorkflowErrorModel implements EnableFrontendProjectWorkflow409ResponseModel {

  private @Nullable Integer invalidWorkflowErrorCount;

  public InvalidWorkflowErrorModel invalidWorkflowErrorCount(@Nullable Integer invalidWorkflowErrorCount) {
    this.invalidWorkflowErrorCount = invalidWorkflowErrorCount;
    return this;
  }

  /**
   * The number of validation errors of the workflow that would run.
   * @return invalidWorkflowErrorCount
   */
  
  @Schema(name = "invalidWorkflowErrorCount", description = "The number of validation errors of the workflow that would run.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("invalidWorkflowErrorCount")
  public @Nullable Integer getInvalidWorkflowErrorCount() {
    return invalidWorkflowErrorCount;
  }

  @JsonProperty("invalidWorkflowErrorCount")
  public void setInvalidWorkflowErrorCount(@Nullable Integer invalidWorkflowErrorCount) {
    this.invalidWorkflowErrorCount = invalidWorkflowErrorCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InvalidWorkflowErrorModel invalidWorkflowError = (InvalidWorkflowErrorModel) o;
    return Objects.equals(this.invalidWorkflowErrorCount, invalidWorkflowError.invalidWorkflowErrorCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(invalidWorkflowErrorCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InvalidWorkflowErrorModel {\n");
    sb.append("    invalidWorkflowErrorCount: ").append(toIndentedString(invalidWorkflowErrorCount)).append("\n");
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

