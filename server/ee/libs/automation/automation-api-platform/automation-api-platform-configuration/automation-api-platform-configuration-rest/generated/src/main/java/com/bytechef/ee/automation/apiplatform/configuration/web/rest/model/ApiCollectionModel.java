package com.bytechef.ee.automation.apiplatform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.apiplatform.configuration.web.rest.model.ApiCollectionEndpointModel;
import com.bytechef.ee.automation.apiplatform.configuration.web.rest.model.TagModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-07T14:49:48.731112+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public class ApiCollectionModel {

  private @Nullable Integer collectionVersion;

  private String contextPath;

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private @Nullable String description;

  private Boolean enabled = false;

  @Valid
  private List<@Valid ApiCollectionEndpointModel> endpoints = new ArrayList<>();

  private @Nullable Long environmentId;

  private @Nullable Long id;

  private String name;

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  private Long projectId;

  private @Nullable com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel project;

  private @Nullable Long projectDeploymentId;

  private @Nullable com.bytechef.automation.configuration.web.rest.model.ProjectDeploymentBasicModel projectDeployment;

  private Integer projectVersion;

  @Valid
  private List<@Valid TagModel> tags = new ArrayList<>();

  private Long workspaceId;

  private @Nullable Integer version;

  public ApiCollectionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ApiCollectionModel(String contextPath, Boolean enabled, String name, Long projectId, Integer projectVersion, Long workspaceId) {
    this.contextPath = contextPath;
    this.enabled = enabled;
    this.name = name;
    this.projectId = projectId;
    this.projectVersion = projectVersion;
    this.workspaceId = workspaceId;
  }

  public ApiCollectionModel collectionVersion(@Nullable Integer collectionVersion) {
    this.collectionVersion = collectionVersion;
    return this;
  }

  /**
   * The version of an API collection.
   * @return collectionVersion
   */
  
  @Schema(name = "collectionVersion", description = "The version of an API collection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("collectionVersion")
  public @Nullable Integer getCollectionVersion() {
    return collectionVersion;
  }

  public void setCollectionVersion(@Nullable Integer collectionVersion) {
    this.collectionVersion = collectionVersion;
  }

  public ApiCollectionModel contextPath(String contextPath) {
    this.contextPath = contextPath;
    return this;
  }

  /**
   * The collection context path.
   * @return contextPath
   */
  @NotNull 
  @Schema(name = "contextPath", description = "The collection context path.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("contextPath")
  public String getContextPath() {
    return contextPath;
  }

  public void setContextPath(String contextPath) {
    this.contextPath = contextPath;
  }

  public ApiCollectionModel createdBy(@Nullable String createdBy) {
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

  public ApiCollectionModel createdDate(@Nullable OffsetDateTime createdDate) {
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

  public ApiCollectionModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of an API collection.
   * @return description
   */
  
  @Schema(name = "description", description = "The description of an API collection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public ApiCollectionModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If an API collection is enabled or not.
   * @return enabled
   */
  @NotNull 
  @Schema(name = "enabled", description = "If an API collection is enabled or not.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public ApiCollectionModel endpoints(List<@Valid ApiCollectionEndpointModel> endpoints) {
    this.endpoints = endpoints;
    return this;
  }

  public ApiCollectionModel addEndpointsItem(ApiCollectionEndpointModel endpointsItem) {
    if (this.endpoints == null) {
      this.endpoints = new ArrayList<>();
    }
    this.endpoints.add(endpointsItem);
    return this;
  }

  /**
   * Get endpoints
   * @return endpoints
   */
  @Valid 
  @Schema(name = "endpoints", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("endpoints")
  public List<@Valid ApiCollectionEndpointModel> getEndpoints() {
    return endpoints;
  }

  public void setEndpoints(List<@Valid ApiCollectionEndpointModel> endpoints) {
    this.endpoints = endpoints;
  }

  public ApiCollectionModel environmentId(@Nullable Long environmentId) {
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

  public ApiCollectionModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an API collection.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an API collection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public ApiCollectionModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of an API collection.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The name of an API collection.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiCollectionModel lastModifiedBy(@Nullable String lastModifiedBy) {
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

  public ApiCollectionModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
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

  public ApiCollectionModel projectId(Long projectId) {
    this.projectId = projectId;
    return this;
  }

  /**
   * The id of a project the API collection is connected to.
   * @return projectId
   */
  @NotNull 
  @Schema(name = "projectId", description = "The id of a project the API collection is connected to.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("projectId")
  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public ApiCollectionModel project(@Nullable com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel project) {
    this.project = project;
    return this;
  }

  /**
   * Get project
   * @return project
   */
  @Valid 
  @Schema(name = "project", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("project")
  public @Nullable com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel getProject() {
    return project;
  }

  public void setProject(@Nullable com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel project) {
    this.project = project;
  }

  public ApiCollectionModel projectDeploymentId(@Nullable Long projectDeploymentId) {
    this.projectDeploymentId = projectDeploymentId;
    return this;
  }

  /**
   * The id of an project deployment the API collection is connected to.
   * @return projectDeploymentId
   */
  
  @Schema(name = "projectDeploymentId", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an project deployment the API collection is connected to.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectDeploymentId")
  public @Nullable Long getProjectDeploymentId() {
    return projectDeploymentId;
  }

  public void setProjectDeploymentId(@Nullable Long projectDeploymentId) {
    this.projectDeploymentId = projectDeploymentId;
  }

  public ApiCollectionModel projectDeployment(@Nullable com.bytechef.automation.configuration.web.rest.model.ProjectDeploymentBasicModel projectDeployment) {
    this.projectDeployment = projectDeployment;
    return this;
  }

  /**
   * Get projectDeployment
   * @return projectDeployment
   */
  @Valid 
  @Schema(name = "projectDeployment", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectDeployment")
  public @Nullable com.bytechef.automation.configuration.web.rest.model.ProjectDeploymentBasicModel getProjectDeployment() {
    return projectDeployment;
  }

  public void setProjectDeployment(@Nullable com.bytechef.automation.configuration.web.rest.model.ProjectDeploymentBasicModel projectDeployment) {
    this.projectDeployment = projectDeployment;
  }

  public ApiCollectionModel projectVersion(Integer projectVersion) {
    this.projectVersion = projectVersion;
    return this;
  }

  /**
   * The version of a project the API collection is connected to.
   * @return projectVersion
   */
  @NotNull 
  @Schema(name = "projectVersion", description = "The version of a project the API collection is connected to.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("projectVersion")
  public Integer getProjectVersion() {
    return projectVersion;
  }

  public void setProjectVersion(Integer projectVersion) {
    this.projectVersion = projectVersion;
  }

  public ApiCollectionModel tags(List<@Valid TagModel> tags) {
    this.tags = tags;
    return this;
  }

  public ApiCollectionModel addTagsItem(TagModel tagsItem) {
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

  public ApiCollectionModel workspaceId(Long workspaceId) {
    this.workspaceId = workspaceId;
    return this;
  }

  /**
   * The workspace id.
   * @return workspaceId
   */
  @NotNull 
  @Schema(name = "workspaceId", description = "The workspace id.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workspaceId")
  public Long getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(Long workspaceId) {
    this.workspaceId = workspaceId;
  }

  public ApiCollectionModel version(@Nullable Integer version) {
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
    ApiCollectionModel apiCollection = (ApiCollectionModel) o;
    return Objects.equals(this.collectionVersion, apiCollection.collectionVersion) &&
        Objects.equals(this.contextPath, apiCollection.contextPath) &&
        Objects.equals(this.createdBy, apiCollection.createdBy) &&
        Objects.equals(this.createdDate, apiCollection.createdDate) &&
        Objects.equals(this.description, apiCollection.description) &&
        Objects.equals(this.enabled, apiCollection.enabled) &&
        Objects.equals(this.endpoints, apiCollection.endpoints) &&
        Objects.equals(this.environmentId, apiCollection.environmentId) &&
        Objects.equals(this.id, apiCollection.id) &&
        Objects.equals(this.name, apiCollection.name) &&
        Objects.equals(this.lastModifiedBy, apiCollection.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, apiCollection.lastModifiedDate) &&
        Objects.equals(this.projectId, apiCollection.projectId) &&
        Objects.equals(this.project, apiCollection.project) &&
        Objects.equals(this.projectDeploymentId, apiCollection.projectDeploymentId) &&
        Objects.equals(this.projectDeployment, apiCollection.projectDeployment) &&
        Objects.equals(this.projectVersion, apiCollection.projectVersion) &&
        Objects.equals(this.tags, apiCollection.tags) &&
        Objects.equals(this.workspaceId, apiCollection.workspaceId) &&
        Objects.equals(this.version, apiCollection.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(collectionVersion, contextPath, createdBy, createdDate, description, enabled, endpoints, environmentId, id, name, lastModifiedBy, lastModifiedDate, projectId, project, projectDeploymentId, projectDeployment, projectVersion, tags, workspaceId, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCollectionModel {\n");
    sb.append("    collectionVersion: ").append(toIndentedString(collectionVersion)).append("\n");
    sb.append("    contextPath: ").append(toIndentedString(contextPath)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    endpoints: ").append(toIndentedString(endpoints)).append("\n");
    sb.append("    environmentId: ").append(toIndentedString(environmentId)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    project: ").append(toIndentedString(project)).append("\n");
    sb.append("    projectDeploymentId: ").append(toIndentedString(projectDeploymentId)).append("\n");
    sb.append("    projectDeployment: ").append(toIndentedString(projectDeployment)).append("\n");
    sb.append("    projectVersion: ").append(toIndentedString(projectVersion)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
    sb.append("    workspaceId: ").append(toIndentedString(workspaceId)).append("\n");
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

