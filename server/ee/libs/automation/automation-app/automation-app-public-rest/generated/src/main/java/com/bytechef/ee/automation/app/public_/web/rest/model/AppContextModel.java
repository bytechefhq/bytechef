package com.bytechef.ee.automation.app.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.lang.Nullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The calling app instance and the settings an app needs to sign its users in.
 */

@Schema(name = "AppContext", description = "The calling app instance and the settings an app needs to sign its users in.")
@JsonTypeName("AppContext")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-16T16:22:03.857597+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class AppContextModel {

  private Long appId;

  private String appName;

  private Long appInstanceId;

  private String appInstanceName;

  /**
   * Gets or Sets environment
   */
  public enum EnvironmentEnum {
    DEVELOPMENT("DEVELOPMENT"),
    
    STAGING("STAGING"),
    
    PRODUCTION("PRODUCTION");

    private final String value;

    EnvironmentEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static EnvironmentEnum fromValue(String value) {
      for (EnvironmentEnum b : EnvironmentEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private EnvironmentEnum environment;

  private Map<String, Object> _configuration = new HashMap<>();

  /**
   * Whether the instance signs its users in: NONE, OPTIONAL or REQUIRED.
   */
  public enum UserAuthModeEnum {
    NONE("NONE"),
    
    OPTIONAL("OPTIONAL"),
    
    REQUIRED("REQUIRED");

    private final String value;

    UserAuthModeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static UserAuthModeEnum fromValue(String value) {
      for (UserAuthModeEnum b : UserAuthModeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private UserAuthModeEnum userAuthMode;

  private JsonNullable<String> issuer = JsonNullable.<String>undefined();

  private JsonNullable<String> clientId = JsonNullable.<String>undefined();

  private JsonNullable<String> authorizationEndpoint = JsonNullable.<String>undefined();

  private JsonNullable<String> tokenEndpoint = JsonNullable.<String>undefined();

  public AppContextModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AppContextModel(Long appId, String appName, Long appInstanceId, String appInstanceName, EnvironmentEnum environment, Map<String, Object> _configuration, UserAuthModeEnum userAuthMode) {
    this.appId = appId;
    this.appName = appName;
    this.appInstanceId = appInstanceId;
    this.appInstanceName = appInstanceName;
    this.environment = environment;
    this._configuration = _configuration;
    this.userAuthMode = userAuthMode;
  }

  public AppContextModel appId(Long appId) {
    this.appId = appId;
    return this;
  }

  /**
   * Get appId
   * @return appId
   */
  @NotNull 
  @Schema(name = "appId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("appId")
  public Long getAppId() {
    return appId;
  }

  @JsonProperty("appId")
  public void setAppId(Long appId) {
    this.appId = appId;
  }

  public AppContextModel appName(String appName) {
    this.appName = appName;
    return this;
  }

  /**
   * Get appName
   * @return appName
   */
  @NotNull 
  @Schema(name = "appName", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("appName")
  public String getAppName() {
    return appName;
  }

  @JsonProperty("appName")
  public void setAppName(String appName) {
    this.appName = appName;
  }

  public AppContextModel appInstanceId(Long appInstanceId) {
    this.appInstanceId = appInstanceId;
    return this;
  }

  /**
   * Get appInstanceId
   * @return appInstanceId
   */
  @NotNull 
  @Schema(name = "appInstanceId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("appInstanceId")
  public Long getAppInstanceId() {
    return appInstanceId;
  }

  @JsonProperty("appInstanceId")
  public void setAppInstanceId(Long appInstanceId) {
    this.appInstanceId = appInstanceId;
  }

  public AppContextModel appInstanceName(String appInstanceName) {
    this.appInstanceName = appInstanceName;
    return this;
  }

  /**
   * Get appInstanceName
   * @return appInstanceName
   */
  @NotNull 
  @Schema(name = "appInstanceName", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("appInstanceName")
  public String getAppInstanceName() {
    return appInstanceName;
  }

  @JsonProperty("appInstanceName")
  public void setAppInstanceName(String appInstanceName) {
    this.appInstanceName = appInstanceName;
  }

  public AppContextModel environment(EnvironmentEnum environment) {
    this.environment = environment;
    return this;
  }

  /**
   * Get environment
   * @return environment
   */
  @NotNull 
  @Schema(name = "environment", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("environment")
  public EnvironmentEnum getEnvironment() {
    return environment;
  }

  @JsonProperty("environment")
  public void setEnvironment(EnvironmentEnum environment) {
    this.environment = environment;
  }

  public AppContextModel _configuration(Map<String, Object> _configuration) {
    this._configuration = _configuration;
    return this;
  }

  public AppContextModel putConfigurationItem(String key, Object _configurationItem) {
    if (this._configuration == null) {
      this._configuration = new HashMap<>();
    }
    this._configuration.put(key, _configurationItem);
    return this;
  }

  /**
   * The instance configuration.
   * @return _configuration
   */
  @NotNull 
  @Schema(name = "configuration", description = "The instance configuration.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("configuration")
  public Map<String, Object> getConfiguration() {
    return _configuration;
  }

  @JsonProperty("configuration")
  public void setConfiguration(Map<String, Object> _configuration) {
    this._configuration = _configuration;
  }

  public AppContextModel userAuthMode(UserAuthModeEnum userAuthMode) {
    this.userAuthMode = userAuthMode;
    return this;
  }

  /**
   * Whether the instance signs its users in: NONE, OPTIONAL or REQUIRED.
   * @return userAuthMode
   */
  @NotNull 
  @Schema(name = "userAuthMode", description = "Whether the instance signs its users in: NONE, OPTIONAL or REQUIRED.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("userAuthMode")
  public UserAuthModeEnum getUserAuthMode() {
    return userAuthMode;
  }

  @JsonProperty("userAuthMode")
  public void setUserAuthMode(UserAuthModeEnum userAuthMode) {
    this.userAuthMode = userAuthMode;
  }

  public AppContextModel issuer(String issuer) {
    this.issuer = JsonNullable.of(issuer);
    return this;
  }

  /**
   * The authorization server's issuer. Null while the instance has no OAuth2 client.
   * @return issuer
   */
  
  @Schema(name = "issuer", description = "The authorization server's issuer. Null while the instance has no OAuth2 client.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("issuer")
  public JsonNullable<String> getIssuer() {
    return issuer;
  }

  public void setIssuer(JsonNullable<String> issuer) {
    this.issuer = issuer;
  }

  public AppContextModel clientId(String clientId) {
    this.clientId = JsonNullable.of(clientId);
    return this;
  }

  /**
   * The instance's OAuth2 client id, a public client that uses PKCE. Null while the instance has no OAuth2 client.
   * @return clientId
   */
  
  @Schema(name = "clientId", description = "The instance's OAuth2 client id, a public client that uses PKCE. Null while the instance has no OAuth2 client.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("clientId")
  public JsonNullable<String> getClientId() {
    return clientId;
  }

  public void setClientId(JsonNullable<String> clientId) {
    this.clientId = clientId;
  }

  public AppContextModel authorizationEndpoint(String authorizationEndpoint) {
    this.authorizationEndpoint = JsonNullable.of(authorizationEndpoint);
    return this;
  }

  /**
   * The authorization endpoint URL. Null while the instance has no OAuth2 client. The server publishes no OpenID discovery document, so an app takes the endpoints from here.
   * @return authorizationEndpoint
   */
  
  @Schema(name = "authorizationEndpoint", description = "The authorization endpoint URL. Null while the instance has no OAuth2 client. The server publishes no OpenID discovery document, so an app takes the endpoints from here.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("authorizationEndpoint")
  public JsonNullable<String> getAuthorizationEndpoint() {
    return authorizationEndpoint;
  }

  public void setAuthorizationEndpoint(JsonNullable<String> authorizationEndpoint) {
    this.authorizationEndpoint = authorizationEndpoint;
  }

  public AppContextModel tokenEndpoint(String tokenEndpoint) {
    this.tokenEndpoint = JsonNullable.of(tokenEndpoint);
    return this;
  }

  /**
   * The token endpoint URL, for the code exchange and the refresh. Null while the instance has no OAuth2 client.
   * @return tokenEndpoint
   */
  
  @Schema(name = "tokenEndpoint", description = "The token endpoint URL, for the code exchange and the refresh. Null while the instance has no OAuth2 client.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("tokenEndpoint")
  public JsonNullable<String> getTokenEndpoint() {
    return tokenEndpoint;
  }

  public void setTokenEndpoint(JsonNullable<String> tokenEndpoint) {
    this.tokenEndpoint = tokenEndpoint;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppContextModel appContext = (AppContextModel) o;
    return Objects.equals(this.appId, appContext.appId) &&
        Objects.equals(this.appName, appContext.appName) &&
        Objects.equals(this.appInstanceId, appContext.appInstanceId) &&
        Objects.equals(this.appInstanceName, appContext.appInstanceName) &&
        Objects.equals(this.environment, appContext.environment) &&
        Objects.equals(this._configuration, appContext._configuration) &&
        Objects.equals(this.userAuthMode, appContext.userAuthMode) &&
        equalsNullable(this.issuer, appContext.issuer) &&
        equalsNullable(this.clientId, appContext.clientId) &&
        equalsNullable(this.authorizationEndpoint, appContext.authorizationEndpoint) &&
        equalsNullable(this.tokenEndpoint, appContext.tokenEndpoint);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(appId, appName, appInstanceId, appInstanceName, environment, _configuration, userAuthMode, hashCodeNullable(issuer), hashCodeNullable(clientId), hashCodeNullable(authorizationEndpoint), hashCodeNullable(tokenEndpoint));
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppContextModel {\n");
    sb.append("    appId: ").append(toIndentedString(appId)).append("\n");
    sb.append("    appName: ").append(toIndentedString(appName)).append("\n");
    sb.append("    appInstanceId: ").append(toIndentedString(appInstanceId)).append("\n");
    sb.append("    appInstanceName: ").append(toIndentedString(appInstanceName)).append("\n");
    sb.append("    environment: ").append(toIndentedString(environment)).append("\n");
    sb.append("    _configuration: ").append(toIndentedString(_configuration)).append("\n");
    sb.append("    userAuthMode: ").append(toIndentedString(userAuthMode)).append("\n");
    sb.append("    issuer: ").append(toIndentedString(issuer)).append("\n");
    sb.append("    clientId: ").append(toIndentedString(clientId)).append("\n");
    sb.append("    authorizationEndpoint: ").append(toIndentedString(authorizationEndpoint)).append("\n");
    sb.append("    tokenEndpoint: ").append(toIndentedString(tokenEndpoint)).append("\n");
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

