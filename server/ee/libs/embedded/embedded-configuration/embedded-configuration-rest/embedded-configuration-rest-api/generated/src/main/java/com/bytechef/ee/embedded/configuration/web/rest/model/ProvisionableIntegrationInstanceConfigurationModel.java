package com.bytechef.ee.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.OAuth2AuthorizationParametersModel;
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
 * An integration instance configuration the integration can be provisioned onto.
 */

@Schema(name = "ProvisionableIntegrationInstanceConfiguration", description = "An integration instance configuration the integration can be provisioned onto.")
@JsonTypeName("ProvisionableIntegrationInstanceConfiguration")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-23T19:46:00.733909+02:00[Europe/Zagreb]", comments = "Generator version: 7.25.0")
public class ProvisionableIntegrationInstanceConfigurationModel {

  private @Nullable IntegrationInstanceConfigurationModel integrationInstanceConfiguration;

  private @Nullable Boolean selfServe;

  private List<String> sharedConnectionParameterNames = new ArrayList<>();

  private @Nullable OAuth2AuthorizationParametersModel oAuth2AuthorizationParameters;

  private @Nullable String redirectUri;

  public ProvisionableIntegrationInstanceConfigurationModel integrationInstanceConfiguration(@Nullable IntegrationInstanceConfigurationModel integrationInstanceConfiguration) {
    this.integrationInstanceConfiguration = integrationInstanceConfiguration;
    return this;
  }

  /**
   * Get integrationInstanceConfiguration
   * @return integrationInstanceConfiguration
   */
  @Valid 
  @Schema(name = "integrationInstanceConfiguration", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationInstanceConfiguration")
  public @Nullable IntegrationInstanceConfigurationModel getIntegrationInstanceConfiguration() {
    return integrationInstanceConfiguration;
  }

  @JsonProperty("integrationInstanceConfiguration")
  public void setIntegrationInstanceConfiguration(@Nullable IntegrationInstanceConfigurationModel integrationInstanceConfiguration) {
    this.integrationInstanceConfiguration = integrationInstanceConfiguration;
  }

  public ProvisionableIntegrationInstanceConfigurationModel selfServe(@Nullable Boolean selfServe) {
    this.selfServe = selfServe;
    return this;
  }

  /**
   * Whether self-serve connections of the connected user use this configuration.
   * @return selfServe
   */
  
  @Schema(name = "selfServe", description = "Whether self-serve connections of the connected user use this configuration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("selfServe")
  public @Nullable Boolean getSelfServe() {
    return selfServe;
  }

  @JsonProperty("selfServe")
  public void setSelfServe(@Nullable Boolean selfServe) {
    this.selfServe = selfServe;
  }

  public ProvisionableIntegrationInstanceConfigurationModel sharedConnectionParameterNames(List<String> sharedConnectionParameterNames) {
    this.sharedConnectionParameterNames = sharedConnectionParameterNames;
    return this;
  }

  public ProvisionableIntegrationInstanceConfigurationModel addSharedConnectionParameterNamesItem(String sharedConnectionParameterNamesItem) {
    if (this.sharedConnectionParameterNames == null) {
      this.sharedConnectionParameterNames = new ArrayList<>();
    }
    this.sharedConnectionParameterNames.add(sharedConnectionParameterNamesItem);
    return this;
  }

  /**
   * The names of connection parameters the configuration already sets for every instance.
   * @return sharedConnectionParameterNames
   */
  
  @Schema(name = "sharedConnectionParameterNames", description = "The names of connection parameters the configuration already sets for every instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("sharedConnectionParameterNames")
  public List<String> getSharedConnectionParameterNames() {
    return sharedConnectionParameterNames;
  }

  @JsonProperty("sharedConnectionParameterNames")
  public void setSharedConnectionParameterNames(List<String> sharedConnectionParameterNames) {
    this.sharedConnectionParameterNames = sharedConnectionParameterNames;
  }

  public ProvisionableIntegrationInstanceConfigurationModel oAuth2AuthorizationParameters(@Nullable OAuth2AuthorizationParametersModel oAuth2AuthorizationParameters) {
    this.oAuth2AuthorizationParameters = oAuth2AuthorizationParameters;
    return this;
  }

  /**
   * Get oAuth2AuthorizationParameters
   * @return oAuth2AuthorizationParameters
   */
  @Valid 
  @Schema(name = "oAuth2AuthorizationParameters", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("oAuth2AuthorizationParameters")
  public @Nullable OAuth2AuthorizationParametersModel getoAuth2AuthorizationParameters() {
    return oAuth2AuthorizationParameters;
  }

  @JsonProperty("oAuth2AuthorizationParameters")
  public void setoAuth2AuthorizationParameters(@Nullable OAuth2AuthorizationParametersModel oAuth2AuthorizationParameters) {
    this.oAuth2AuthorizationParameters = oAuth2AuthorizationParameters;
  }

  public ProvisionableIntegrationInstanceConfigurationModel redirectUri(@Nullable String redirectUri) {
    this.redirectUri = redirectUri;
    return this;
  }

  /**
   * The OAuth2 redirect uri.
   * @return redirectUri
   */
  
  @Schema(name = "redirectUri", description = "The OAuth2 redirect uri.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("redirectUri")
  public @Nullable String getRedirectUri() {
    return redirectUri;
  }

  @JsonProperty("redirectUri")
  public void setRedirectUri(@Nullable String redirectUri) {
    this.redirectUri = redirectUri;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProvisionableIntegrationInstanceConfigurationModel provisionableIntegrationInstanceConfiguration = (ProvisionableIntegrationInstanceConfigurationModel) o;
    return Objects.equals(this.integrationInstanceConfiguration, provisionableIntegrationInstanceConfiguration.integrationInstanceConfiguration) &&
        Objects.equals(this.selfServe, provisionableIntegrationInstanceConfiguration.selfServe) &&
        Objects.equals(this.sharedConnectionParameterNames, provisionableIntegrationInstanceConfiguration.sharedConnectionParameterNames) &&
        Objects.equals(this.oAuth2AuthorizationParameters, provisionableIntegrationInstanceConfiguration.oAuth2AuthorizationParameters) &&
        Objects.equals(this.redirectUri, provisionableIntegrationInstanceConfiguration.redirectUri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(integrationInstanceConfiguration, selfServe, sharedConnectionParameterNames, oAuth2AuthorizationParameters, redirectUri);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProvisionableIntegrationInstanceConfigurationModel {\n");
    sb.append("    integrationInstanceConfiguration: ").append(toIndentedString(integrationInstanceConfiguration)).append("\n");
    sb.append("    selfServe: ").append(toIndentedString(selfServe)).append("\n");
    sb.append("    sharedConnectionParameterNames: ").append(toIndentedString(sharedConnectionParameterNames)).append("\n");
    sb.append("    oAuth2AuthorizationParameters: ").append(toIndentedString(oAuth2AuthorizationParameters)).append("\n");
    sb.append("    redirectUri: ").append(toIndentedString(redirectUri)).append("\n");
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

