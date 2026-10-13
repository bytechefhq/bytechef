package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
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
 * OAuth2AuthorizationParametersModel
 */

@JsonTypeName("OAuth2AuthorizationParameters")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-16T17:05:21.726625+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class OAuth2AuthorizationParametersModel {

  private @Nullable String authorizationUrl;

  private Map<String, String> extraQueryParameters = new HashMap<>();

  private @Nullable String clientId;

  private Map<String, Boolean> scopes = new HashMap<>();

  private @Nullable String authorizationContext;

  public OAuth2AuthorizationParametersModel authorizationUrl(@Nullable String authorizationUrl) {
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

  @JsonProperty("authorizationUrl")
  public void setAuthorizationUrl(@Nullable String authorizationUrl) {
    this.authorizationUrl = authorizationUrl;
  }

  public OAuth2AuthorizationParametersModel extraQueryParameters(Map<String, String> extraQueryParameters) {
    this.extraQueryParameters = extraQueryParameters;
    return this;
  }

  public OAuth2AuthorizationParametersModel putExtraQueryParametersItem(String key, String extraQueryParametersItem) {
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

  @JsonProperty("extraQueryParameters")
  public void setExtraQueryParameters(Map<String, String> extraQueryParameters) {
    this.extraQueryParameters = extraQueryParameters;
  }

  public OAuth2AuthorizationParametersModel clientId(@Nullable String clientId) {
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

  @JsonProperty("clientId")
  public void setClientId(@Nullable String clientId) {
    this.clientId = clientId;
  }

  public OAuth2AuthorizationParametersModel scopes(Map<String, Boolean> scopes) {
    this.scopes = scopes;
    return this;
  }

  public OAuth2AuthorizationParametersModel putScopesItem(String key, Boolean scopesItem) {
    if (this.scopes == null) {
      this.scopes = new HashMap<>();
    }
    this.scopes.put(key, scopesItem);
    return this;
  }

  /**
   * Get scopes
   * @return scopes
   */
  
  @Schema(name = "scopes", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("scopes")
  public Map<String, Boolean> getScopes() {
    return scopes;
  }

  @JsonProperty("scopes")
  public void setScopes(Map<String, Boolean> scopes) {
    this.scopes = scopes;
  }

  public OAuth2AuthorizationParametersModel authorizationContext(@Nullable String authorizationContext) {
    this.authorizationContext = authorizationContext;
    return this;
  }

  /**
   * Opaque, server-sealed state the client must post back with the authorization code.
   * @return authorizationContext
   */
  
  @Schema(name = "authorizationContext", description = "Opaque, server-sealed state the client must post back with the authorization code.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("authorizationContext")
  public @Nullable String getAuthorizationContext() {
    return authorizationContext;
  }

  @JsonProperty("authorizationContext")
  public void setAuthorizationContext(@Nullable String authorizationContext) {
    this.authorizationContext = authorizationContext;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OAuth2AuthorizationParametersModel oauth2AuthorizationParameters = (OAuth2AuthorizationParametersModel) o;
    return Objects.equals(this.authorizationUrl, oauth2AuthorizationParameters.authorizationUrl) &&
        Objects.equals(this.extraQueryParameters, oauth2AuthorizationParameters.extraQueryParameters) &&
        Objects.equals(this.clientId, oauth2AuthorizationParameters.clientId) &&
        Objects.equals(this.scopes, oauth2AuthorizationParameters.scopes) &&
        Objects.equals(this.authorizationContext, oauth2AuthorizationParameters.authorizationContext);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorizationUrl, extraQueryParameters, clientId, scopes, authorizationContext);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OAuth2AuthorizationParametersModel {\n");
    sb.append("    authorizationUrl: ").append(toIndentedString(authorizationUrl)).append("\n");
    sb.append("    extraQueryParameters: ").append(toIndentedString(extraQueryParameters)).append("\n");
    sb.append("    clientId: ").append(toIndentedString(clientId)).append("\n");
    sb.append("    scopes: ").append(toIndentedString(scopes)).append("\n");
    sb.append("    authorizationContext: ").append(toIndentedString(authorizationContext)).append("\n");
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

