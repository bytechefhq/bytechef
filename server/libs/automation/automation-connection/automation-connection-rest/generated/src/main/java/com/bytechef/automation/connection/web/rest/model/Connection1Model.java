package com.bytechef.automation.connection.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.connection.web.rest.model.TagModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDateTime;
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

@Schema(name = "Connection_1", description = "Contains all required information to open a connection to a service defined by componentName parameter.")
@JsonTypeName("Connection_1")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-08-09T17:58:10.189796+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public class Connection1Model {

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
  private LocalDateTime createdDate;

  private com.bytechef.platform.connection.web.rest.model.CredentialStatusModel credentialStatus;

  private com.bytechef.platform.connection.web.rest.model.ConnectionEnvironmentModel environment;

  private Long id;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  private String name;

  @Valid
  private Map<String, Object> parameters = new HashMap<>();

  @Valid
  private List<@Valid TagModel> tags = new ArrayList<>();

  private Integer version;

  public Connection1Model() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Connection1Model(String componentName, String name, Map<String, Object> parameters) {
    this.componentName = componentName;
    this.name = name;
    this.parameters = parameters;
  }

  public Connection1Model active(Boolean active) {
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

  public Connection1Model authorizationName(String authorizationName) {
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

  public Connection1Model authorizationParameters(Map<String, Object> authorizationParameters) {
    this.authorizationParameters = authorizationParameters;
    return this;
  }

  public Connection1Model putAuthorizationParametersItem(String key, Object authorizationParametersItem) {
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

  public Connection1Model componentName(String componentName) {
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

  public Connection1Model connectionParameters(Map<String, Object> connectionParameters) {
    this.connectionParameters = connectionParameters;
    return this;
  }

  public Connection1Model putConnectionParametersItem(String key, Object connectionParametersItem) {
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

  public Connection1Model connectionVersion(Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
    return this;
  }

  /**
   * The version of a component that uses this connection.
   * @return connectionVersion
  */
  
  @Schema(name = "connectionVersion", description = "The version of a component that uses this connection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionVersion")
  public Integer getConnectionVersion() {
    return connectionVersion;
  }

  public void setConnectionVersion(Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
  }

  public Connection1Model createdBy(String createdBy) {
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

  public Connection1Model createdDate(LocalDateTime createdDate) {
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
  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public Connection1Model credentialStatus(com.bytechef.platform.connection.web.rest.model.CredentialStatusModel credentialStatus) {
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
  public com.bytechef.platform.connection.web.rest.model.CredentialStatusModel getCredentialStatus() {
    return credentialStatus;
  }

  public void setCredentialStatus(com.bytechef.platform.connection.web.rest.model.CredentialStatusModel credentialStatus) {
    this.credentialStatus = credentialStatus;
  }

  public Connection1Model environment(com.bytechef.platform.connection.web.rest.model.ConnectionEnvironmentModel environment) {
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
  public com.bytechef.platform.connection.web.rest.model.ConnectionEnvironmentModel getEnvironment() {
    return environment;
  }

  public void setEnvironment(com.bytechef.platform.connection.web.rest.model.ConnectionEnvironmentModel environment) {
    this.environment = environment;
  }

  public Connection1Model id(Long id) {
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

  public Connection1Model lastModifiedBy(String lastModifiedBy) {
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

  public Connection1Model lastModifiedDate(LocalDateTime lastModifiedDate) {
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
  public LocalDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public Connection1Model name(String name) {
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

  public Connection1Model parameters(Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  public Connection1Model putParametersItem(String key, Object parametersItem) {
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

  public Connection1Model tags(List<@Valid TagModel> tags) {
    this.tags = tags;
    return this;
  }

  public Connection1Model addTagsItem(TagModel tagsItem) {
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

  public Connection1Model version(Integer version) {
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
    Connection1Model connection1 = (Connection1Model) o;
    return Objects.equals(this.active, connection1.active) &&
        Objects.equals(this.authorizationName, connection1.authorizationName) &&
        Objects.equals(this.authorizationParameters, connection1.authorizationParameters) &&
        Objects.equals(this.componentName, connection1.componentName) &&
        Objects.equals(this.connectionParameters, connection1.connectionParameters) &&
        Objects.equals(this.connectionVersion, connection1.connectionVersion) &&
        Objects.equals(this.createdBy, connection1.createdBy) &&
        Objects.equals(this.createdDate, connection1.createdDate) &&
        Objects.equals(this.credentialStatus, connection1.credentialStatus) &&
        Objects.equals(this.environment, connection1.environment) &&
        Objects.equals(this.id, connection1.id) &&
        Objects.equals(this.lastModifiedBy, connection1.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, connection1.lastModifiedDate) &&
        Objects.equals(this.name, connection1.name) &&
        Objects.equals(this.parameters, connection1.parameters) &&
        Objects.equals(this.tags, connection1.tags) &&
        Objects.equals(this.version, connection1.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(active, authorizationName, authorizationParameters, componentName, connectionParameters, connectionVersion, createdBy, createdDate, credentialStatus, environment, id, lastModifiedBy, lastModifiedDate, name, parameters, tags, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Connection1Model {\n");
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

