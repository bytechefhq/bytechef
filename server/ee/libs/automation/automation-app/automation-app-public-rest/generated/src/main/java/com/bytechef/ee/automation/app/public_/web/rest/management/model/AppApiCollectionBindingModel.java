package com.bytechef.ee.automation.app.public_.web.rest.management.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * An API collection bound to an app.
 */

@Schema(name = "AppApiCollectionBinding", description = "An API collection bound to an app.")
@JsonTypeName("AppApiCollectionBinding")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:30.252991+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class AppApiCollectionBindingModel {

  private @Nullable Long id;

  private @Nullable Long appId;

  private @Nullable Long apiCollectionId;

  private @Nullable String name;

  private @Nullable String contextPath;

  private @Nullable Integer collectionVersion;

  private @Nullable Long projectId;

  private @Nullable String projectName;

  private @Nullable Boolean available;

  public AppApiCollectionBindingModel id(@Nullable Long id) {
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

  public AppApiCollectionBindingModel appId(@Nullable Long appId) {
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

  public AppApiCollectionBindingModel apiCollectionId(@Nullable Long apiCollectionId) {
    this.apiCollectionId = apiCollectionId;
    return this;
  }

  /**
   * Get apiCollectionId
   * @return apiCollectionId
   */
  
  @Schema(name = "apiCollectionId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("apiCollectionId")
  public @Nullable Long getApiCollectionId() {
    return apiCollectionId;
  }

  @JsonProperty("apiCollectionId")
  public void setApiCollectionId(@Nullable Long apiCollectionId) {
    this.apiCollectionId = apiCollectionId;
  }

  public AppApiCollectionBindingModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The collection's name, or #apiCollectionId when the collection no longer exists.
   * @return name
   */
  
  @Schema(name = "name", description = "The collection's name, or #apiCollectionId when the collection no longer exists.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public AppApiCollectionBindingModel contextPath(@Nullable String contextPath) {
    this.contextPath = contextPath;
    return this;
  }

  /**
   * Get contextPath
   * @return contextPath
   */
  
  @Schema(name = "contextPath", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("contextPath")
  public @Nullable String getContextPath() {
    return contextPath;
  }

  @JsonProperty("contextPath")
  public void setContextPath(@Nullable String contextPath) {
    this.contextPath = contextPath;
  }

  public AppApiCollectionBindingModel collectionVersion(@Nullable Integer collectionVersion) {
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

  public AppApiCollectionBindingModel projectId(@Nullable Long projectId) {
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

  public AppApiCollectionBindingModel projectName(@Nullable String projectName) {
    this.projectName = projectName;
    return this;
  }

  /**
   * Get projectName
   * @return projectName
   */
  
  @Schema(name = "projectName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectName")
  public @Nullable String getProjectName() {
    return projectName;
  }

  @JsonProperty("projectName")
  public void setProjectName(@Nullable String projectName) {
    this.projectName = projectName;
  }

  public AppApiCollectionBindingModel available(@Nullable Boolean available) {
    this.available = available;
    return this;
  }

  /**
   * Whether the collection's deployment exists, is enabled and is in the app's workspace and environment.
   * @return available
   */
  
  @Schema(name = "available", description = "Whether the collection's deployment exists, is enabled and is in the app's workspace and environment.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("available")
  public @Nullable Boolean getAvailable() {
    return available;
  }

  @JsonProperty("available")
  public void setAvailable(@Nullable Boolean available) {
    this.available = available;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppApiCollectionBindingModel appApiCollectionBinding = (AppApiCollectionBindingModel) o;
    return Objects.equals(this.id, appApiCollectionBinding.id) &&
        Objects.equals(this.appId, appApiCollectionBinding.appId) &&
        Objects.equals(this.apiCollectionId, appApiCollectionBinding.apiCollectionId) &&
        Objects.equals(this.name, appApiCollectionBinding.name) &&
        Objects.equals(this.contextPath, appApiCollectionBinding.contextPath) &&
        Objects.equals(this.collectionVersion, appApiCollectionBinding.collectionVersion) &&
        Objects.equals(this.projectId, appApiCollectionBinding.projectId) &&
        Objects.equals(this.projectName, appApiCollectionBinding.projectName) &&
        Objects.equals(this.available, appApiCollectionBinding.available);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, appId, apiCollectionId, name, contextPath, collectionVersion, projectId, projectName, available);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppApiCollectionBindingModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    appId: ").append(toIndentedString(appId)).append("\n");
    sb.append("    apiCollectionId: ").append(toIndentedString(apiCollectionId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    contextPath: ").append(toIndentedString(contextPath)).append("\n");
    sb.append("    collectionVersion: ").append(toIndentedString(collectionVersion)).append("\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    projectName: ").append(toIndentedString(projectName)).append("\n");
    sb.append("    available: ").append(toIndentedString(available)).append("\n");
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

