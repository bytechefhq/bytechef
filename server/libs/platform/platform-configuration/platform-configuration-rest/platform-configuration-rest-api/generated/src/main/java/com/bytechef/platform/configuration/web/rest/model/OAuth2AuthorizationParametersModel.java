package com.bytechef.platform.configuration.web.rest.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.media.Schema;


import jakarta.annotation.Generated;

/**
 * OAuth2AuthorizationParametersModel
 */

@JsonTypeName("OAuth2AuthorizationParameters")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-05-13T22:08:56.346286+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public class OAuth2AuthorizationParametersModel {

  private String authorizationUrl;

  private String clientId;

  @Valid
  private List<String> scopes = new ArrayList<>();

  private Map<String, String> extraQueryParameters;

    private void putExtraQueryParameter(String name, String value) {
        if (extraQueryParameters == null) {
            extraQueryParameters = new HashMap<>();
        }

        extraQueryParameters.put(name, value);
    }

    public OAuth2AuthorizationParametersModel accessType(String accessType) {
        putExtraQueryParameter("access_type", accessType);
        return this;
    }

    @Schema(name = "extraQueryParameters", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("extraQueryParameters")
    public Map<String, String> getExtraQueryParameters() {
        return Collections.unmodifiableMap(extraQueryParameters);
    }

    public OAuth2AuthorizationParametersModel authorizationUrl(String authorizationUrl) {
    this.authorizationUrl = authorizationUrl;
    return this;
  }

  /**
   * Get authorizationUrl
   * @return authorizationUrl
  */

  @Schema(name = "authorizationUrl", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("authorizationUrl")
  public String getAuthorizationUrl() {
    return authorizationUrl;
  }

  public void setAuthorizationUrl(String authorizationUrl) {
    this.authorizationUrl = authorizationUrl;
  }

  public OAuth2AuthorizationParametersModel clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  /**
   * Get clientId
   * @return clientId
  */

  @Schema(name = "clientId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("clientId")
  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
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
        Objects.equals(this.clientId, oauth2AuthorizationParameters.clientId) &&
        Objects.equals(this.scopes, oauth2AuthorizationParameters.scopes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorizationUrl, clientId, scopes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OAuth2AuthorizationParametersModel {\n");
    sb.append("    authorizationUrl: ").append(toIndentedString(authorizationUrl)).append("\n");
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

