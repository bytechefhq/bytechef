package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.AuthorizationTypeModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * Contains all required information to open a connection to a service defined by componentName parameter.
 */

@Schema(name = "getOAuth2AuthorizationParameters_request", description = "Contains all required information to open a connection to a service defined by componentName parameter.")
@JsonTypeName("getOAuth2AuthorizationParameters_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-02T08:44:56.413708+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class GetOAuth2AuthorizationParametersRequestModel {

  private AuthorizationTypeModel authorizationType;

  private String componentName;

  private @Nullable Integer connectionVersion;

  @Valid
  private Map<String, Object> parameters = new HashMap<>();

  public GetOAuth2AuthorizationParametersRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GetOAuth2AuthorizationParametersRequestModel(AuthorizationTypeModel authorizationType, String componentName, Map<String, Object> parameters) {
    this.authorizationType = authorizationType;
    this.componentName = componentName;
    this.parameters = parameters;
  }

  public GetOAuth2AuthorizationParametersRequestModel authorizationType(AuthorizationTypeModel authorizationType) {
    this.authorizationType = authorizationType;
    return this;
  }

  /**
   * Get authorizationType
   * @return authorizationType
   */
  @NotNull @Valid 
  @Schema(name = "authorizationType", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("authorizationType")
  public AuthorizationTypeModel getAuthorizationType() {
    return authorizationType;
  }

  public void setAuthorizationType(AuthorizationTypeModel authorizationType) {
    this.authorizationType = authorizationType;
  }

  public GetOAuth2AuthorizationParametersRequestModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of a component that uses this connection.
   * @return componentName
   */
  @NotNull 
  @Schema(name = "componentName", description = "The name of a component that uses this connection.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public GetOAuth2AuthorizationParametersRequestModel connectionVersion(@Nullable Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
    return this;
  }

  /**
   * The version of a connection.
   * @return connectionVersion
   */
  
  @Schema(name = "connectionVersion", description = "The version of a connection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionVersion")
  public @Nullable Integer getConnectionVersion() {
    return connectionVersion;
  }

  public void setConnectionVersion(@Nullable Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
  }

  public GetOAuth2AuthorizationParametersRequestModel parameters(Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  public GetOAuth2AuthorizationParametersRequestModel putParametersItem(String key, Object parametersItem) {
    if (this.parameters == null) {
      this.parameters = new HashMap<>();
    }
    this.parameters.put(key, parametersItem);
    return this;
  }

  /**
   * The parameters of a connection.
   * @return parameters
   */
  @NotNull 
  @Schema(name = "parameters", description = "The parameters of a connection.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("parameters")
  public Map<String, Object> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetOAuth2AuthorizationParametersRequestModel getOAuth2AuthorizationParametersRequest = (GetOAuth2AuthorizationParametersRequestModel) o;
    return Objects.equals(this.authorizationType, getOAuth2AuthorizationParametersRequest.authorizationType) &&
        Objects.equals(this.componentName, getOAuth2AuthorizationParametersRequest.componentName) &&
        Objects.equals(this.connectionVersion, getOAuth2AuthorizationParametersRequest.connectionVersion) &&
        Objects.equals(this.parameters, getOAuth2AuthorizationParametersRequest.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorizationType, componentName, connectionVersion, parameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetOAuth2AuthorizationParametersRequestModel {\n");
    sb.append("    authorizationType: ").append(toIndentedString(authorizationType)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    connectionVersion: ").append(toIndentedString(connectionVersion)).append("\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
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

