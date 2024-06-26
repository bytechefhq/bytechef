package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-26T12:24:38.500893+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public class GetOAuth2AuthorizationParametersRequestModel {

  private String authorizationName;

  private String componentName;

  private Integer connectionVersion;

  @Valid
  private Map<String, Object> parameters = new HashMap<>();

  public GetOAuth2AuthorizationParametersRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GetOAuth2AuthorizationParametersRequestModel(String componentName, Map<String, Object> parameters) {
    this.componentName = componentName;
    this.parameters = parameters;
  }

  public GetOAuth2AuthorizationParametersRequestModel authorizationName(String authorizationName) {
    this.authorizationName = authorizationName;
    return this;
  }

  /**
   * The name of an authorization used by this connection. Used for HTTP based services.
   * @return authorizationName
  */
  
  @Schema(name = "authorizationName", description = "The name of an authorization used by this connection. Used for HTTP based services.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("authorizationName")
  public String getAuthorizationName() {
    return authorizationName;
  }

  public void setAuthorizationName(String authorizationName) {
    this.authorizationName = authorizationName;
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

  public GetOAuth2AuthorizationParametersRequestModel connectionVersion(Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
    return this;
  }

  /**
   * The version of a connection.
   * @return connectionVersion
  */
  
  @Schema(name = "connectionVersion", description = "The version of a connection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionVersion")
  public Integer getConnectionVersion() {
    return connectionVersion;
  }

  public void setConnectionVersion(Integer connectionVersion) {
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
    return Objects.equals(this.authorizationName, getOAuth2AuthorizationParametersRequest.authorizationName) &&
        Objects.equals(this.componentName, getOAuth2AuthorizationParametersRequest.componentName) &&
        Objects.equals(this.connectionVersion, getOAuth2AuthorizationParametersRequest.connectionVersion) &&
        Objects.equals(this.parameters, getOAuth2AuthorizationParametersRequest.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorizationName, componentName, connectionVersion, parameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetOAuth2AuthorizationParametersRequestModel {\n");
    sb.append("    authorizationName: ").append(toIndentedString(authorizationName)).append("\n");
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

