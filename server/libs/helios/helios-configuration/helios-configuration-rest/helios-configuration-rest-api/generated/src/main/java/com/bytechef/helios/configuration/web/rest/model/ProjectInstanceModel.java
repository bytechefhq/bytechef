package com.bytechef.helios.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.helios.configuration.web.rest.model.ProjectInstanceProjectModel;
import com.bytechef.helios.configuration.web.rest.model.ProjectInstanceWorkflowModel;
import com.bytechef.helios.configuration.web.rest.model.TagModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Contains configurations and connections required for the execution of project workflows.
 */

@Schema(name = "ProjectInstance", description = "Contains configurations and connections required for the execution of project workflows.")
@JsonTypeName("ProjectInstance")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-11-21T21:27:13.496994+01:00[Europe/Zagreb]")
public class ProjectInstanceModel {

  private String description;

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  private Long id;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastExecutionDate;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  private String name;

  private ProjectInstanceProjectModel project;

  private Long projectId;

  @Valid
  private List<@Valid ProjectInstanceWorkflowModel> projectInstanceWorkflows;

  private Boolean enabled;

  @Valid
  private List<@Valid TagModel> tags;

  private Integer version;

  public ProjectInstanceModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProjectInstanceModel(String name) {
    this.name = name;
  }

  public ProjectInstanceModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of a project instance.
   * @return description
  */
  
  @Schema(name = "description", description = "The description of a project instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ProjectInstanceModel createdBy(String createdBy) {
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

  public ProjectInstanceModel createdDate(LocalDateTime createdDate) {
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

  public ProjectInstanceModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a project instance.
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a project instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ProjectInstanceModel lastExecutionDate(LocalDateTime lastExecutionDate) {
    this.lastExecutionDate = lastExecutionDate;
    return this;
  }

  /**
   * The last execution date.
   * @return lastExecutionDate
  */
  @Valid 
  @Schema(name = "lastExecutionDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The last execution date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastExecutionDate")
  public LocalDateTime getLastExecutionDate() {
    return lastExecutionDate;
  }

  public void setLastExecutionDate(LocalDateTime lastExecutionDate) {
    this.lastExecutionDate = lastExecutionDate;
  }

  public ProjectInstanceModel lastModifiedBy(String lastModifiedBy) {
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

  public ProjectInstanceModel lastModifiedDate(LocalDateTime lastModifiedDate) {
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

  public ProjectInstanceModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of a project instance.
   * @return name
  */
  @NotNull 
  @Schema(name = "name", description = "The name of a project instance.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ProjectInstanceModel project(ProjectInstanceProjectModel project) {
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
  public ProjectInstanceProjectModel getProject() {
    return project;
  }

  public void setProject(ProjectInstanceProjectModel project) {
    this.project = project;
  }

  public ProjectInstanceModel projectId(Long projectId) {
    this.projectId = projectId;
    return this;
  }

  /**
   * Th id of a project.
   * @return projectId
  */
  
  @Schema(name = "projectId", description = "Th id of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectId")
  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public ProjectInstanceModel projectInstanceWorkflows(List<@Valid ProjectInstanceWorkflowModel> projectInstanceWorkflows) {
    this.projectInstanceWorkflows = projectInstanceWorkflows;
    return this;
  }

  public ProjectInstanceModel addProjectInstanceWorkflowsItem(ProjectInstanceWorkflowModel projectInstanceWorkflowsItem) {
    if (this.projectInstanceWorkflows == null) {
      this.projectInstanceWorkflows = new ArrayList<>();
    }
    this.projectInstanceWorkflows.add(projectInstanceWorkflowsItem);
    return this;
  }

  /**
   * Get projectInstanceWorkflows
   * @return projectInstanceWorkflows
  */
  @Valid 
  @Schema(name = "projectInstanceWorkflows", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectInstanceWorkflows")
  public List<@Valid ProjectInstanceWorkflowModel> getProjectInstanceWorkflows() {
    return projectInstanceWorkflows;
  }

  public void setProjectInstanceWorkflows(List<@Valid ProjectInstanceWorkflowModel> projectInstanceWorkflows) {
    this.projectInstanceWorkflows = projectInstanceWorkflows;
  }

  public ProjectInstanceModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If a workflow is enabled or not in the project instance.
   * @return enabled
  */
  
  @Schema(name = "enabled", description = "If a workflow is enabled or not in the project instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public ProjectInstanceModel tags(List<@Valid TagModel> tags) {
    this.tags = tags;
    return this;
  }

  public ProjectInstanceModel addTagsItem(TagModel tagsItem) {
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

  public ProjectInstanceModel version(Integer version) {
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
    ProjectInstanceModel projectInstance = (ProjectInstanceModel) o;
    return Objects.equals(this.description, projectInstance.description) &&
        Objects.equals(this.createdBy, projectInstance.createdBy) &&
        Objects.equals(this.createdDate, projectInstance.createdDate) &&
        Objects.equals(this.id, projectInstance.id) &&
        Objects.equals(this.lastExecutionDate, projectInstance.lastExecutionDate) &&
        Objects.equals(this.lastModifiedBy, projectInstance.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, projectInstance.lastModifiedDate) &&
        Objects.equals(this.name, projectInstance.name) &&
        Objects.equals(this.project, projectInstance.project) &&
        Objects.equals(this.projectId, projectInstance.projectId) &&
        Objects.equals(this.projectInstanceWorkflows, projectInstance.projectInstanceWorkflows) &&
        Objects.equals(this.enabled, projectInstance.enabled) &&
        Objects.equals(this.tags, projectInstance.tags) &&
        Objects.equals(this.version, projectInstance.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, createdBy, createdDate, id, lastExecutionDate, lastModifiedBy, lastModifiedDate, name, project, projectId, projectInstanceWorkflows, enabled, tags, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectInstanceModel {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastExecutionDate: ").append(toIndentedString(lastExecutionDate)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    project: ").append(toIndentedString(project)).append("\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    projectInstanceWorkflows: ").append(toIndentedString(projectInstanceWorkflows)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
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

