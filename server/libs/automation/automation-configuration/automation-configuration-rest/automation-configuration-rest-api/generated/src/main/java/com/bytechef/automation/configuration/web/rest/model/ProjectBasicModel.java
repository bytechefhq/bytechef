package com.bytechef.automation.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.automation.configuration.web.rest.model.ProjectStatusModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * A group of workflows that make one logical project.
 */

@Schema(name = "ProjectBasic", description = "A group of workflows that make one logical project.")
@JsonTypeName("ProjectBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-11-26T21:19:57.797159+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public class ProjectBasicModel {

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  private String description;

  private Long id;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  private String name;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastPublishedDate;

  private ProjectStatusModel lastStatus;

  private Integer lastProjectVersion;

  public ProjectBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProjectBasicModel(String name) {
    this.name = name;
  }

  public ProjectBasicModel createdBy(String createdBy) {
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

  public ProjectBasicModel createdDate(LocalDateTime createdDate) {
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

  public ProjectBasicModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of a project.
   * @return description
   */
  
  @Schema(name = "description", description = "The description of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ProjectBasicModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a project.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ProjectBasicModel lastModifiedBy(String lastModifiedBy) {
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

  public ProjectBasicModel lastModifiedDate(LocalDateTime lastModifiedDate) {
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

  public ProjectBasicModel name(String name) {
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

  public ProjectBasicModel lastPublishedDate(LocalDateTime lastPublishedDate) {
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
  public LocalDateTime getLastPublishedDate() {
    return lastPublishedDate;
  }

  public void setLastPublishedDate(LocalDateTime lastPublishedDate) {
    this.lastPublishedDate = lastPublishedDate;
  }

  public ProjectBasicModel lastStatus(ProjectStatusModel lastStatus) {
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
  public ProjectStatusModel getLastStatus() {
    return lastStatus;
  }

  public void setLastStatus(ProjectStatusModel lastStatus) {
    this.lastStatus = lastStatus;
  }

  public ProjectBasicModel lastProjectVersion(Integer lastProjectVersion) {
    this.lastProjectVersion = lastProjectVersion;
    return this;
  }

  /**
   * The last version of a project.
   * @return lastProjectVersion
   */
  
  @Schema(name = "lastProjectVersion", accessMode = Schema.AccessMode.READ_ONLY, description = "The last version of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastProjectVersion")
  public Integer getLastProjectVersion() {
    return lastProjectVersion;
  }

  public void setLastProjectVersion(Integer lastProjectVersion) {
    this.lastProjectVersion = lastProjectVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectBasicModel projectBasic = (ProjectBasicModel) o;
    return Objects.equals(this.createdBy, projectBasic.createdBy) &&
        Objects.equals(this.createdDate, projectBasic.createdDate) &&
        Objects.equals(this.description, projectBasic.description) &&
        Objects.equals(this.id, projectBasic.id) &&
        Objects.equals(this.lastModifiedBy, projectBasic.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, projectBasic.lastModifiedDate) &&
        Objects.equals(this.name, projectBasic.name) &&
        Objects.equals(this.lastPublishedDate, projectBasic.lastPublishedDate) &&
        Objects.equals(this.lastStatus, projectBasic.lastStatus) &&
        Objects.equals(this.lastProjectVersion, projectBasic.lastProjectVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, description, id, lastModifiedBy, lastModifiedDate, name, lastPublishedDate, lastStatus, lastProjectVersion);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectBasicModel {\n");
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

