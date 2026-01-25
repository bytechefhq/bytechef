package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * OAuth2Model
 */

@JsonTypeName("OAuth2")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-23T12:36:59.411987+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class OAuth2Model {

  private @Nullable String authorizationUrl;

  @Valid
  private Map<String, String> extraQueryParameters = new HashMap<>();

  private @Nullable String clientId;

  private @Nullable String redirectUri;

  @Valid
  private List<String> scopes = new ArrayList<>();

  public OAuth2Model authorizationUrl(@Nullable String authorizationUrl) {
    this.authorizationUrl = authorizationUrl;
    return this;
  }

  /**
   * Get authorizationUrl
   * @return authorizationUrl
   */
  
  @Schema(name = "authorizationUrl", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("authorizationUrl")
  public @Nullable String getAuthorizationUrl() {
    return authorizationUrl;
  }

  public void setAuthorizationUrl(@Nullable String authorizationUrl) {
    this.authorizationUrl = authorizationUrl;
  }

  public OAuth2Model extraQueryParameters(Map<String, String> extraQueryParameters) {
    this.extraQueryParameters = extraQueryParameters;
    return this;
  }

  public OAuth2Model putExtraQueryParametersItem(String key, String extraQueryParametersItem) {
    if (this.extraQueryParameters == null) {
      this.extraQueryParameters = new HashMap<>();
    }
    this.extraQueryParameters.put(key, extraQueryParametersItem);
    return this;
  }

  /**
   * Get extraQueryParameters
   * @return extraQueryParameters
   */
  
  @Schema(name = "extraQueryParameters", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("extraQueryParameters")
  public Map<String, String> getExtraQueryParameters() {
    return extraQueryParameters;
  }

  public void setExtraQueryParameters(Map<String, String> extraQueryParameters) {
    this.extraQueryParameters = extraQueryParameters;
  }

  public OAuth2Model clientId(@Nullable String clientId) {
    this.clientId = clientId;
    return this;
  }

  /**
   * Get clientId
   * @return clientId
   */
  
  @Schema(name = "clientId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("clientId")
  public @Nullable String getClientId() {
    return clientId;
  }

  public void setClientId(@Nullable String clientId) {
    this.clientId = clientId;
  }

  public OAuth2Model redirectUri(@Nullable String redirectUri) {
    this.redirectUri = redirectUri;
    return this;
  }

  /**
   * The redirect URI used for OAuth2 callback URL.
   * @return redirectUri
   */
  
  @Schema(name = "redirectUri", description = "The redirect URI used for OAuth2 callback URL.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("redirectUri")
  public @Nullable String getRedirectUri() {
    return redirectUri;
  }

  public void setRedirectUri(@Nullable String redirectUri) {
    this.redirectUri = redirectUri;
  }

  public OAuth2Model scopes(List<String> scopes) {
    this.scopes = scopes;
    return this;
  }

  public OAuth2Model addScopesItem(String scopesItem) {
    if (this.scopes == null) {
      this.scopes = new ArrayList<>();
    }
    this.scopes.add(scopesItem);
    return this;
  }

  /**
   * Get scopes
   * @return scopes
   */
  
  @Schema(name = "scopes", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("scopes")
  public List<String> getScopes() {
    return scopes;
  }

  public void setScopes(List<String> scopes) {
    this.scopes = scopes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OAuth2Model oauth2 = (OAuth2Model) o;
    return Objects.equals(this.authorizationUrl, oauth2.authorizationUrl) &&
        Objects.equals(this.extraQueryParameters, oauth2.extraQueryParameters) &&
        Objects.equals(this.clientId, oauth2.clientId) &&
        Objects.equals(this.redirectUri, oauth2.redirectUri) &&
        Objects.equals(this.scopes, oauth2.scopes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorizationUrl, extraQueryParameters, clientId, redirectUri, scopes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OAuth2Model {\n");
    sb.append("    authorizationUrl: ").append(toIndentedString(authorizationUrl)).append("\n");
    sb.append("    extraQueryParameters: ").append(toIndentedString(extraQueryParameters)).append("\n");
    sb.append("    clientId: ").append(toIndentedString(clientId)).append("\n");
    sb.append("    redirectUri: ").append(toIndentedString(redirectUri)).append("\n");
    sb.append("    scopes: ").append(toIndentedString(scopes)).append("\n");
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

