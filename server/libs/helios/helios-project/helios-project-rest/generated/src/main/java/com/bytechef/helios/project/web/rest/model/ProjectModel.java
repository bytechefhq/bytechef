package com.bytechef.helios.project.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-27T18:32:18.852699+02:00[Europe/Zagreb]")
public class ProjectModel {

  @JsonProperty("category")
  private com.bytechef.category.web.rest.model.CategoryModel category;

  @JsonProperty("createdBy")
  private String createdBy;

  @JsonProperty("createdDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  @JsonProperty("description")
  private String description;

  @JsonProperty("id")
  private Long id;

  @JsonProperty("lastModifiedBy")
  private String lastModifiedBy;

  @JsonProperty("lastModifiedDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  @JsonProperty("name")
  private String name;

  @JsonProperty("publishedDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime publishedDate;

  @JsonProperty("projectVersion")
  private Integer projectVersion;

  /**
   * The status of a project.
   */
  public enum StatusEnum {
    PUBLISHED("PUBLISHED"),
    
    UNPUBLISHED("UNPUBLISHED");

    private String value;

    StatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("status")
  private StatusEnum status;

  @JsonProperty("tags")
  @Valid
  private List<com.bytechef.tag.web.rest.model.TagModel> tags = null;

  @JsonProperty("workflowIds")
  @Valid
  private List<String> workflowIds = null;

  @JsonProperty("__version")
  private Integer version;

  public ProjectModel category(com.bytechef.category.web.rest.model.CategoryModel category) {
    this.category = category;
    return this;
  }

  /**
   * Get category
   * @return category
  */
  @Valid 
  @Schema(name = "category", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public com.bytechef.category.web.rest.model.CategoryModel getCategory() {
    return category;
  }

  public void setCategory(com.bytechef.category.web.rest.model.CategoryModel category) {
    this.category = category;
  }

  public ProjectModel createdBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * The created by.
   * @return createdBy
  */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The created by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public ProjectModel createdDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  /**
   * The created date.
   * @return createdDate
  */
  @Valid 
  @Schema(name = "createdDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The created date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public ProjectModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of a project.
   * @return description
  */
  
  @Schema(name = "description", description = "The description of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ProjectModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a project.
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ProjectModel lastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  /**
   * The last modified by.
   * @return lastModifiedBy
  */
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public ProjectModel lastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
    return this;
  }

  /**
   * The last modified date.
   * @return lastModifiedDate
  */
  @Valid 
  @Schema(name = "lastModifiedDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public LocalDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
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
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ProjectModel publishedDate(LocalDateTime publishedDate) {
    this.publishedDate = publishedDate;
    return this;
  }

  /**
   * The published date.
   * @return publishedDate
  */
  @Valid 
  @Schema(name = "publishedDate", description = "The published date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public LocalDateTime getPublishedDate() {
    return publishedDate;
  }

  public void setPublishedDate(LocalDateTime publishedDate) {
    this.publishedDate = publishedDate;
  }

  public ProjectModel projectVersion(Integer projectVersion) {
    this.projectVersion = projectVersion;
    return this;
  }

  /**
   * The version of a project.
   * @return projectVersion
  */
  
  @Schema(name = "projectVersion", description = "The version of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Integer getProjectVersion() {
    return projectVersion;
  }

  public void setProjectVersion(Integer projectVersion) {
    this.projectVersion = projectVersion;
  }

  public ProjectModel status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * The status of a project.
   * @return status
  */
  
  @Schema(name = "status", description = "The status of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public StatusEnum getStatus() {
    return status;
  }

  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  public ProjectModel tags(List<com.bytechef.tag.web.rest.model.TagModel> tags) {
    this.tags = tags;
    return this;
  }

  public ProjectModel addTagsItem(com.bytechef.tag.web.rest.model.TagModel tagsItem) {
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
  public List<com.bytechef.tag.web.rest.model.TagModel> getTags() {
    return tags;
  }

  public void setTags(List<com.bytechef.tag.web.rest.model.TagModel> tags) {
    this.tags = tags;
  }

  public ProjectModel workflowIds(List<String> workflowIds) {
    this.workflowIds = workflowIds;
    return this;
  }

  public ProjectModel addWorkflowIdsItem(String workflowIdsItem) {
    if (this.workflowIds == null) {
      this.workflowIds = new ArrayList<>();
    }
    this.workflowIds.add(workflowIdsItem);
    return this;
  }

  /**
   * The workflow ids belonging to this project.
   * @return workflowIds
  */
  
  @Schema(name = "workflowIds", description = "The workflow ids belonging to this project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public List<String> getWorkflowIds() {
    return workflowIds;
  }

  public void setWorkflowIds(List<String> workflowIds) {
    this.workflowIds = workflowIds;
  }

  public ProjectModel version(Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * @return version
  */
  
  @Schema(name = "__version", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    ProjectModel project = (ProjectModel) o;
    return Objects.equals(this.category, project.category) &&
        Objects.equals(this.createdBy, project.createdBy) &&
        Objects.equals(this.createdDate, project.createdDate) &&
        Objects.equals(this.description, project.description) &&
        Objects.equals(this.id, project.id) &&
        Objects.equals(this.lastModifiedBy, project.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, project.lastModifiedDate) &&
        Objects.equals(this.name, project.name) &&
        Objects.equals(this.publishedDate, project.publishedDate) &&
        Objects.equals(this.projectVersion, project.projectVersion) &&
        Objects.equals(this.status, project.status) &&
        Objects.equals(this.tags, project.tags) &&
        Objects.equals(this.workflowIds, project.workflowIds) &&
        Objects.equals(this.version, project.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(category, createdBy, createdDate, description, id, lastModifiedBy, lastModifiedDate, name, publishedDate, projectVersion, status, tags, workflowIds, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectModel {\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    publishedDate: ").append(toIndentedString(publishedDate)).append("\n");
    sb.append("    projectVersion: ").append(toIndentedString(projectVersion)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
    sb.append("    workflowIds: ").append(toIndentedString(workflowIds)).append("\n");
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

