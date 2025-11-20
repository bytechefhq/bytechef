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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:32.735520+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class AiProviderModel {

  private @Nullable Integer id;

  private String name;

  private @Nullable String icon;

  private @Nullable String apiKey;

  private @Nullable Boolean enabled;

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

  public void setApiKey(@Nullable String apiKey) {
    this.apiKey = apiKey;
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

  public void setEnabled(@Nullable Boolean enabled) {
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
    AiProviderModel aiProvider = (AiProviderModel) o;
    return Objects.equals(this.id, aiProvider.id) &&
        Objects.equals(this.name, aiProvider.name) &&
        Objects.equals(this.icon, aiProvider.icon) &&
        Objects.equals(this.apiKey, aiProvider.apiKey) &&
        Objects.equals(this.enabled, aiProvider.enabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, icon, apiKey, enabled);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AiProviderModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    apiKey: ").append(toIndentedString(apiKey)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
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

