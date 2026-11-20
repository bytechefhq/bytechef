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
 * Returned when a request sets the connection of a component that the configuration provides for every connected user.
 */

@Schema(name = "ProvidedValueLockedError", description = "Returned when a request sets the connection of a component that the configuration provides for every connected user.")
@JsonTypeName("ProvidedValueLockedError")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-20T20:08:50.901089+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class ProvidedValueLockedErrorModel implements ProvisionFrontendWorkflowReference409ResponseModel {

  private @Nullable String providedComponentName;

  public ProvidedValueLockedErrorModel providedComponentName(@Nullable String providedComponentName) {
    this.providedComponentName = providedComponentName;
    return this;
  }

  /**
   * The name of the component whose connection the configuration provides.
   * @return providedComponentName
   */
  
  @Schema(name = "providedComponentName", description = "The name of the component whose connection the configuration provides.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("providedComponentName")
  public @Nullable String getProvidedComponentName() {
    return providedComponentName;
  }

  @JsonProperty("providedComponentName")
  public void setProvidedComponentName(@Nullable String providedComponentName) {
    this.providedComponentName = providedComponentName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProvidedValueLockedErrorModel providedValueLockedError = (ProvidedValueLockedErrorModel) o;
    return Objects.equals(this.providedComponentName, providedValueLockedError.providedComponentName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(providedComponentName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProvidedValueLockedErrorModel {\n");
    sb.append("    providedComponentName: ").append(toIndentedString(providedComponentName)).append("\n");
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

