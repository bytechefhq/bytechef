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
 * An AI provider.
 */

@Schema(name = "AiProvider", description = "An AI provider.")
@JsonTypeName("AiProvider")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-07T19:00:48.664497+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class AiProviderModel {

  private @Nullable Integer id;

  private String name;

  private @Nullable String icon;

  private @Nullable String apiKey;

  private @Nullable String url;

  private @Nullable Boolean enabled;

  private @Nullable Boolean requiresApiKey;

  private @Nullable Boolean requiresEndpoint;

  private @Nullable Boolean supportsText;

  private @Nullable Boolean supportsImage;

  private @Nullable Boolean supportsEmbeddings;

  private @Nullable Boolean copilotDocsProvider;

  public AiProviderModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AiProviderModel(String name) {
    this.name = name;
  }

  public AiProviderModel id(@Nullable Integer id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an AI provider.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an AI provider.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Integer getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable Integer id) {
    this.id = id;
  }

  public AiProviderModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of an AI provider.
   * @return name
   */
  
  @Schema(name = "name", accessMode = Schema.AccessMode.READ_ONLY, description = "The name of an AI provider.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public AiProviderModel icon(@Nullable String icon) {
    this.icon = icon;
    return this;
  }

  /**
   * The icon of an AI provider.
   * @return icon
   */
  
  @Schema(name = "icon", accessMode = Schema.AccessMode.READ_ONLY, description = "The icon of an AI provider.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("icon")
  public @Nullable String getIcon() {
    return icon;
  }

  @JsonProperty("icon")
  public void setIcon(@Nullable String icon) {
    this.icon = icon;
  }

  public AiProviderModel apiKey(@Nullable String apiKey) {
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

  public AiProviderModel url(@Nullable String url) {
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

  public AiProviderModel enabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * The enabled status of an AI provider.
   * @return enabled
   */
  
  @Schema(name = "enabled", description = "The enabled status of an AI provider.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public @Nullable Boolean getEnabled() {
    return enabled;
  }

  @JsonProperty("enabled")
  public void setEnabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
  }

  public AiProviderModel requiresApiKey(@Nullable Boolean requiresApiKey) {
    this.requiresApiKey = requiresApiKey;
    return this;
  }

  /**
   * Whether this AI provider requires an API key.
   * @return requiresApiKey
   */
  
  @Schema(name = "requiresApiKey", accessMode = Schema.AccessMode.READ_ONLY, description = "Whether this AI provider requires an API key.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("requiresApiKey")
  public @Nullable Boolean getRequiresApiKey() {
    return requiresApiKey;
  }

  @JsonProperty("requiresApiKey")
  public void setRequiresApiKey(@Nullable Boolean requiresApiKey) {
    this.requiresApiKey = requiresApiKey;
  }

  public AiProviderModel requiresEndpoint(@Nullable Boolean requiresEndpoint) {
    this.requiresEndpoint = requiresEndpoint;
    return this;
  }

  /**
   * Whether this AI provider requires a per-deployment endpoint URL.
   * @return requiresEndpoint
   */
  
  @Schema(name = "requiresEndpoint", accessMode = Schema.AccessMode.READ_ONLY, description = "Whether this AI provider requires a per-deployment endpoint URL.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("requiresEndpoint")
  public @Nullable Boolean getRequiresEndpoint() {
    return requiresEndpoint;
  }

  @JsonProperty("requiresEndpoint")
  public void setRequiresEndpoint(@Nullable Boolean requiresEndpoint) {
    this.requiresEndpoint = requiresEndpoint;
  }

  public AiProviderModel supportsText(@Nullable Boolean supportsText) {
    this.supportsText = supportsText;
    return this;
  }

  /**
   * Whether this AI provider can be used for text (chat) generation.
   * @return supportsText
   */
  
  @Schema(name = "supportsText", accessMode = Schema.AccessMode.READ_ONLY, description = "Whether this AI provider can be used for text (chat) generation.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("supportsText")
  public @Nullable Boolean getSupportsText() {
    return supportsText;
  }

  @JsonProperty("supportsText")
  public void setSupportsText(@Nullable Boolean supportsText) {
    this.supportsText = supportsText;
  }

  public AiProviderModel supportsImage(@Nullable Boolean supportsImage) {
    this.supportsImage = supportsImage;
    return this;
  }

  /**
   * Whether this AI provider can be used for image generation.
   * @return supportsImage
   */
  
  @Schema(name = "supportsImage", accessMode = Schema.AccessMode.READ_ONLY, description = "Whether this AI provider can be used for image generation.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("supportsImage")
  public @Nullable Boolean getSupportsImage() {
    return supportsImage;
  }

  @JsonProperty("supportsImage")
  public void setSupportsImage(@Nullable Boolean supportsImage) {
    this.supportsImage = supportsImage;
  }

  public AiProviderModel supportsEmbeddings(@Nullable Boolean supportsEmbeddings) {
    this.supportsEmbeddings = supportsEmbeddings;
    return this;
  }

  /**
   * Whether this AI provider can be used for embeddings.
   * @return supportsEmbeddings
   */
  
  @Schema(name = "supportsEmbeddings", accessMode = Schema.AccessMode.READ_ONLY, description = "Whether this AI provider can be used for embeddings.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("supportsEmbeddings")
  public @Nullable Boolean getSupportsEmbeddings() {
    return supportsEmbeddings;
  }

  @JsonProperty("supportsEmbeddings")
  public void setSupportsEmbeddings(@Nullable Boolean supportsEmbeddings) {
    this.supportsEmbeddings = supportsEmbeddings;
  }

  public AiProviderModel copilotDocsProvider(@Nullable Boolean copilotDocsProvider) {
    this.copilotDocsProvider = copilotDocsProvider;
    return this;
  }

  /**
   * Whether this AI provider is the one used to embed the shared Copilot documentation.
   * @return copilotDocsProvider
   */
  
  @Schema(name = "copilotDocsProvider", accessMode = Schema.AccessMode.READ_ONLY, description = "Whether this AI provider is the one used to embed the shared Copilot documentation.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("copilotDocsProvider")
  public @Nullable Boolean getCopilotDocsProvider() {
    return copilotDocsProvider;
  }

  @JsonProperty("copilotDocsProvider")
  public void setCopilotDocsProvider(@Nullable Boolean copilotDocsProvider) {
    this.copilotDocsProvider = copilotDocsProvider;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AiProviderModel aiProvider = (AiProviderModel) o;
    return Objects.equals(this.id, aiProvider.id) &&
        Objects.equals(this.name, aiProvider.name) &&
        Objects.equals(this.icon, aiProvider.icon) &&
        Objects.equals(this.apiKey, aiProvider.apiKey) &&
        Objects.equals(this.url, aiProvider.url) &&
        Objects.equals(this.enabled, aiProvider.enabled) &&
        Objects.equals(this.requiresApiKey, aiProvider.requiresApiKey) &&
        Objects.equals(this.requiresEndpoint, aiProvider.requiresEndpoint) &&
        Objects.equals(this.supportsText, aiProvider.supportsText) &&
        Objects.equals(this.supportsImage, aiProvider.supportsImage) &&
        Objects.equals(this.supportsEmbeddings, aiProvider.supportsEmbeddings) &&
        Objects.equals(this.copilotDocsProvider, aiProvider.copilotDocsProvider);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, icon, apiKey, url, enabled, requiresApiKey, requiresEndpoint, supportsText, supportsImage, supportsEmbeddings, copilotDocsProvider);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AiProviderModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    apiKey: ").append(toIndentedString(apiKey)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    requiresApiKey: ").append(toIndentedString(requiresApiKey)).append("\n");
    sb.append("    requiresEndpoint: ").append(toIndentedString(requiresEndpoint)).append("\n");
    sb.append("    supportsText: ").append(toIndentedString(supportsText)).append("\n");
    sb.append("    supportsImage: ").append(toIndentedString(supportsImage)).append("\n");
    sb.append("    supportsEmbeddings: ").append(toIndentedString(supportsEmbeddings)).append("\n");
    sb.append("    copilotDocsProvider: ").append(toIndentedString(copilotDocsProvider)).append("\n");
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

