package com.bytechef.automation.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.automation.configuration.web.rest.model.CategoryModel;
import com.bytechef.automation.configuration.web.rest.model.ProjectStatusModel;
import com.bytechef.automation.configuration.web.rest.model.TagModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * A group of workflows that make one logical project.
 */

@Schema(name = "Project", description = "A group of workflows that make one logical project.")
@JsonTypeName("Project")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-07T14:49:45.643121+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public class ProjectModel {

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private @Nullable String description;

  private @Nullable Long id;

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  private String name;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastPublishedDate;

  private @Nullable ProjectStatusModel lastStatus;

  private @Nullable Integer lastProjectVersion;

  private @Nullable String uuid;

  private @Nullable CategoryModel category;

  @Valid
  private List<Long> projectWorkflowIds = new ArrayList<>();

  @Valid
  private List<@Valid TagModel> tags = new ArrayList<>();

  private Long workspaceId;

  private @Nullable Integer version;

  public ProjectModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProjectModel(String name, Long workspaceId) {
    this.name = name;
    this.workspaceId = workspaceId;
  }

  public ProjectModel createdBy(@Nullable String createdBy) {
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

  public ProjectModel createdDate(@Nullable OffsetDateTime createdDate) {
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

  public ProjectModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of a project.
   * @return description
   */
  
  @Schema(name = "description", description = "The description of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public ProjectModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a project.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public ProjectModel lastModifiedBy(@Nullable String lastModifiedBy) {
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

  public ProjectModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
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

  public ProjectModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of a project.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The name of a project.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ProjectModel lastPublishedDate(@Nullable OffsetDateTime lastPublishedDate) {
    this.lastPublishedDate = lastPublishedDate;
    return this;
  }

  /**
   * The last published date.
   * @return lastPublishedDate
   */
  @Valid 
  @Schema(name = "lastPublishedDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The last published date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastPublishedDate")
  public @Nullable OffsetDateTime getLastPublishedDate() {
    return lastPublishedDate;
  }

  public void setLastPublishedDate(@Nullable OffsetDateTime lastPublishedDate) {
    this.lastPublishedDate = lastPublishedDate;
  }

  public ProjectModel lastStatus(@Nullable ProjectStatusModel lastStatus) {
    this.lastStatus = lastStatus;
    return this;
  }

  /**
   * Get lastStatus
   * @return lastStatus
   */
  @Valid 
  @Schema(name = "lastStatus", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastStatus")
  public @Nullable ProjectStatusModel getLastStatus() {
    return lastStatus;
  }

  public void setLastStatus(@Nullable ProjectStatusModel lastStatus) {
    this.lastStatus = lastStatus;
  }

  public ProjectModel lastProjectVersion(@Nullable Integer lastProjectVersion) {
    this.lastProjectVersion = lastProjectVersion;
    return this;
  }

  /**
   * The last version of a project.
   * @return lastProjectVersion
   */
  
  @Schema(name = "lastProjectVersion", accessMode = Schema.AccessMode.READ_ONLY, description = "The last version of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastProjectVersion")
  public @Nullable Integer getLastProjectVersion() {
    return lastProjectVersion;
  }

  public void setLastProjectVersion(@Nullable Integer lastProjectVersion) {
    this.lastProjectVersion = lastProjectVersion;
  }

  public ProjectModel uuid(@Nullable String uuid) {
    this.uuid = uuid;
    return this;
  }

  /**
   * The uuid of a project.
   * @return uuid
   */
  
  @Schema(name = "uuid", description = "The uuid of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("uuid")
  public @Nullable String getUuid() {
    return uuid;
  }

  public void setUuid(@Nullable String uuid) {
    this.uuid = uuid;
  }

  public ProjectModel category(@Nullable CategoryModel category) {
    this.category = category;
    return this;
  }

  /**
   * Get category
   * @return category
   */
  @Valid 
  @Schema(name = "category", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("category")
  public @Nullable CategoryModel getCategory() {
    return category;
  }

  public void setCategory(@Nullable CategoryModel category) {
    this.category = category;
  }

  public ProjectModel projectWorkflowIds(List<Long> projectWorkflowIds) {
    this.projectWorkflowIds = projectWorkflowIds;
    return this;
  }

  public ProjectModel addProjectWorkflowIdsItem(Long projectWorkflowIdsItem) {
    if (this.projectWorkflowIds == null) {
      this.projectWorkflowIds = new ArrayList<>();
    }
    this.projectWorkflowIds.add(projectWorkflowIdsItem);
    return this;
  }

  /**
   * The project workflow ids belonging to this project.
   * @return projectWorkflowIds
   */
  
  @Schema(name = "projectWorkflowIds", description = "The project workflow ids belonging to this project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectWorkflowIds")
  public List<Long> getProjectWorkflowIds() {
    return projectWorkflowIds;
  }

  public void setProjectWorkflowIds(List<Long> projectWorkflowIds) {
    this.projectWorkflowIds = projectWorkflowIds;
  }

  public ProjectModel tags(List<@Valid TagModel> tags) {
    this.tags = tags;
    return this;
  }

  public ProjectModel addTagsItem(TagModel tagsItem) {
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

  public ProjectModel workspaceId(Long workspaceId) {
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

  public ProjectModel version(@Nullable Integer version) {
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
    ProjectModel project = (ProjectModel) o;
    return Objects.equals(this.createdBy, project.createdBy) &&
        Objects.equals(this.createdDate, project.createdDate) &&
        Objects.equals(this.description, project.description) &&
        Objects.equals(this.id, project.id) &&
        Objects.equals(this.lastModifiedBy, project.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, project.lastModifiedDate) &&
        Objects.equals(this.name, project.name) &&
        Objects.equals(this.lastPublishedDate, project.lastPublishedDate) &&
        Objects.equals(this.lastStatus, project.lastStatus) &&
        Objects.equals(this.lastProjectVersion, project.lastProjectVersion) &&
        Objects.equals(this.uuid, project.uuid) &&
        Objects.equals(this.category, project.category) &&
        Objects.equals(this.projectWorkflowIds, project.projectWorkflowIds) &&
        Objects.equals(this.tags, project.tags) &&
        Objects.equals(this.workspaceId, project.workspaceId) &&
        Objects.equals(this.version, project.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, description, id, lastModifiedBy, lastModifiedDate, name, lastPublishedDate, lastStatus, lastProjectVersion, uuid, category, projectWorkflowIds, tags, workspaceId, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    lastPublishedDate: ").append(toIndentedString(lastPublishedDate)).append("\n");
    sb.append("    lastStatus: ").append(toIndentedString(lastStatus)).append("\n");
    sb.append("    lastProjectVersion: ").append(toIndentedString(lastProjectVersion)).append("\n");
    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    projectWorkflowIds: ").append(toIndentedString(projectWorkflowIds)).append("\n");
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

