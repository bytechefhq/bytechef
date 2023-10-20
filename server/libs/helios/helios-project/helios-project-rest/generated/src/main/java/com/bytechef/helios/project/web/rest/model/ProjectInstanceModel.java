package com.bytechef.helios.project.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.helios.project.web.rest.model.ProjectModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Contains specific configuration required for the execution of project workflows.
 */

@Schema(name = "ProjectInstance", description = "Contains specific configuration required for the execution of project workflows.")
@JsonTypeName("ProjectInstance")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-30T10:07:11.328260+02:00[Europe/Zagreb]")
public class ProjectInstanceModel {

  @JsonProperty("configurationParameters")
  @Valid
  private Map<String, Object> configurationParameters = null;

  @JsonProperty("connectionIds")
  @Valid
  private List<Long> connectionIds = null;

  @JsonProperty("connections")
  @Valid
  private List<com.bytechef.hermes.connection.web.rest.model.ConnectionModel> connections = null;

  @JsonProperty("description")
  private String description;

  @JsonProperty("createdBy")
  private String createdBy;

  @JsonProperty("createdDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  @JsonProperty("id")
  private Long id;

  @JsonProperty("lastExecutionDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastExecutionDate;

  @JsonProperty("lastModifiedBy")
  private String lastModifiedBy;

  @JsonProperty("lastModifiedDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  @JsonProperty("name")
  private String name;

  @JsonProperty("project")
  private ProjectModel project;

  @JsonProperty("projectId")
  private Long projectId;

  /**
   * The status of a project instance.
   */
  public enum StatusEnum {
    DISABLED("DISABLED"),
    
    ENABLED("ENABLED");

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

  @JsonProperty("__version")
  private Integer version;

  public ProjectInstanceModel configurationParameters(Map<String, Object> configurationParameters) {
    this.configurationParameters = configurationParameters;
    return this;
  }

  public ProjectInstanceModel putConfigurationParametersItem(String key, Object configurationParametersItem) {
    if (this.configurationParameters == null) {
      this.configurationParameters = new HashMap<>();
    }
    this.configurationParameters.put(key, configurationParametersItem);
    return this;
  }

  /**
   * The configuration parameters of an project instance used as workflow input values.
   * @return configurationParameters
  */
  
  @Schema(name = "configurationParameters", description = "The configuration parameters of an project instance used as workflow input values.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Map<String, Object> getConfigurationParameters() {
    return configurationParameters;
  }

  public void setConfigurationParameters(Map<String, Object> configurationParameters) {
    this.configurationParameters = configurationParameters;
  }

  public ProjectInstanceModel connectionIds(List<Long> connectionIds) {
    this.connectionIds = connectionIds;
    return this;
  }

  public ProjectInstanceModel addConnectionIdsItem(Long connectionIdsItem) {
    if (this.connectionIds == null) {
      this.connectionIds = new ArrayList<>();
    }
    this.connectionIds.add(connectionIdsItem);
    return this;
  }

  /**
   * The ids of connections used by a project instance.
   * @return connectionIds
  */
  
  @Schema(name = "connectionIds", description = "The ids of connections used by a project instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public List<Long> getConnectionIds() {
    return connectionIds;
  }

  public void setConnectionIds(List<Long> connectionIds) {
    this.connectionIds = connectionIds;
  }

  public ProjectInstanceModel connections(List<com.bytechef.hermes.connection.web.rest.model.ConnectionModel> connections) {
    this.connections = connections;
    return this;
  }

  public ProjectInstanceModel addConnectionsItem(com.bytechef.hermes.connection.web.rest.model.ConnectionModel connectionsItem) {
    if (this.connections == null) {
      this.connections = new ArrayList<>();
    }
    this.connections.add(connectionsItem);
    return this;
  }

  /**
   * The connections used by a project instance.
   * @return connections
  */
  @Valid 
  @Schema(name = "connections", accessMode = Schema.AccessMode.READ_ONLY, description = "The connections used by a project instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public List<com.bytechef.hermes.connection.web.rest.model.ConnectionModel> getConnections() {
    return connections;
  }

  public void setConnections(List<com.bytechef.hermes.connection.web.rest.model.ConnectionModel> connections) {
    this.connections = connections;
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
   * The last execution date of a project instance.
   * @return lastExecutionDate
  */
  @Valid 
  @Schema(name = "lastExecutionDate", description = "The last execution date of a project instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ProjectInstanceModel project(ProjectModel project) {
    this.project = project;
    return this;
  }

  /**
   * Get project
   * @return project
  */
  @Valid 
  @Schema(name = "project", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public ProjectModel getProject() {
    return project;
  }

  public void setProject(ProjectModel project) {
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
  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public ProjectInstanceModel status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * The status of a project instance.
   * @return status
  */
  
  @Schema(name = "status", description = "The status of a project instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public StatusEnum getStatus() {
    return status;
  }

  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  public ProjectInstanceModel tags(List<com.bytechef.tag.web.rest.model.TagModel> tags) {
    this.tags = tags;
    return this;
  }

  public ProjectInstanceModel addTagsItem(com.bytechef.tag.web.rest.model.TagModel tagsItem) {
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

  public ProjectInstanceModel version(Integer version) {
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
    ProjectInstanceModel projectInstance = (ProjectInstanceModel) o;
    return Objects.equals(this.configurationParameters, projectInstance.configurationParameters) &&
        Objects.equals(this.connectionIds, projectInstance.connectionIds) &&
        Objects.equals(this.connections, projectInstance.connections) &&
        Objects.equals(this.description, projectInstance.description) &&
        Objects.equals(this.createdBy, projectInstance.createdBy) &&
        Objects.equals(this.createdDate, projectInstance.createdDate) &&
        Objects.equals(this.id, projectInstance.id) &&
        Objects.equals(this.lastExecutionDate, projectInstance.lastExecutionDate) &&
        Objects.equals(this.lastModifiedBy, projectInstance.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, projectInstance.lastModifiedDate) &&
        Objects.equals(this.name, projectInstance.name) &&
        Objects.equals(this.project, projectInstance.project) &&
        Objects.equals(this.projectId, projectInstance.projectId) &&
        Objects.equals(this.status, projectInstance.status) &&
        Objects.equals(this.tags, projectInstance.tags) &&
        Objects.equals(this.version, projectInstance.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(configurationParameters, connectionIds, connections, description, createdBy, createdDate, id, lastExecutionDate, lastModifiedBy, lastModifiedDate, name, project, projectId, status, tags, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectInstanceModel {\n");
    sb.append("    configurationParameters: ").append(toIndentedString(configurationParameters)).append("\n");
    sb.append("    connectionIds: ").append(toIndentedString(connectionIds)).append("\n");
    sb.append("    connections: ").append(toIndentedString(connections)).append("\n");
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
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

