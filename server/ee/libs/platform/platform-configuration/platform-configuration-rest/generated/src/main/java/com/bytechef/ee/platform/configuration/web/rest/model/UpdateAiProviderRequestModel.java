package com.bytechef.ee.platform.configuration.web.rest.model;

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
 * UpdateAiProviderRequestModel
 */

@JsonTypeName("updateAiProvider_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:32.735520+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class UpdateAiProviderRequestModel {

  private @Nullable String apiKey;

  public UpdateAiProviderRequestModel apiKey(@Nullable String apiKey) {
    this.apiKey = apiKey;
    return this;
  }

  /**
   * The API key of an AI provider.
   * @return apiKey
   */
  
  @Schema(name = "apiKey", description = "The API key of an AI provider.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("apiKey")
  public @Nullable String getApiKey() {
    return apiKey;
  }

  public void setApiKey(@Nullable String apiKey) {
    this.apiKey = apiKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateAiProviderRequestModel updateAiProviderRequest = (UpdateAiProviderRequestModel) o;
    return Objects.equals(this.apiKey, updateAiProviderRequest.apiKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiKey);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateAiProviderRequestModel {\n");
    sb.append("    apiKey: ").append(toIndentedString(apiKey)).append("\n");
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

