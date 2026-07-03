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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-03T17:58:15.209180+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class UpdateAiProviderRequestModel {

  private @Nullable String apiKey;

  private @Nullable String url;

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

  @JsonProperty("apiKey")
  public void setApiKey(@Nullable String apiKey) {
    this.apiKey = apiKey;
  }

  public UpdateAiProviderRequestModel url(@Nullable String url) {
    this.url = url;
    return this;
  }

  /**
   * The base URL of an AI provider (used by Ollama; blank defaults to localhost).
   * @return url
   */
  
  @Schema(name = "url", description = "The base URL of an AI provider (used by Ollama; blank defaults to localhost).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("url")
  public @Nullable String getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(@Nullable String url) {
    this.url = url;
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
    return Objects.equals(this.apiKey, updateAiProviderRequest.apiKey) &&
        Objects.equals(this.url, updateAiProviderRequest.url);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiKey, url);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateAiProviderRequestModel {\n");
    sb.append("    apiKey: ").append(toIndentedString(apiKey)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
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

