package com.bytechef.platform.configuration.web.rest.model;

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
 * OAuth2AuthorizationParametersModel
 */

@JsonTypeName("OAuth2AuthorizationParameters")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:33.876560+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class OAuth2AuthorizationParametersModel {

  private @Nullable String authorizationUrl;

  @Valid
  private Map<String, String> extraQueryParameters = new HashMap<>();

  private @Nullable String clientId;

  @Valid
  private List<String> scopes = new ArrayList<>();

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

  public void setClientId(@Nullable String clientId) {
    this.clientId = clientId;
  }

  public OAuth2AuthorizationParametersModel scopes(List<String> scopes) {
    this.scopes = scopes;
    return this;
  }

  public OAuth2AuthorizationParametersModel addScopesItem(String scopesItem) {
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
    OAuth2AuthorizationParametersModel oauth2AuthorizationParameters = (OAuth2AuthorizationParametersModel) o;
    return Objects.equals(this.authorizationUrl, oauth2AuthorizationParameters.authorizationUrl) &&
        Objects.equals(this.extraQueryParameters, oauth2AuthorizationParameters.extraQueryParameters) &&
        Objects.equals(this.clientId, oauth2AuthorizationParameters.clientId) &&
        Objects.equals(this.scopes, oauth2AuthorizationParameters.scopes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorizationUrl, extraQueryParameters, clientId, scopes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OAuth2AuthorizationParametersModel {\n");
    sb.append("    authorizationUrl: ").append(toIndentedString(authorizationUrl)).append("\n");
    sb.append("    extraQueryParameters: ").append(toIndentedString(extraQueryParameters)).append("\n");
    sb.append("    clientId: ").append(toIndentedString(clientId)).append("\n");
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

