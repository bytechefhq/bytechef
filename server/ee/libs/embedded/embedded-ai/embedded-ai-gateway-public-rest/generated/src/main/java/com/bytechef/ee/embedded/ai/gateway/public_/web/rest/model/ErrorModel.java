package com.bytechef.ee.embedded.ai.gateway.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.ai.gateway.public_.web.rest.model.ErrorErrorModel;
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
 * The error envelope shared by every non-2xx JSON response.
 */

@Schema(name = "Error", description = "The error envelope shared by every non-2xx JSON response.")
@JsonTypeName("Error")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-07T00:41:20.907024+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ErrorModel {

  private @Nullable ErrorErrorModel error;

  public ErrorModel error(@Nullable ErrorErrorModel error) {
    this.error = error;
    return this;
  }

  /**
   * Get error
   * @return error
   */
  @Valid 
  @Schema(name = "error", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("error")
  public @Nullable ErrorErrorModel getError() {
    return error;
  }

  @JsonProperty("error")
  public void setError(@Nullable ErrorErrorModel error) {
    this.error = error;
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
    return Objects.equals(this.error, error.error);
  }

  @Override
  public int hashCode() {
    return Objects.hash(error);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ErrorModel {\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
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

