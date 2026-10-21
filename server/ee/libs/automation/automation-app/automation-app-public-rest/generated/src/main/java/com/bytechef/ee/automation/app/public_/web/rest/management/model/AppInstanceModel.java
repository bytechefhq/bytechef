package com.bytechef.ee.automation.app.public_.web.rest.management.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppHostingTargetModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppInstanceStatusModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppStatusModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppUserAuthModeModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppVisibilityModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.EnvironmentModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.HashMap;
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
 * An installation of an app in a workspace and environment. It never carries the secret key, only a hint (the key&#39;s last characters).
 */

@Schema(name = "AppInstance", description = "An installation of an app in a workspace and environment. It never carries the secret key, only a hint (the key's last characters).")
@JsonTypeName("AppInstance")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-16T22:08:52.163262+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class AppInstanceModel {

  private @Nullable Long id;

  private @Nullable String uuid;

  private @Nullable Long appId;

  private @Nullable String appName;

  private @Nullable Boolean appHasIcon;

  private @Nullable AppStatusModel appStatus;

  private @Nullable Long workspaceId;

  private @Nullable EnvironmentModel environment;

  private @Nullable String name;

  private @Nullable Long ownerUserId;

  private @Nullable AppInstanceStatusModel status;

  private @Nullable AppVisibilityModel visibility;

  private @Nullable AppHostingTargetModel hostingTarget;

  private @Nullable String url;

  private @Nullable String secretKeyHint;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime secretKeyLastUsedDate;

  private @Nullable AppUserAuthModeModel userAuthMode;

  private Map<String, Object> _configuration = new HashMap<>();

  private @Nullable Long currentAppDeploymentId;

  private @Nullable Long deploymentConnectionId;

  private Map<String, Object> deploymentSettings = new HashMap<>();

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  private @Nullable Integer version;

  public AppInstanceModel id(@Nullable Long id) {
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

  public AppInstanceModel uuid(@Nullable String uuid) {
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

  public AppInstanceModel appId(@Nullable Long appId) {
    this.appId = appId;
    return this;
  }

  /**
   * Get appId
   * @return appId
   */
  
  @Schema(name = "appId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("appId")
  public @Nullable Long getAppId() {
    return appId;
  }

  @JsonProperty("appId")
  public void setAppId(@Nullable Long appId) {
    this.appId = appId;
  }

  public AppInstanceModel appName(@Nullable String appName) {
    this.appName = appName;
    return this;
  }

  /**
   * Get appName
   * @return appName
   */
  
  @Schema(name = "appName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("appName")
  public @Nullable String getAppName() {
    return appName;
  }

  @JsonProperty("appName")
  public void setAppName(@Nullable String appName) {
    this.appName = appName;
  }

  public AppInstanceModel appHasIcon(@Nullable Boolean appHasIcon) {
    this.appHasIcon = appHasIcon;
    return this;
  }

  /**
   * Whether the instance's app has an icon.
   * @return appHasIcon
   */
  
  @Schema(name = "appHasIcon", description = "Whether the instance's app has an icon.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("appHasIcon")
  public @Nullable Boolean getAppHasIcon() {
    return appHasIcon;
  }

  @JsonProperty("appHasIcon")
  public void setAppHasIcon(@Nullable Boolean appHasIcon) {
    this.appHasIcon = appHasIcon;
  }

  public AppInstanceModel appStatus(@Nullable AppStatusModel appStatus) {
    this.appStatus = appStatus;
    return this;
  }

  /**
   * Get appStatus
   * @return appStatus
   */
  @Valid 
  @Schema(name = "appStatus", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("appStatus")
  public @Nullable AppStatusModel getAppStatus() {
    return appStatus;
  }

  @JsonProperty("appStatus")
  public void setAppStatus(@Nullable AppStatusModel appStatus) {
    this.appStatus = appStatus;
  }

  public AppInstanceModel workspaceId(@Nullable Long workspaceId) {
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

  public AppInstanceModel environment(@Nullable EnvironmentModel environment) {
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

  public AppInstanceModel name(@Nullable String name) {
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

  public AppInstanceModel ownerUserId(@Nullable Long ownerUserId) {
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

  public AppInstanceModel status(@Nullable AppInstanceStatusModel status) {
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
  public @Nullable AppInstanceStatusModel getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable AppInstanceStatusModel status) {
    this.status = status;
  }

  public AppInstanceModel visibility(@Nullable AppVisibilityModel visibility) {
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

  public AppInstanceModel hostingTarget(@Nullable AppHostingTargetModel hostingTarget) {
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

  public AppInstanceModel url(@Nullable String url) {
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

  public AppInstanceModel secretKeyHint(@Nullable String secretKeyHint) {
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

  public AppInstanceModel secretKeyLastUsedDate(@Nullable OffsetDateTime secretKeyLastUsedDate) {
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

  public AppInstanceModel userAuthMode(@Nullable AppUserAuthModeModel userAuthMode) {
    this.userAuthMode = userAuthMode;
    return this;
  }

  /**
   * Get userAuthMode
   * @return userAuthMode
   */
  @Valid 
  @Schema(name = "userAuthMode", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("userAuthMode")
  public @Nullable AppUserAuthModeModel getUserAuthMode() {
    return userAuthMode;
  }

  @JsonProperty("userAuthMode")
  public void setUserAuthMode(@Nullable AppUserAuthModeModel userAuthMode) {
    this.userAuthMode = userAuthMode;
  }

  public AppInstanceModel _configuration(Map<String, Object> _configuration) {
    this._configuration = _configuration;
    return this;
  }

  public AppInstanceModel putConfigurationItem(String key, Object _configurationItem) {
    if (this._configuration == null) {
      this._configuration = new HashMap<>();
    }
    this._configuration.put(key, _configurationItem);
    return this;
  }

  /**
   * The instance's configuration, a JSON object of at most 64 KB. It is visible to everyone who can view the instance and must not hold secrets; credentials belong in connections.
   * @return _configuration
   */
  
  @Schema(name = "configuration", description = "The instance's configuration, a JSON object of at most 64 KB. It is visible to everyone who can view the instance and must not hold secrets; credentials belong in connections.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("configuration")
  public Map<String, Object> getConfiguration() {
    return _configuration;
  }

  @JsonProperty("configuration")
  public void setConfiguration(Map<String, Object> _configuration) {
    this._configuration = _configuration;
  }

  public AppInstanceModel currentAppDeploymentId(@Nullable Long currentAppDeploymentId) {
    this.currentAppDeploymentId = currentAppDeploymentId;
    return this;
  }

  /**
   * Get currentAppDeploymentId
   * @return currentAppDeploymentId
   */
  
  @Schema(name = "currentAppDeploymentId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("currentAppDeploymentId")
  public @Nullable Long getCurrentAppDeploymentId() {
    return currentAppDeploymentId;
  }

  @JsonProperty("currentAppDeploymentId")
  public void setCurrentAppDeploymentId(@Nullable Long currentAppDeploymentId) {
    this.currentAppDeploymentId = currentAppDeploymentId;
  }

  public AppInstanceModel deploymentConnectionId(@Nullable Long deploymentConnectionId) {
    this.deploymentConnectionId = deploymentConnectionId;
    return this;
  }

  /**
   * The connection the deployment provider of the hosting target uses, if one is set.
   * @return deploymentConnectionId
   */
  
  @Schema(name = "deploymentConnectionId", description = "The connection the deployment provider of the hosting target uses, if one is set.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("deploymentConnectionId")
  public @Nullable Long getDeploymentConnectionId() {
    return deploymentConnectionId;
  }

  @JsonProperty("deploymentConnectionId")
  public void setDeploymentConnectionId(@Nullable Long deploymentConnectionId) {
    this.deploymentConnectionId = deploymentConnectionId;
  }

  public AppInstanceModel deploymentSettings(Map<String, Object> deploymentSettings) {
    this.deploymentSettings = deploymentSettings;
    return this;
  }

  public AppInstanceModel putDeploymentSettingsItem(String key, Object deploymentSettingsItem) {
    if (this.deploymentSettings == null) {
      this.deploymentSettings = new HashMap<>();
    }
    this.deploymentSettings.put(key, deploymentSettingsItem);
    return this;
  }

  /**
   * The settings the deployment provider of the hosting target uses. They are visible to everyone who can view the instance and hold no secrets.
   * @return deploymentSettings
   */
  
  @Schema(name = "deploymentSettings", description = "The settings the deployment provider of the hosting target uses. They are visible to everyone who can view the instance and hold no secrets.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("deploymentSettings")
  public Map<String, Object> getDeploymentSettings() {
    return deploymentSettings;
  }

  @JsonProperty("deploymentSettings")
  public void setDeploymentSettings(Map<String, Object> deploymentSettings) {
    this.deploymentSettings = deploymentSettings;
  }

  public AppInstanceModel createdBy(@Nullable String createdBy) {
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

  public AppInstanceModel createdDate(@Nullable OffsetDateTime createdDate) {
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

  public AppInstanceModel lastModifiedBy(@Nullable String lastModifiedBy) {
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

  public AppInstanceModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
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

  public AppInstanceModel version(@Nullable Integer version) {
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
    AppInstanceModel appInstance = (AppInstanceModel) o;
    return Objects.equals(this.id, appInstance.id) &&
        Objects.equals(this.uuid, appInstance.uuid) &&
        Objects.equals(this.appId, appInstance.appId) &&
        Objects.equals(this.appName, appInstance.appName) &&
        Objects.equals(this.appHasIcon, appInstance.appHasIcon) &&
        Objects.equals(this.appStatus, appInstance.appStatus) &&
        Objects.equals(this.workspaceId, appInstance.workspaceId) &&
        Objects.equals(this.environment, appInstance.environment) &&
        Objects.equals(this.name, appInstance.name) &&
        Objects.equals(this.ownerUserId, appInstance.ownerUserId) &&
        Objects.equals(this.status, appInstance.status) &&
        Objects.equals(this.visibility, appInstance.visibility) &&
        Objects.equals(this.hostingTarget, appInstance.hostingTarget) &&
        Objects.equals(this.url, appInstance.url) &&
        Objects.equals(this.secretKeyHint, appInstance.secretKeyHint) &&
        Objects.equals(this.secretKeyLastUsedDate, appInstance.secretKeyLastUsedDate) &&
        Objects.equals(this.userAuthMode, appInstance.userAuthMode) &&
        Objects.equals(this._configuration, appInstance._configuration) &&
        Objects.equals(this.currentAppDeploymentId, appInstance.currentAppDeploymentId) &&
        Objects.equals(this.deploymentConnectionId, appInstance.deploymentConnectionId) &&
        Objects.equals(this.deploymentSettings, appInstance.deploymentSettings) &&
        Objects.equals(this.createdBy, appInstance.createdBy) &&
        Objects.equals(this.createdDate, appInstance.createdDate) &&
        Objects.equals(this.lastModifiedBy, appInstance.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, appInstance.lastModifiedDate) &&
        Objects.equals(this.version, appInstance.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, uuid, appId, appName, appHasIcon, appStatus, workspaceId, environment, name, ownerUserId, status, visibility, hostingTarget, url, secretKeyHint, secretKeyLastUsedDate, userAuthMode, _configuration, currentAppDeploymentId, deploymentConnectionId, deploymentSettings, createdBy, createdDate, lastModifiedBy, lastModifiedDate, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppInstanceModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
    sb.append("    appId: ").append(toIndentedString(appId)).append("\n");
    sb.append("    appName: ").append(toIndentedString(appName)).append("\n");
    sb.append("    appHasIcon: ").append(toIndentedString(appHasIcon)).append("\n");
    sb.append("    appStatus: ").append(toIndentedString(appStatus)).append("\n");
    sb.append("    workspaceId: ").append(toIndentedString(workspaceId)).append("\n");
    sb.append("    environment: ").append(toIndentedString(environment)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    ownerUserId: ").append(toIndentedString(ownerUserId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    visibility: ").append(toIndentedString(visibility)).append("\n");
    sb.append("    hostingTarget: ").append(toIndentedString(hostingTarget)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    secretKeyHint: ").append(toIndentedString(secretKeyHint)).append("\n");
    sb.append("    secretKeyLastUsedDate: ").append(toIndentedString(secretKeyLastUsedDate)).append("\n");
    sb.append("    userAuthMode: ").append(toIndentedString(userAuthMode)).append("\n");
    sb.append("    _configuration: ").append(toIndentedString(_configuration)).append("\n");
    sb.append("    currentAppDeploymentId: ").append(toIndentedString(currentAppDeploymentId)).append("\n");
    sb.append("    deploymentConnectionId: ").append(toIndentedString(deploymentConnectionId)).append("\n");
    sb.append("    deploymentSettings: ").append(toIndentedString(deploymentSettings)).append("\n");
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

