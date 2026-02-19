package com.bytechef.ee.embedded.connected.user.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.connected.user.web.rest.model.ConnectedUserIntegrationInstanceModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ConnectedUserModel
 */

@JsonTypeName("ConnectedUser")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-02T08:44:56.368618+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class ConnectedUserModel {

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private @Nullable String email;

  private @Nullable Boolean enabled;

  private @Nullable Long environmentId;

  private String externalId;

  private @Nullable Long id;

  @Valid
  private List<@Valid ConnectedUserIntegrationInstanceModel> integrationInstances = new ArrayList<>();

  @Valid
  private Map<String, Object> metadata = new HashMap<>();

  private @Nullable String name;

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  private @Nullable Integer version;

  public ConnectedUserModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ConnectedUserModel(String externalId) {
    this.externalId = externalId;
  }

  public ConnectedUserModel createdBy(@Nullable String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * The created by.
   * @return createdBy
   */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The created by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdBy")
  public @Nullable String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(@Nullable String createdBy) {
    this.createdBy = createdBy;
  }

  public ConnectedUserModel createdDate(@Nullable OffsetDateTime createdDate) {
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
  public @Nullable OffsetDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(@Nullable OffsetDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public ConnectedUserModel email(@Nullable String email) {
    this.email = email;
    return this;
  }

  /**
   * The email address.
   * @return email
   */
  
  @Schema(name = "email", description = "The email address.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("email")
  public @Nullable String getEmail() {
    return email;
  }

  public void setEmail(@Nullable String email) {
    this.email = email;
  }

  public ConnectedUserModel enabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If a connected user is enabled or not
   * @return enabled
   */
  
  @Schema(name = "enabled", description = "If a connected user is enabled or not", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public @Nullable Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
  }

  public ConnectedUserModel environmentId(@Nullable Long environmentId) {
    this.environmentId = environmentId;
    return this;
  }

  /**
   * The id of an environment.
   * @return environmentId
   */
  
  @Schema(name = "environmentId", description = "The id of an environment.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("environmentId")
  public @Nullable Long getEnvironmentId() {
    return environmentId;
  }

  public void setEnvironmentId(@Nullable Long environmentId) {
    this.environmentId = environmentId;
  }

  public ConnectedUserModel externalId(String externalId) {
    this.externalId = externalId;
    return this;
  }

  /**
   * The connected user external id.
   * @return externalId
   */
  
  @Schema(name = "externalId", accessMode = Schema.AccessMode.READ_ONLY, description = "The connected user external id.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("externalId")
  public String getExternalId() {
    return externalId;
  }

  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  public ConnectedUserModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a connected user.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a connected user.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public ConnectedUserModel integrationInstances(List<@Valid ConnectedUserIntegrationInstanceModel> integrationInstances) {
    this.integrationInstances = integrationInstances;
    return this;
  }

  public ConnectedUserModel addIntegrationInstancesItem(ConnectedUserIntegrationInstanceModel integrationInstancesItem) {
    if (this.integrationInstances == null) {
      this.integrationInstances = new ArrayList<>();
    }
    this.integrationInstances.add(integrationInstancesItem);
    return this;
  }

  /**
   * Get integrationInstances
   * @return integrationInstances
   */
  @Valid 
  @Schema(name = "integrationInstances", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationInstances")
  public List<@Valid ConnectedUserIntegrationInstanceModel> getIntegrationInstances() {
    return integrationInstances;
  }

  public void setIntegrationInstances(List<@Valid ConnectedUserIntegrationInstanceModel> integrationInstances) {
    this.integrationInstances = integrationInstances;
  }

  public ConnectedUserModel metadata(Map<String, Object> metadata) {
    this.metadata = metadata;
    return this;
  }

  public ConnectedUserModel putMetadataItem(String key, Object metadataItem) {
    if (this.metadata == null) {
      this.metadata = new HashMap<>();
    }
    this.metadata.put(key, metadataItem);
    return this;
  }

  /**
   * Get metadata
   * @return metadata
   */
  
  @Schema(name = "metadata", accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("metadata")
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  public ConnectedUserModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of a connection.
   * @return name
   */
  
  @Schema(name = "name", description = "The name of a connection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }

  public ConnectedUserModel lastModifiedBy(@Nullable String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  /**
   * The last modified by.
   * @return lastModifiedBy
   */
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedBy")
  public @Nullable String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(@Nullable String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public ConnectedUserModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
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
  public @Nullable OffsetDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public ConnectedUserModel version(@Nullable Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * @return version
   */
  
  @Schema(name = "__version", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("__version")
  public @Nullable Integer getVersion() {
    return version;
  }

  public void setVersion(@Nullable Integer version) {
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
    ConnectedUserModel connectedUser = (ConnectedUserModel) o;
    return Objects.equals(this.createdBy, connectedUser.createdBy) &&
        Objects.equals(this.createdDate, connectedUser.createdDate) &&
        Objects.equals(this.email, connectedUser.email) &&
        Objects.equals(this.enabled, connectedUser.enabled) &&
        Objects.equals(this.environmentId, connectedUser.environmentId) &&
        Objects.equals(this.externalId, connectedUser.externalId) &&
        Objects.equals(this.id, connectedUser.id) &&
        Objects.equals(this.integrationInstances, connectedUser.integrationInstances) &&
        Objects.equals(this.metadata, connectedUser.metadata) &&
        Objects.equals(this.name, connectedUser.name) &&
        Objects.equals(this.lastModifiedBy, connectedUser.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, connectedUser.lastModifiedDate) &&
        Objects.equals(this.version, connectedUser.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, email, enabled, environmentId, externalId, id, integrationInstances, metadata, name, lastModifiedBy, lastModifiedDate, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectedUserModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    environmentId: ").append(toIndentedString(environmentId)).append("\n");
    sb.append("    externalId: ").append(toIndentedString(externalId)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    integrationInstances: ").append(toIndentedString(integrationInstances)).append("\n");
    sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
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

