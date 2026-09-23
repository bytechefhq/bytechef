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
 * Returned when a reference cannot be enabled because a required workflow input has no value yet.
 */

@Schema(name = "MissingInputError", description = "Returned when a reference cannot be enabled because a required workflow input has no value yet.")
@JsonTypeName("MissingInputError")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-23T21:34:31.743535+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class MissingInputErrorModel implements EnableFrontendProjectWorkflow409ResponseModel {

  private @Nullable String missingInputName;

  public MissingInputErrorModel missingInputName(@Nullable String missingInputName) {
    this.missingInputName = missingInputName;
    return this;
  }

  /**
   * The name of the required input that has no value.
   * @return missingInputName
   */
  
  @Schema(name = "missingInputName", description = "The name of the required input that has no value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("missingInputName")
  public @Nullable String getMissingInputName() {
    return missingInputName;
  }

  @JsonProperty("missingInputName")
  public void setMissingInputName(@Nullable String missingInputName) {
    this.missingInputName = missingInputName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MissingInputErrorModel missingInputError = (MissingInputErrorModel) o;
    return Objects.equals(this.missingInputName, missingInputError.missingInputName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(missingInputName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MissingInputErrorModel {\n");
    sb.append("    missingInputName: ").append(toIndentedString(missingInputName)).append("\n");
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

