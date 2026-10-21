package com.bytechef.ee.automation.app.public_.web.rest.management.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppHostingTargetModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppStatusModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppVisibilityModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.EnvironmentModel;
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
 * An app. It never carries the secret key, only a hint (the key&#39;s last characters).
 */

@Schema(name = "App", description = "An app. It never carries the secret key, only a hint (the key's last characters).")
@JsonTypeName("App")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:30.252991+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class AppModel {

  private @Nullable Long id;

  private @Nullable String uuid;

  private @Nullable String name;

  private @Nullable String description;

  private @Nullable Long workspaceId;

  private @Nullable EnvironmentModel environment;

  private @Nullable Long ownerUserId;

  private @Nullable AppStatusModel status;

  private @Nullable AppVisibilityModel visibility;

  private @Nullable AppHostingTargetModel hostingTarget;

  private @Nullable String url;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastDeployedDate;

  private @Nullable String secretKeyHint;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime secretKeyLastUsedDate;

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  private @Nullable Integer version;

  public AppModel id(@Nullable Long id) {
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

  public AppModel uuid(@Nullable String uuid) {
    this.uuid = uuid;
    return this;
  }

  /**
   * Get uuid
   * @return uuid
   */
  
  @Schema(name = "uuid", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("uuid")
  public @Nullable String getUuid() {
    return uuid;
  }

  @JsonProperty("uuid")
  public void setUuid(@Nullable String uuid) {
    this.uuid = uuid;
  }

  public AppModel name(@Nullable String name) {
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

  public AppModel description(@Nullable String description) {
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

  public AppModel workspaceId(@Nullable Long workspaceId) {
    this.workspaceId = workspaceId;
    return this;
  }

  /**
   * Get workspaceId
   * @return workspaceId
   */
  
  @Schema(name = "workspaceId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workspaceId")
  public @Nullable Long getWorkspaceId() {
    return workspaceId;
  }

  @JsonProperty("workspaceId")
  public void setWorkspaceId(@Nullable Long workspaceId) {
    this.workspaceId = workspaceId;
  }

  public AppModel environment(@Nullable EnvironmentModel environment) {
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

  public AppModel ownerUserId(@Nullable Long ownerUserId) {
    this.ownerUserId = ownerUserId;
    return this;
  }

  /**
   * Get ownerUserId
   * @return ownerUserId
   */
  
  @Schema(name = "ownerUserId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("ownerUserId")
  public @Nullable Long getOwnerUserId() {
    return ownerUserId;
  }

  @JsonProperty("ownerUserId")
  public void setOwnerUserId(@Nullable Long ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public AppModel status(@Nullable AppStatusModel status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @Valid 
  @Schema(name = "status", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("status")
  public @Nullable AppStatusModel getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable AppStatusModel status) {
    this.status = status;
  }

  public AppModel visibility(@Nullable AppVisibilityModel visibility) {
    this.visibility = visibility;
    return this;
  }

  /**
   * Get visibility
   * @return visibility
   */
  @Valid 
  @Schema(name = "visibility", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("visibility")
  public @Nullable AppVisibilityModel getVisibility() {
    return visibility;
  }

  @JsonProperty("visibility")
  public void setVisibility(@Nullable AppVisibilityModel visibility) {
    this.visibility = visibility;
  }

  public AppModel hostingTarget(@Nullable AppHostingTargetModel hostingTarget) {
    this.hostingTarget = hostingTarget;
    return this;
  }

  /**
   * Get hostingTarget
   * @return hostingTarget
   */
  @Valid 
  @Schema(name = "hostingTarget", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("hostingTarget")
  public @Nullable AppHostingTargetModel getHostingTarget() {
    return hostingTarget;
  }

  @JsonProperty("hostingTarget")
  public void setHostingTarget(@Nullable AppHostingTargetModel hostingTarget) {
    this.hostingTarget = hostingTarget;
  }

  public AppModel url(@Nullable String url) {
    this.url = url;
    return this;
  }

  /**
   * Get url
   * @return url
   */
  
  @Schema(name = "url", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("url")
  public @Nullable String getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(@Nullable String url) {
    this.url = url;
  }

  public AppModel lastDeployedDate(@Nullable OffsetDateTime lastDeployedDate) {
    this.lastDeployedDate = lastDeployedDate;
    return this;
  }

  /**
   * Get lastDeployedDate
   * @return lastDeployedDate
   */
  @Valid 
  @Schema(name = "lastDeployedDate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastDeployedDate")
  public @Nullable OffsetDateTime getLastDeployedDate() {
    return lastDeployedDate;
  }

  @JsonProperty("lastDeployedDate")
  public void setLastDeployedDate(@Nullable OffsetDateTime lastDeployedDate) {
    this.lastDeployedDate = lastDeployedDate;
  }

  public AppModel secretKeyHint(@Nullable String secretKeyHint) {
    this.secretKeyHint = secretKeyHint;
    return this;
  }

  /**
   * Get secretKeyHint
   * @return secretKeyHint
   */
  
  @Schema(name = "secretKeyHint", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("secretKeyHint")
  public @Nullable String getSecretKeyHint() {
    return secretKeyHint;
  }

  @JsonProperty("secretKeyHint")
  public void setSecretKeyHint(@Nullable String secretKeyHint) {
    this.secretKeyHint = secretKeyHint;
  }

  public AppModel secretKeyLastUsedDate(@Nullable OffsetDateTime secretKeyLastUsedDate) {
    this.secretKeyLastUsedDate = secretKeyLastUsedDate;
    return this;
  }

  /**
   * Get secretKeyLastUsedDate
   * @return secretKeyLastUsedDate
   */
  @Valid 
  @Schema(name = "secretKeyLastUsedDate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("secretKeyLastUsedDate")
  public @Nullable OffsetDateTime getSecretKeyLastUsedDate() {
    return secretKeyLastUsedDate;
  }

  @JsonProperty("secretKeyLastUsedDate")
  public void setSecretKeyLastUsedDate(@Nullable OffsetDateTime secretKeyLastUsedDate) {
    this.secretKeyLastUsedDate = secretKeyLastUsedDate;
  }

  public AppModel createdBy(@Nullable String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * Get createdBy
   * @return createdBy
   */
  
  @Schema(name = "createdBy", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdBy")
  public @Nullable String getCreatedBy() {
    return createdBy;
  }

  @JsonProperty("createdBy")
  public void setCreatedBy(@Nullable String createdBy) {
    this.createdBy = createdBy;
  }

  public AppModel createdDate(@Nullable OffsetDateTime createdDate) {
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

  public AppModel lastModifiedBy(@Nullable String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  /**
   * Get lastModifiedBy
   * @return lastModifiedBy
   */
  
  @Schema(name = "lastModifiedBy", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedBy")
  public @Nullable String getLastModifiedBy() {
    return lastModifiedBy;
  }

  @JsonProperty("lastModifiedBy")
  public void setLastModifiedBy(@Nullable String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public AppModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
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

  public AppModel version(@Nullable Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * @return version
   */
  
  @Schema(name = "version", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("version")
  public @Nullable Integer getVersion() {
    return version;
  }

  @JsonProperty("version")
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
    AppModel app = (AppModel) o;
    return Objects.equals(this.id, app.id) &&
        Objects.equals(this.uuid, app.uuid) &&
        Objects.equals(this.name, app.name) &&
        Objects.equals(this.description, app.description) &&
        Objects.equals(this.workspaceId, app.workspaceId) &&
        Objects.equals(this.environment, app.environment) &&
        Objects.equals(this.ownerUserId, app.ownerUserId) &&
        Objects.equals(this.status, app.status) &&
        Objects.equals(this.visibility, app.visibility) &&
        Objects.equals(this.hostingTarget, app.hostingTarget) &&
        Objects.equals(this.url, app.url) &&
        Objects.equals(this.lastDeployedDate, app.lastDeployedDate) &&
        Objects.equals(this.secretKeyHint, app.secretKeyHint) &&
        Objects.equals(this.secretKeyLastUsedDate, app.secretKeyLastUsedDate) &&
        Objects.equals(this.createdBy, app.createdBy) &&
        Objects.equals(this.createdDate, app.createdDate) &&
        Objects.equals(this.lastModifiedBy, app.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, app.lastModifiedDate) &&
        Objects.equals(this.version, app.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, uuid, name, description, workspaceId, environment, ownerUserId, status, visibility, hostingTarget, url, lastDeployedDate, secretKeyHint, secretKeyLastUsedDate, createdBy, createdDate, lastModifiedBy, lastModifiedDate, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    workspaceId: ").append(toIndentedString(workspaceId)).append("\n");
    sb.append("    environment: ").append(toIndentedString(environment)).append("\n");
    sb.append("    ownerUserId: ").append(toIndentedString(ownerUserId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    visibility: ").append(toIndentedString(visibility)).append("\n");
    sb.append("    hostingTarget: ").append(toIndentedString(hostingTarget)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    lastDeployedDate: ").append(toIndentedString(lastDeployedDate)).append("\n");
    sb.append("    secretKeyHint: ").append(toIndentedString(secretKeyHint)).append("\n");
    sb.append("    secretKeyLastUsedDate: ").append(toIndentedString(secretKeyLastUsedDate)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
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
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

