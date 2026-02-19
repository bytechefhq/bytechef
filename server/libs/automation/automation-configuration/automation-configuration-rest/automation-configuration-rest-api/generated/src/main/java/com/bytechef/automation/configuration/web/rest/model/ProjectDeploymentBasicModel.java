package com.bytechef.automation.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
 * Contains configurations and connections required for the execution of project workflows.
 */

@Schema(name = "ProjectDeploymentBasic", description = "Contains configurations and connections required for the execution of project workflows.")
@JsonTypeName("ProjectDeploymentBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-05T14:11:26.033736+01:00[Europe/Ljubljana]", comments = "Generator version: 7.18.0")
public class ProjectDeploymentBasicModel {

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private @Nullable String description;

  private @Nullable Boolean enabled;

  private @Nullable Long environmentId;

  private @Nullable Long id;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastExecutionDate;

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  private String name;

  private @Nullable Long projectId;

  private @Nullable Integer projectVersion;

  public ProjectDeploymentBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProjectDeploymentBasicModel(String name) {
    this.name = name;
  }

  public ProjectDeploymentBasicModel createdBy(@Nullable String createdBy) {
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

  public ProjectDeploymentBasicModel createdDate(@Nullable OffsetDateTime createdDate) {
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

  public ProjectDeploymentBasicModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of a project deployment.
   * @return description
   */
  
  @Schema(name = "description", description = "The description of a project deployment.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public ProjectDeploymentBasicModel enabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If a project deployment is enabled or not.
   * @return enabled
   */
  
  @Schema(name = "enabled", description = "If a project deployment is enabled or not.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public @Nullable Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
  }

  public ProjectDeploymentBasicModel environmentId(@Nullable Long environmentId) {
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

  public ProjectDeploymentBasicModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a project deployment.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a project deployment.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public ProjectDeploymentBasicModel lastExecutionDate(@Nullable OffsetDateTime lastExecutionDate) {
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
  public @Nullable OffsetDateTime getLastExecutionDate() {
    return lastExecutionDate;
  }

  public void setLastExecutionDate(@Nullable OffsetDateTime lastExecutionDate) {
    this.lastExecutionDate = lastExecutionDate;
  }

  public ProjectDeploymentBasicModel lastModifiedBy(@Nullable String lastModifiedBy) {
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

  public ProjectDeploymentBasicModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
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

  public ProjectDeploymentBasicModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of a project deployment.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The name of a project deployment.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ProjectDeploymentBasicModel projectId(@Nullable Long projectId) {
    this.projectId = projectId;
    return this;
  }

  /**
   * The id of a project.
   * @return projectId
   */
  
  @Schema(name = "projectId", description = "The id of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectId")
  public @Nullable Long getProjectId() {
    return projectId;
  }

  public void setProjectId(@Nullable Long projectId) {
    this.projectId = projectId;
  }

  public ProjectDeploymentBasicModel projectVersion(@Nullable Integer projectVersion) {
    this.projectVersion = projectVersion;
    return this;
  }

  /**
   * The version of a project.
   * @return projectVersion
   */
  
  @Schema(name = "projectVersion", description = "The version of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectVersion")
  public @Nullable Integer getProjectVersion() {
    return projectVersion;
  }

  public void setProjectVersion(@Nullable Integer projectVersion) {
    this.projectVersion = projectVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectDeploymentBasicModel projectDeploymentBasic = (ProjectDeploymentBasicModel) o;
    return Objects.equals(this.createdBy, projectDeploymentBasic.createdBy) &&
        Objects.equals(this.createdDate, projectDeploymentBasic.createdDate) &&
        Objects.equals(this.description, projectDeploymentBasic.description) &&
        Objects.equals(this.enabled, projectDeploymentBasic.enabled) &&
        Objects.equals(this.environmentId, projectDeploymentBasic.environmentId) &&
        Objects.equals(this.id, projectDeploymentBasic.id) &&
        Objects.equals(this.lastExecutionDate, projectDeploymentBasic.lastExecutionDate) &&
        Objects.equals(this.lastModifiedBy, projectDeploymentBasic.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, projectDeploymentBasic.lastModifiedDate) &&
        Objects.equals(this.name, projectDeploymentBasic.name) &&
        Objects.equals(this.projectId, projectDeploymentBasic.projectId) &&
        Objects.equals(this.projectVersion, projectDeploymentBasic.projectVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, description, enabled, environmentId, id, lastExecutionDate, lastModifiedBy, lastModifiedDate, name, projectId, projectVersion);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectDeploymentBasicModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    environmentId: ").append(toIndentedString(environmentId)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastExecutionDate: ").append(toIndentedString(lastExecutionDate)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    projectVersion: ").append(toIndentedString(projectVersion)).append("\n");
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

