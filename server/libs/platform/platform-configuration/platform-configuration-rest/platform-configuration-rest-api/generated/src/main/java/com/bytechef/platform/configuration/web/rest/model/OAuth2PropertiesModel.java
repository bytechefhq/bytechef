package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * OAuth2PropertiesModel
 */

@JsonTypeName("OAuth2Properties")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-15T09:52:48.574632+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class OAuth2PropertiesModel {

  private @Nullable String redirectUri;

  @Valid
  private List<String> predefinedApps = new ArrayList<>();

  public OAuth2PropertiesModel redirectUri(@Nullable String redirectUri) {
    this.redirectUri = redirectUri;
    return this;
  }

  /**
   * The redirect URI used for OAuth2 callback URL.
   * @return redirectUri
   */
  
  @Schema(name = "redirectUri", accessMode = Schema.AccessMode.READ_ONLY, description = "The redirect URI used for OAuth2 callback URL.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("redirectUri")
  public @Nullable String getRedirectUri() {
    return redirectUri;
  }

  public void setRedirectUri(@Nullable String redirectUri) {
    this.redirectUri = redirectUri;
  }

  public OAuth2PropertiesModel predefinedApps(List<String> predefinedApps) {
    this.predefinedApps = predefinedApps;
    return this;
  }

  public OAuth2PropertiesModel addPredefinedAppsItem(String predefinedAppsItem) {
    if (this.predefinedApps == null) {
      this.predefinedApps = new ArrayList<>();
    }
    this.predefinedApps.add(predefinedAppsItem);
    return this;
  }

  /**
   * The list of predefined OAuth2 apps.
   * @return predefinedApps
   */
  
  @Schema(name = "predefinedApps", accessMode = Schema.AccessMode.READ_ONLY, description = "The list of predefined OAuth2 apps.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("predefinedApps")
  public List<String> getPredefinedApps() {
    return predefinedApps;
  }

  public void setPredefinedApps(List<String> predefinedApps) {
    this.predefinedApps = predefinedApps;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OAuth2PropertiesModel oauth2Properties = (OAuth2PropertiesModel) o;
    return Objects.equals(this.redirectUri, oauth2Properties.redirectUri) &&
        Objects.equals(this.predefinedApps, oauth2Properties.predefinedApps);
  }

  @Override
  public int hashCode() {
    return Objects.hash(redirectUri, predefinedApps);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OAuth2PropertiesModel {\n");
    sb.append("    redirectUri: ").append(toIndentedString(redirectUri)).append("\n");
    sb.append("    predefinedApps: ").append(toIndentedString(predefinedApps)).append("\n");
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

