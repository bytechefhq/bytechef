package com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.EnvironmentModel;
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
 * An API collection.
 */

@Schema(name = "ApiCollection", description = "An API collection.")
@JsonTypeName("ApiCollection")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T01:43:17.216712+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ApiCollectionModel {

  private @Nullable Long id;

  private @Nullable String name;

  private @Nullable String description;

  private @Nullable String contextPath;

  private @Nullable Integer collectionVersion;

  private @Nullable Long projectId;

  private @Nullable Integer projectVersion;

  private @Nullable EnvironmentModel environment;

  private @Nullable Boolean enabled;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  public ApiCollectionModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  
  @Schema(name = "id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public ApiCollectionModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  
  @Schema(name = "name", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public ApiCollectionModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
   */
  
  @Schema(name = "description", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public ApiCollectionModel contextPath(@Nullable String contextPath) {
    this.contextPath = contextPath;
    return this;
  }

  /**
   * The path every endpoint of the collection is served under.
   * @return contextPath
   */
  
  @Schema(name = "contextPath", description = "The path every endpoint of the collection is served under.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("contextPath")
  public @Nullable String getContextPath() {
    return contextPath;
  }

  @JsonProperty("contextPath")
  public void setContextPath(@Nullable String contextPath) {
    this.contextPath = contextPath;
  }

  public ApiCollectionModel collectionVersion(@Nullable Integer collectionVersion) {
    this.collectionVersion = collectionVersion;
    return this;
  }

  /**
   * Get collectionVersion
   * @return collectionVersion
   */
  
  @Schema(name = "collectionVersion", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("collectionVersion")
  public @Nullable Integer getCollectionVersion() {
    return collectionVersion;
  }

  @JsonProperty("collectionVersion")
  public void setCollectionVersion(@Nullable Integer collectionVersion) {
    this.collectionVersion = collectionVersion;
  }

  public ApiCollectionModel projectId(@Nullable Long projectId) {
    this.projectId = projectId;
    return this;
  }

  /**
   * Get projectId
   * @return projectId
   */
  
  @Schema(name = "projectId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectId")
  public @Nullable Long getProjectId() {
    return projectId;
  }

  @JsonProperty("projectId")
  public void setProjectId(@Nullable Long projectId) {
    this.projectId = projectId;
  }

  public ApiCollectionModel projectVersion(@Nullable Integer projectVersion) {
    this.projectVersion = projectVersion;
    return this;
  }

  /**
   * Get projectVersion
   * @return projectVersion
   */
  
  @Schema(name = "projectVersion", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectVersion")
  public @Nullable Integer getProjectVersion() {
    return projectVersion;
  }

  @JsonProperty("projectVersion")
  public void setProjectVersion(@Nullable Integer projectVersion) {
    this.projectVersion = projectVersion;
  }

  public ApiCollectionModel environment(@Nullable EnvironmentModel environment) {
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
  public @Nullable EnvironmentModel getEnvironment() {
    return environment;
  }

  @JsonProperty("environment")
  public void setEnvironment(@Nullable EnvironmentModel environment) {
    this.environment = environment;
  }

  public ApiCollectionModel enabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Get enabled
   * @return enabled
   */
  
  @Schema(name = "enabled", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public @Nullable Boolean getEnabled() {
    return enabled;
  }

  @JsonProperty("enabled")
  public void setEnabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
  }

  public ApiCollectionModel createdDate(@Nullable OffsetDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  /**
   * Get createdDate
   * @return createdDate
   */
  @Valid 
  @Schema(name = "createdDate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdDate")
  public @Nullable OffsetDateTime getCreatedDate() {
    return createdDate;
  }

  @JsonProperty("createdDate")
  public void setCreatedDate(@Nullable OffsetDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public ApiCollectionModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
    return this;
  }

  /**
   * Get lastModifiedDate
   * @return lastModifiedDate
   */
  @Valid 
  @Schema(name = "lastModifiedDate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedDate")
  public @Nullable OffsetDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  @JsonProperty("lastModifiedDate")
  public void setLastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiCollectionModel apiCollection = (ApiCollectionModel) o;
    return Objects.equals(this.id, apiCollection.id) &&
        Objects.equals(this.name, apiCollection.name) &&
        Objects.equals(this.description, apiCollection.description) &&
        Objects.equals(this.contextPath, apiCollection.contextPath) &&
        Objects.equals(this.collectionVersion, apiCollection.collectionVersion) &&
        Objects.equals(this.projectId, apiCollection.projectId) &&
        Objects.equals(this.projectVersion, apiCollection.projectVersion) &&
        Objects.equals(this.environment, apiCollection.environment) &&
        Objects.equals(this.enabled, apiCollection.enabled) &&
        Objects.equals(this.createdDate, apiCollection.createdDate) &&
        Objects.equals(this.lastModifiedDate, apiCollection.lastModifiedDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description, contextPath, collectionVersion, projectId, projectVersion, environment, enabled, createdDate, lastModifiedDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCollectionModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    contextPath: ").append(toIndentedString(contextPath)).append("\n");
    sb.append("    collectionVersion: ").append(toIndentedString(collectionVersion)).append("\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    projectVersion: ").append(toIndentedString(projectVersion)).append("\n");
    sb.append("    environment: ").append(toIndentedString(environment)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
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

