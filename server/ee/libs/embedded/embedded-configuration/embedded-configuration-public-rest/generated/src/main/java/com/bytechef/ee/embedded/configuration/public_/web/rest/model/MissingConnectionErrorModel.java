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
 * Returned when a required connection could not be auto-wired for a catalog code workflow reference.
 */

@Schema(name = "MissingConnectionError", description = "Returned when a required connection could not be auto-wired for a catalog code workflow reference.")
@JsonTypeName("MissingConnectionError")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-27T21:42:20.429973+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class MissingConnectionErrorModel {

  private @Nullable String missingConnectionComponentName;

  public MissingConnectionErrorModel missingConnectionComponentName(@Nullable String missingConnectionComponentName) {
    this.missingConnectionComponentName = missingConnectionComponentName;
    return this;
  }

  /**
   * The name of the component that is missing a matching connection for the connected user.
   * @return missingConnectionComponentName
   */
  
  @Schema(name = "missingConnectionComponentName", description = "The name of the component that is missing a matching connection for the connected user.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("missingConnectionComponentName")
  public @Nullable String getMissingConnectionComponentName() {
    return missingConnectionComponentName;
  }

  @JsonProperty("missingConnectionComponentName")
  public void setMissingConnectionComponentName(@Nullable String missingConnectionComponentName) {
    this.missingConnectionComponentName = missingConnectionComponentName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MissingConnectionErrorModel missingConnectionError = (MissingConnectionErrorModel) o;
    return Objects.equals(this.missingConnectionComponentName, missingConnectionError.missingConnectionComponentName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(missingConnectionComponentName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MissingConnectionErrorModel {\n");
    sb.append("    missingConnectionComponentName: ").append(toIndentedString(missingConnectionComponentName)).append("\n");
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

