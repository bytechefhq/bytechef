package com.bytechef.helios.project.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.helios.project.web.rest.model.ProjectInstanceBasicModel;
import com.bytechef.hermes.connection.web.rest.model.ConnectionModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
 * Contains configuration and connections required for the execution of a particular project workflow.
 */

@Schema(name = "ProjectInstanceWorkflow", description = "Contains configuration and connections required for the execution of a particular project workflow.")
@JsonTypeName("ProjectInstanceWorkflow")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-25T07:55:30.301914+02:00[Europe/Zagreb]")
public class ProjectInstanceWorkflowModel {

  @Valid
  private Map<String, Object> inputParameters = new HashMap<>();

  @Valid
  private List<Long> connectionIds;

  @Valid
  private List<@Valid ConnectionModel> connections;

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  private Boolean enabled;

  private Long id;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastExecutionDate;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  private ProjectInstanceBasicModel projectInstance;

  private Long projectInstanceId;

  private Integer workflowId;

  private Integer version;

  public ProjectInstanceWorkflowModel inputParameters(Map<String, Object> inputParameters) {
    this.inputParameters = inputParameters;
    return this;
  }

  public ProjectInstanceWorkflowModel putInputParametersItem(String key, Object inputParametersItem) {
    if (this.inputParameters == null) {
      this.inputParameters = new HashMap<>();
    }
    this.inputParameters.put(key, inputParametersItem);
    return this;
  }

  /**
   * The input parameters of an project instance used as workflow input values.
   * @return inputParameters
  */
  
  @Schema(name = "inputParameters", description = "The input parameters of an project instance used as workflow input values.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputParameters")
  public Map<String, Object> getInputParameters() {
    return inputParameters;
  }

  public void setInputParameters(Map<String, Object> inputParameters) {
    this.inputParameters = inputParameters;
  }

  public ProjectInstanceWorkflowModel connectionIds(List<Long> connectionIds) {
    this.connectionIds = connectionIds;
    return this;
  }

  public ProjectInstanceWorkflowModel addConnectionIdsItem(Long connectionIdsItem) {
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
  @JsonProperty("connectionIds")
  public List<Long> getConnectionIds() {
    return connectionIds;
  }

  public void setConnectionIds(List<Long> connectionIds) {
    this.connectionIds = connectionIds;
  }

  public ProjectInstanceWorkflowModel connections(List<@Valid ConnectionModel> connections) {
    this.connections = connections;
    return this;
  }

  public ProjectInstanceWorkflowModel addConnectionsItem(ConnectionModel connectionsItem) {
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
  @JsonProperty("connections")
  public List<@Valid ConnectionModel> getConnections() {
    return connections;
  }

  public void setConnections(List<@Valid ConnectionModel> connections) {
    this.connections = connections;
  }

  public ProjectInstanceWorkflowModel createdBy(String createdBy) {
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

  public ProjectInstanceWorkflowModel createdDate(LocalDateTime createdDate) {
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

  public ProjectInstanceWorkflowModel enabled(Boolean enabled) {
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

  public ProjectInstanceWorkflowModel id(Long id) {
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

  public ProjectInstanceWorkflowModel lastExecutionDate(LocalDateTime lastExecutionDate) {
    this.lastExecutionDate = lastExecutionDate;
    return this;
  }

  /**
   * The last execution date of a project instance.
   * @return lastExecutionDate
  */
  @Valid 
  @Schema(name = "lastExecutionDate", description = "The last execution date of a project instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastExecutionDate")
  public LocalDateTime getLastExecutionDate() {
    return lastExecutionDate;
  }

  public void setLastExecutionDate(LocalDateTime lastExecutionDate) {
    this.lastExecutionDate = lastExecutionDate;
  }

  public ProjectInstanceWorkflowModel lastModifiedBy(String lastModifiedBy) {
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

  public ProjectInstanceWorkflowModel lastModifiedDate(LocalDateTime lastModifiedDate) {
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

  public ProjectInstanceWorkflowModel projectInstance(ProjectInstanceBasicModel projectInstance) {
    this.projectInstance = projectInstance;
    return this;
  }

  /**
   * Get projectInstance
   * @return projectInstance
  */
  @Valid 
  @Schema(name = "projectInstance", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectInstance")
  public ProjectInstanceBasicModel getProjectInstance() {
    return projectInstance;
  }

  public void setProjectInstance(ProjectInstanceBasicModel projectInstance) {
    this.projectInstance = projectInstance;
  }

  public ProjectInstanceWorkflowModel projectInstanceId(Long projectInstanceId) {
    this.projectInstanceId = projectInstanceId;
    return this;
  }

  /**
   * Th id of a project instance.
   * @return projectInstanceId
  */
  
  @Schema(name = "projectInstanceId", description = "Th id of a project instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectInstanceId")
  public Long getProjectInstanceId() {
    return projectInstanceId;
  }

  public void setProjectInstanceId(Long projectInstanceId) {
    this.projectInstanceId = projectInstanceId;
  }

  public ProjectInstanceWorkflowModel workflowId(Integer workflowId) {
    this.workflowId = workflowId;
    return this;
  }

  /**
   * The id of a workflow.
   * @return workflowId
  */
  
  @Schema(name = "workflowId", description = "The id of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowId")
  public Integer getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(Integer workflowId) {
    this.workflowId = workflowId;
  }

  public ProjectInstanceWorkflowModel version(Integer version) {
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
    ProjectInstanceWorkflowModel projectInstanceWorkflow = (ProjectInstanceWorkflowModel) o;
    return Objects.equals(this.inputParameters, projectInstanceWorkflow.inputParameters) &&
        Objects.equals(this.connectionIds, projectInstanceWorkflow.connectionIds) &&
        Objects.equals(this.connections, projectInstanceWorkflow.connections) &&
        Objects.equals(this.createdBy, projectInstanceWorkflow.createdBy) &&
        Objects.equals(this.createdDate, projectInstanceWorkflow.createdDate) &&
        Objects.equals(this.enabled, projectInstanceWorkflow.enabled) &&
        Objects.equals(this.id, projectInstanceWorkflow.id) &&
        Objects.equals(this.lastExecutionDate, projectInstanceWorkflow.lastExecutionDate) &&
        Objects.equals(this.lastModifiedBy, projectInstanceWorkflow.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, projectInstanceWorkflow.lastModifiedDate) &&
        Objects.equals(this.projectInstance, projectInstanceWorkflow.projectInstance) &&
        Objects.equals(this.projectInstanceId, projectInstanceWorkflow.projectInstanceId) &&
        Objects.equals(this.workflowId, projectInstanceWorkflow.workflowId) &&
        Objects.equals(this.version, projectInstanceWorkflow.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(inputParameters, connectionIds, connections, createdBy, createdDate, enabled, id, lastExecutionDate, lastModifiedBy, lastModifiedDate, projectInstance, projectInstanceId, workflowId, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectInstanceWorkflowModel {\n");
    sb.append("    inputParameters: ").append(toIndentedString(inputParameters)).append("\n");
    sb.append("    connectionIds: ").append(toIndentedString(connectionIds)).append("\n");
    sb.append("    connections: ").append(toIndentedString(connections)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastExecutionDate: ").append(toIndentedString(lastExecutionDate)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    projectInstance: ").append(toIndentedString(projectInstance)).append("\n");
    sb.append("    projectInstanceId: ").append(toIndentedString(projectInstanceId)).append("\n");
    sb.append("    workflowId: ").append(toIndentedString(workflowId)).append("\n");
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

