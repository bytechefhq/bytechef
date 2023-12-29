package com.bytechef.helios.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.helios.configuration.web.rest.model.ProjectInstanceWorkflowConnectionModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-01-05T07:50:53.784118+01:00[Europe/Zagreb]")
public class ProjectInstanceWorkflowModel {

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  @Valid
  private Map<String, Object> inputs = new HashMap<>();

  @Valid
  private List<@Valid ProjectInstanceWorkflowConnectionModel> connections;

  private Boolean enabled;

  private Long id;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastExecutionDate;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  private Long projectInstanceId;

  private String workflowId;

  private Integer version;

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

  public ProjectInstanceWorkflowModel inputs(Map<String, Object> inputs) {
    this.inputs = inputs;
    return this;
  }

  public ProjectInstanceWorkflowModel putInputsItem(String key, Object inputsItem) {
    if (this.inputs == null) {
      this.inputs = new HashMap<>();
    }
    this.inputs.put(key, inputsItem);
    return this;
  }

  /**
   * The input parameters of an project instance used as workflow input values.
   * @return inputs
  */
  
  @Schema(name = "inputs", description = "The input parameters of an project instance used as workflow input values.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
  public Map<String, Object> getInputs() {
    return inputs;
  }

  public void setInputs(Map<String, Object> inputs) {
    this.inputs = inputs;
  }

  public ProjectInstanceWorkflowModel connections(List<@Valid ProjectInstanceWorkflowConnectionModel> connections) {
    this.connections = connections;
    return this;
  }

  public ProjectInstanceWorkflowModel addConnectionsItem(ProjectInstanceWorkflowConnectionModel connectionsItem) {
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
  @Schema(name = "connections", description = "The connections used by a project instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connections")
  public List<@Valid ProjectInstanceWorkflowConnectionModel> getConnections() {
    return connections;
  }

  public void setConnections(List<@Valid ProjectInstanceWorkflowConnectionModel> connections) {
    this.connections = connections;
  }

  public ProjectInstanceWorkflowModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If a workflow is enabled or not in the project instance workflow.
   * @return enabled
  */
  
  @Schema(name = "enabled", description = "If a workflow is enabled or not in the project instance workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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

  public ProjectInstanceWorkflowModel projectInstanceId(Long projectInstanceId) {
    this.projectInstanceId = projectInstanceId;
    return this;
  }

  /**
   * The id of a project instance.
   * @return projectInstanceId
  */
  
  @Schema(name = "projectInstanceId", description = "The id of a project instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectInstanceId")
  public Long getProjectInstanceId() {
    return projectInstanceId;
  }

  public void setProjectInstanceId(Long projectInstanceId) {
    this.projectInstanceId = projectInstanceId;
  }

  public ProjectInstanceWorkflowModel workflowId(String workflowId) {
    this.workflowId = workflowId;
    return this;
  }

  /**
   * The id of a workflow.
   * @return workflowId
  */
  
  @Schema(name = "workflowId", description = "The id of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowId")
  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
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
    return Objects.equals(this.createdBy, projectInstanceWorkflow.createdBy) &&
        Objects.equals(this.createdDate, projectInstanceWorkflow.createdDate) &&
        Objects.equals(this.inputs, projectInstanceWorkflow.inputs) &&
        Objects.equals(this.connections, projectInstanceWorkflow.connections) &&
        Objects.equals(this.enabled, projectInstanceWorkflow.enabled) &&
        Objects.equals(this.id, projectInstanceWorkflow.id) &&
        Objects.equals(this.lastExecutionDate, projectInstanceWorkflow.lastExecutionDate) &&
        Objects.equals(this.lastModifiedBy, projectInstanceWorkflow.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, projectInstanceWorkflow.lastModifiedDate) &&
        Objects.equals(this.projectInstanceId, projectInstanceWorkflow.projectInstanceId) &&
        Objects.equals(this.workflowId, projectInstanceWorkflow.workflowId) &&
        Objects.equals(this.version, projectInstanceWorkflow.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, inputs, connections, enabled, id, lastExecutionDate, lastModifiedBy, lastModifiedDate, projectInstanceId, workflowId, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectInstanceWorkflowModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    connections: ").append(toIndentedString(connections)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastExecutionDate: ").append(toIndentedString(lastExecutionDate)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
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

