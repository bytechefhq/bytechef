package com.bytechef.ee.automation.configuration.web.rest.model;

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
 * The git configuration.
 */

@Schema(name = "ProjectGitConfiguration", description = "The git configuration.")
@JsonTypeName("ProjectGitConfiguration")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:31.339708+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class ProjectGitConfigurationModel {

  private Long projectId;

  private String branch;

  private Boolean enabled;

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  private @Nullable Integer version;

  public ProjectGitConfigurationModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProjectGitConfigurationModel(Long projectId, String branch, Boolean enabled) {
    this.projectId = projectId;
    this.branch = branch;
    this.enabled = enabled;
  }

  public ProjectGitConfigurationModel projectId(Long projectId) {
    this.projectId = projectId;
    return this;
  }

  /**
   * The project id of a project git configuration.
   * @return projectId
   */
  
  @Schema(name = "projectId", accessMode = Schema.AccessMode.READ_ONLY, description = "The project id of a project git configuration.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("projectId")
  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public ProjectGitConfigurationModel branch(String branch) {
    this.branch = branch;
    return this;
  }

  /**
   * The branch.
   * @return branch
   */
  @NotNull 
  @Schema(name = "branch", description = "The branch.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("branch")
  public String getBranch() {
    return branch;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }

  public ProjectGitConfigurationModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * The enabled.
   * @return enabled
   */
  @NotNull 
  @Schema(name = "enabled", description = "The enabled.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public ProjectGitConfigurationModel createdBy(@Nullable String createdBy) {
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

  public ProjectGitConfigurationModel createdDate(@Nullable OffsetDateTime createdDate) {
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

  public ProjectGitConfigurationModel lastModifiedBy(@Nullable String lastModifiedBy) {
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

  public ProjectGitConfigurationModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
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

  public ProjectGitConfigurationModel version(@Nullable Integer version) {
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
    ProjectGitConfigurationModel projectGitConfiguration = (ProjectGitConfigurationModel) o;
    return Objects.equals(this.projectId, projectGitConfiguration.projectId) &&
        Objects.equals(this.branch, projectGitConfiguration.branch) &&
        Objects.equals(this.enabled, projectGitConfiguration.enabled) &&
        Objects.equals(this.createdBy, projectGitConfiguration.createdBy) &&
        Objects.equals(this.createdDate, projectGitConfiguration.createdDate) &&
        Objects.equals(this.lastModifiedBy, projectGitConfiguration.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, projectGitConfiguration.lastModifiedDate) &&
        Objects.equals(this.version, projectGitConfiguration.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId, branch, enabled, createdBy, createdDate, lastModifiedBy, lastModifiedDate, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectGitConfigurationModel {\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    branch: ").append(toIndentedString(branch)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
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
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

