package com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model;

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
 * Whether an API collection is enabled.
 */

@Schema(name = "ApiCollectionEnabledInput", description = "Whether an API collection is enabled.")
@JsonTypeName("ApiCollectionEnabledInput")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-18T22:35:44.814650+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class ApiCollectionEnabledInputModel {

  private Boolean enabled;

  public ApiCollectionEnabledInputModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ApiCollectionEnabledInputModel(Boolean enabled) {
    this.enabled = enabled;
  }

  public ApiCollectionEnabledInputModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Get enabled
   * @return enabled
   */
  @NotNull 
  @Schema(name = "enabled", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  @JsonProperty("enabled")
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiCollectionEnabledInputModel apiCollectionEnabledInput = (ApiCollectionEnabledInputModel) o;
    return Objects.equals(this.enabled, apiCollectionEnabledInput.enabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCollectionEnabledInputModel {\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
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

