package com.bytechef.ee.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.web.rest.model.AuthorizationTypeModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
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
 * Contains configurations and connections required for the execution of integration workflows.
 */

@Schema(name = "IntegrationInstanceConfigurationBasic", description = "Contains configurations and connections required for the execution of integration workflows.")
@JsonTypeName("IntegrationInstanceConfigurationBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-23T12:37:00.328650+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class IntegrationInstanceConfigurationBasicModel {

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private @Nullable String description;

  private @Nullable Boolean enabled;

  private @Nullable Long environmentId;

  private @Nullable Long id;

  private @Nullable Long integrationId;

  private @Nullable Integer integrationVersion;

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  private String name;

  private @Nullable AuthorizationTypeModel authorizationType;

  public IntegrationInstanceConfigurationBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public IntegrationInstanceConfigurationBasicModel(String name) {
    this.name = name;
  }

  public IntegrationInstanceConfigurationBasicModel createdBy(@Nullable String createdBy) {
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

  public IntegrationInstanceConfigurationBasicModel createdDate(@Nullable OffsetDateTime createdDate) {
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

  public IntegrationInstanceConfigurationBasicModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of an integration configuration.
   * @return description
   */
  
  @Schema(name = "description", description = "The description of an integration configuration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public IntegrationInstanceConfigurationBasicModel enabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If an integration instance configuration is enabled or not.
   * @return enabled
   */
  
  @Schema(name = "enabled", description = "If an integration instance configuration is enabled or not.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public @Nullable Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
  }

  public IntegrationInstanceConfigurationBasicModel environmentId(@Nullable Long environmentId) {
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

  public IntegrationInstanceConfigurationBasicModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an integration instance configuration.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an integration instance configuration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public IntegrationInstanceConfigurationBasicModel integrationId(@Nullable Long integrationId) {
    this.integrationId = integrationId;
    return this;
  }

  /**
   * Th id of an integration.
   * @return integrationId
   */
  
  @Schema(name = "integrationId", description = "Th id of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationId")
  public @Nullable Long getIntegrationId() {
    return integrationId;
  }

  public void setIntegrationId(@Nullable Long integrationId) {
    this.integrationId = integrationId;
  }

  public IntegrationInstanceConfigurationBasicModel integrationVersion(@Nullable Integer integrationVersion) {
    this.integrationVersion = integrationVersion;
    return this;
  }

  /**
   * The version of an integration.
   * @return integrationVersion
   */
  
  @Schema(name = "integrationVersion", description = "The version of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationVersion")
  public @Nullable Integer getIntegrationVersion() {
    return integrationVersion;
  }

  public void setIntegrationVersion(@Nullable Integer integrationVersion) {
    this.integrationVersion = integrationVersion;
  }

  public IntegrationInstanceConfigurationBasicModel lastModifiedBy(@Nullable String lastModifiedBy) {
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

  public IntegrationInstanceConfigurationBasicModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
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

  public IntegrationInstanceConfigurationBasicModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of an integration instance configuration.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The name of an integration instance configuration.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public IntegrationInstanceConfigurationBasicModel authorizationType(@Nullable AuthorizationTypeModel authorizationType) {
    this.authorizationType = authorizationType;
    return this;
  }

  /**
   * Get authorizationType
   * @return authorizationType
   */
  @Valid 
  @Schema(name = "authorizationType", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("authorizationType")
  public @Nullable AuthorizationTypeModel getAuthorizationType() {
    return authorizationType;
  }

  public void setAuthorizationType(@Nullable AuthorizationTypeModel authorizationType) {
    this.authorizationType = authorizationType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IntegrationInstanceConfigurationBasicModel integrationInstanceConfigurationBasic = (IntegrationInstanceConfigurationBasicModel) o;
    return Objects.equals(this.createdBy, integrationInstanceConfigurationBasic.createdBy) &&
        Objects.equals(this.createdDate, integrationInstanceConfigurationBasic.createdDate) &&
        Objects.equals(this.description, integrationInstanceConfigurationBasic.description) &&
        Objects.equals(this.enabled, integrationInstanceConfigurationBasic.enabled) &&
        Objects.equals(this.environmentId, integrationInstanceConfigurationBasic.environmentId) &&
        Objects.equals(this.id, integrationInstanceConfigurationBasic.id) &&
        Objects.equals(this.integrationId, integrationInstanceConfigurationBasic.integrationId) &&
        Objects.equals(this.integrationVersion, integrationInstanceConfigurationBasic.integrationVersion) &&
        Objects.equals(this.lastModifiedBy, integrationInstanceConfigurationBasic.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, integrationInstanceConfigurationBasic.lastModifiedDate) &&
        Objects.equals(this.name, integrationInstanceConfigurationBasic.name) &&
        Objects.equals(this.authorizationType, integrationInstanceConfigurationBasic.authorizationType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, description, enabled, environmentId, id, integrationId, integrationVersion, lastModifiedBy, lastModifiedDate, name, authorizationType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationInstanceConfigurationBasicModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    environmentId: ").append(toIndentedString(environmentId)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    integrationId: ").append(toIndentedString(integrationId)).append("\n");
    sb.append("    integrationVersion: ").append(toIndentedString(integrationVersion)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    authorizationType: ").append(toIndentedString(authorizationType)).append("\n");
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

