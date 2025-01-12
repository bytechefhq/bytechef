package com.bytechef.embedded.connection.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.embedded.connection.web.rest.model.ConnectionEnvironmentModel;
import com.bytechef.embedded.connection.web.rest.model.CredentialStatusModel;
import com.bytechef.platform.tag.web.rest.model.TagModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
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

@Schema(name = "connection_base", description = "Contains all required information to open a connection to a service defined by componentName parameter.")
@JsonTypeName("connection_base")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-01-07T09:20:56.575529+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public class ConnectionBaseModel {

  private Boolean active;

  private String authorizationName;

  @Valid
  private Map<String, Object> authorizationParameters = new HashMap<>();

  private String componentName;

  @Valid
  private Map<String, Object> connectionParameters = new HashMap<>();

  private Integer connectionVersion;

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdDate;

  private CredentialStatusModel credentialStatus;

  private ConnectionEnvironmentModel environment;

  private Long id;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime lastModifiedDate;

  private String name;

  @Valid
  private Map<String, Object> parameters = new HashMap<>();

  @Valid
  private List<@Valid TagModel> tags = new ArrayList<>();

  private Integer version;

  public ConnectionBaseModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ConnectionBaseModel(String componentName, Integer connectionVersion, String name, Map<String, Object> parameters) {
    this.componentName = componentName;
    this.connectionVersion = connectionVersion;
    this.name = name;
    this.parameters = parameters;
  }

  public ConnectionBaseModel active(Boolean active) {
    this.active = active;
    return this;
  }

  /**
   * If a connection is used in any of active workflows.
   * @return active
   */
  
  @Schema(name = "active", accessMode = Schema.AccessMode.READ_ONLY, description = "If a connection is used in any of active workflows.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("active")
  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public ConnectionBaseModel authorizationName(String authorizationName) {
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

  public ConnectionBaseModel authorizationParameters(Map<String, Object> authorizationParameters) {
    this.authorizationParameters = authorizationParameters;
    return this;
  }

  public ConnectionBaseModel putAuthorizationParametersItem(String key, Object authorizationParametersItem) {
    if (this.authorizationParameters == null) {
      this.authorizationParameters = new HashMap<>();
    }
    this.authorizationParameters.put(key, authorizationParametersItem);
    return this;
  }

  /**
   * The authorization parameters of a connection.
   * @return authorizationParameters
   */
  
  @Schema(name = "authorizationParameters", accessMode = Schema.AccessMode.READ_ONLY, description = "The authorization parameters of a connection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("authorizationParameters")
  public Map<String, Object> getAuthorizationParameters() {
    return authorizationParameters;
  }

  public void setAuthorizationParameters(Map<String, Object> authorizationParameters) {
    this.authorizationParameters = authorizationParameters;
  }

  public ConnectionBaseModel componentName(String componentName) {
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

  public ConnectionBaseModel connectionParameters(Map<String, Object> connectionParameters) {
    this.connectionParameters = connectionParameters;
    return this;
  }

  public ConnectionBaseModel putConnectionParametersItem(String key, Object connectionParametersItem) {
    if (this.connectionParameters == null) {
      this.connectionParameters = new HashMap<>();
    }
    this.connectionParameters.put(key, connectionParametersItem);
    return this;
  }

  /**
   * The connection parameters of a connection.
   * @return connectionParameters
   */
  
  @Schema(name = "connectionParameters", accessMode = Schema.AccessMode.READ_ONLY, description = "The connection parameters of a connection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionParameters")
  public Map<String, Object> getConnectionParameters() {
    return connectionParameters;
  }

  public void setConnectionParameters(Map<String, Object> connectionParameters) {
    this.connectionParameters = connectionParameters;
  }

  public ConnectionBaseModel connectionVersion(Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
    return this;
  }

  /**
   * The version of a component that uses this connection.
   * @return connectionVersion
   */
  @NotNull 
  @Schema(name = "connectionVersion", description = "The version of a component that uses this connection.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connectionVersion")
  public Integer getConnectionVersion() {
    return connectionVersion;
  }

  public void setConnectionVersion(Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
  }

  public ConnectionBaseModel createdBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * The created by.
   * @return createdBy
   */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The created by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdBy")
  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public ConnectionBaseModel createdDate(OffsetDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  /**
   * The created date.
   * @return createdDate
   */
  @Valid 
  @Schema(name = "createdDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The created date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdDate")
  public OffsetDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(OffsetDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public ConnectionBaseModel credentialStatus(CredentialStatusModel credentialStatus) {
    this.credentialStatus = credentialStatus;
    return this;
  }

  /**
   * Get credentialStatus
   * @return credentialStatus
   */
  @Valid 
  @Schema(name = "credentialStatus", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("credentialStatus")
  public CredentialStatusModel getCredentialStatus() {
    return credentialStatus;
  }

  public void setCredentialStatus(CredentialStatusModel credentialStatus) {
    this.credentialStatus = credentialStatus;
  }

  public ConnectionBaseModel environment(ConnectionEnvironmentModel environment) {
    this.environment = environment;
    return this;
  }

  /**
   * Get environment
   * @return environment
   */
  @Valid 
  @Schema(name = "environment", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("environment")
  public ConnectionEnvironmentModel getEnvironment() {
    return environment;
  }

  public void setEnvironment(ConnectionEnvironmentModel environment) {
    this.environment = environment;
  }

  public ConnectionBaseModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a connection.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a connection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ConnectionBaseModel lastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  /**
   * The last modified by.
   * @return lastModifiedBy
   */
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedBy")
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public ConnectionBaseModel lastModifiedDate(OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
    return this;
  }

  /**
   * The last modified date.
   * @return lastModifiedDate
   */
  @Valid 
  @Schema(name = "lastModifiedDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedDate")
  public OffsetDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public ConnectionBaseModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of a connection.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The name of a connection.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ConnectionBaseModel parameters(Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  public ConnectionBaseModel putParametersItem(String key, Object parametersItem) {
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

  public ConnectionBaseModel tags(List<@Valid TagModel> tags) {
    this.tags = tags;
    return this;
  }

  public ConnectionBaseModel addTagsItem(TagModel tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Get tags
   * @return tags
   */
  @Valid 
  @Schema(name = "tags", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("tags")
  public List<@Valid TagModel> getTags() {
    return tags;
  }

  public void setTags(List<@Valid TagModel> tags) {
    this.tags = tags;
  }

  public ConnectionBaseModel version(Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * @return version
   */
  
  @Schema(name = "__version", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("__version")
  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConnectionBaseModel connectionBase = (ConnectionBaseModel) o;
    return Objects.equals(this.active, connectionBase.active) &&
        Objects.equals(this.authorizationName, connectionBase.authorizationName) &&
        Objects.equals(this.authorizationParameters, connectionBase.authorizationParameters) &&
        Objects.equals(this.componentName, connectionBase.componentName) &&
        Objects.equals(this.connectionParameters, connectionBase.connectionParameters) &&
        Objects.equals(this.connectionVersion, connectionBase.connectionVersion) &&
        Objects.equals(this.createdBy, connectionBase.createdBy) &&
        Objects.equals(this.createdDate, connectionBase.createdDate) &&
        Objects.equals(this.credentialStatus, connectionBase.credentialStatus) &&
        Objects.equals(this.environment, connectionBase.environment) &&
        Objects.equals(this.id, connectionBase.id) &&
        Objects.equals(this.lastModifiedBy, connectionBase.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, connectionBase.lastModifiedDate) &&
        Objects.equals(this.name, connectionBase.name) &&
        Objects.equals(this.parameters, connectionBase.parameters) &&
        Objects.equals(this.tags, connectionBase.tags) &&
        Objects.equals(this.version, connectionBase.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(active, authorizationName, authorizationParameters, componentName, connectionParameters, connectionVersion, createdBy, createdDate, credentialStatus, environment, id, lastModifiedBy, lastModifiedDate, name, parameters, tags, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectionBaseModel {\n");
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
    sb.append("    authorizationName: ").append(toIndentedString(authorizationName)).append("\n");
    sb.append("    authorizationParameters: ").append(toIndentedString(authorizationParameters)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    connectionParameters: ").append(toIndentedString(connectionParameters)).append("\n");
    sb.append("    connectionVersion: ").append(toIndentedString(connectionVersion)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    credentialStatus: ").append(toIndentedString(credentialStatus)).append("\n");
    sb.append("    environment: ").append(toIndentedString(environment)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
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

