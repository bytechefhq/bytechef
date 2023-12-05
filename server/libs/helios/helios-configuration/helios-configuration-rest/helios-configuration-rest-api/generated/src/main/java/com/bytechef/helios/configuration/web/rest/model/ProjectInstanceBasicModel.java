package com.bytechef.helios.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.LocalDateTime;
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

@Schema(name = "ProjectInstanceBasic", description = "Contains configurations and connections required for the execution of project workflows.")
@JsonTypeName("ProjectInstanceBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-12-04T15:12:04.933898+01:00[Europe/Zagreb]")
public class ProjectInstanceBasicModel {

  private String description;

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  private Long id;

  private String lastExecutionDate;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  private String name;

  private Long projectId;

  private Boolean enabled;

  public ProjectInstanceBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProjectInstanceBasicModel(String name) {
    this.name = name;
  }

  public ProjectInstanceBasicModel description(String description) {
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

  public ProjectInstanceBasicModel createdBy(String createdBy) {
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

  public ProjectInstanceBasicModel createdDate(LocalDateTime createdDate) {
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

  public ProjectInstanceBasicModel id(Long id) {
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

  public ProjectInstanceBasicModel lastExecutionDate(String lastExecutionDate) {
    this.lastExecutionDate = lastExecutionDate;
    return this;
  }

  /**
   * The last execution date.
   * @return lastExecutionDate
  */
  
  @Schema(name = "lastExecutionDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The last execution date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastExecutionDate")
  public String getLastExecutionDate() {
    return lastExecutionDate;
  }

  public void setLastExecutionDate(String lastExecutionDate) {
    this.lastExecutionDate = lastExecutionDate;
  }

  public ProjectInstanceBasicModel lastModifiedBy(String lastModifiedBy) {
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

  public ProjectInstanceBasicModel lastModifiedDate(LocalDateTime lastModifiedDate) {
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

  public ProjectInstanceBasicModel name(String name) {
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

  public ProjectInstanceBasicModel projectId(Long projectId) {
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

  public ProjectInstanceBasicModel enabled(Boolean enabled) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectInstanceBasicModel projectInstanceBasic = (ProjectInstanceBasicModel) o;
    return Objects.equals(this.description, projectInstanceBasic.description) &&
        Objects.equals(this.createdBy, projectInstanceBasic.createdBy) &&
        Objects.equals(this.createdDate, projectInstanceBasic.createdDate) &&
        Objects.equals(this.id, projectInstanceBasic.id) &&
        Objects.equals(this.lastExecutionDate, projectInstanceBasic.lastExecutionDate) &&
        Objects.equals(this.lastModifiedBy, projectInstanceBasic.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, projectInstanceBasic.lastModifiedDate) &&
        Objects.equals(this.name, projectInstanceBasic.name) &&
        Objects.equals(this.projectId, projectInstanceBasic.projectId) &&
        Objects.equals(this.enabled, projectInstanceBasic.enabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, createdBy, createdDate, id, lastExecutionDate, lastModifiedBy, lastModifiedDate, name, projectId, enabled);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectInstanceBasicModel {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastExecutionDate: ").append(toIndentedString(lastExecutionDate)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
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

