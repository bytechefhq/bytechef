package com.bytechef.automation.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.automation.configuration.web.rest.model.ProjectDeploymentWorkflowConnectionModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.OffsetDateTime;
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

@Schema(name = "ProjectDeploymentWorkflow", description = "Contains configuration and connections required for the execution of a particular project workflow.")
@JsonTypeName("ProjectDeploymentWorkflow")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-01-17T22:14:22.561228+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public class ProjectDeploymentWorkflowModel {

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdDate;

  @Valid
  private Map<String, Object> inputs = new HashMap<>();

  @Valid
  private List<@Valid ProjectDeploymentWorkflowConnectionModel> connections = new ArrayList<>();

  private Boolean enabled;

  private Long id;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime lastExecutionDate;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime lastModifiedDate;

  private Long projectDeploymentId;

  private String staticWebhookUrl;

  private String workflowId;

  private String workflowReferenceCode;

  private Integer version;

  public ProjectDeploymentWorkflowModel createdBy(String createdBy) {
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

  public ProjectDeploymentWorkflowModel createdDate(OffsetDateTime createdDate) {
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
  public OffsetDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(OffsetDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public ProjectDeploymentWorkflowModel inputs(Map<String, Object> inputs) {
    this.inputs = inputs;
    return this;
  }

  public ProjectDeploymentWorkflowModel putInputsItem(String key, Object inputsItem) {
    if (this.inputs == null) {
      this.inputs = new HashMap<>();
    }
    this.inputs.put(key, inputsItem);
    return this;
  }

  /**
   * The input parameters of an project deployment used as workflow input values.
   * @return inputs
   */
  
  @Schema(name = "inputs", description = "The input parameters of an project deployment used as workflow input values.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
  public Map<String, Object> getInputs() {
    return inputs;
  }

  public void setInputs(Map<String, Object> inputs) {
    this.inputs = inputs;
  }

  public ProjectDeploymentWorkflowModel connections(List<@Valid ProjectDeploymentWorkflowConnectionModel> connections) {
    this.connections = connections;
    return this;
  }

  public ProjectDeploymentWorkflowModel addConnectionsItem(ProjectDeploymentWorkflowConnectionModel connectionsItem) {
    if (this.connections == null) {
      this.connections = new ArrayList<>();
    }
    this.connections.add(connectionsItem);
    return this;
  }

  /**
   * The connections used by a project deployment.
   * @return connections
   */
  @Valid 
  @Schema(name = "connections", description = "The connections used by a project deployment.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connections")
  public List<@Valid ProjectDeploymentWorkflowConnectionModel> getConnections() {
    return connections;
  }

  public void setConnections(List<@Valid ProjectDeploymentWorkflowConnectionModel> connections) {
    this.connections = connections;
  }

  public ProjectDeploymentWorkflowModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If a workflow is enabled or not in the project deployment workflow.
   * @return enabled
   */
  
  @Schema(name = "enabled", description = "If a workflow is enabled or not in the project deployment workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public ProjectDeploymentWorkflowModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a project deployment workflow.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a project deployment workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ProjectDeploymentWorkflowModel lastExecutionDate(OffsetDateTime lastExecutionDate) {
    this.lastExecutionDate = lastExecutionDate;
    return this;
  }

  /**
   * The last execution date of a project deployment.
   * @return lastExecutionDate
   */
  @Valid 
  @Schema(name = "lastExecutionDate", description = "The last execution date of a project deployment.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastExecutionDate")
  public OffsetDateTime getLastExecutionDate() {
    return lastExecutionDate;
  }

  public void setLastExecutionDate(OffsetDateTime lastExecutionDate) {
    this.lastExecutionDate = lastExecutionDate;
  }

  public ProjectDeploymentWorkflowModel lastModifiedBy(String lastModifiedBy) {
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

  public ProjectDeploymentWorkflowModel lastModifiedDate(OffsetDateTime lastModifiedDate) {
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
  public OffsetDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public ProjectDeploymentWorkflowModel projectDeploymentId(Long projectDeploymentId) {
    this.projectDeploymentId = projectDeploymentId;
    return this;
  }

  /**
   * The id of a project deployment.
   * @return projectDeploymentId
   */
  
  @Schema(name = "projectDeploymentId", description = "The id of a project deployment.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectDeploymentId")
  public Long getProjectDeploymentId() {
    return projectDeploymentId;
  }

  public void setProjectDeploymentId(Long projectDeploymentId) {
    this.projectDeploymentId = projectDeploymentId;
  }

  public ProjectDeploymentWorkflowModel staticWebhookUrl(String staticWebhookUrl) {
    this.staticWebhookUrl = staticWebhookUrl;
    return this;
  }

  /**
   * The url of a static url used to trigger a workflow.
   * @return staticWebhookUrl
   */
  
  @Schema(name = "staticWebhookUrl", description = "The url of a static url used to trigger a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("staticWebhookUrl")
  public String getStaticWebhookUrl() {
    return staticWebhookUrl;
  }

  public void setStaticWebhookUrl(String staticWebhookUrl) {
    this.staticWebhookUrl = staticWebhookUrl;
  }

  public ProjectDeploymentWorkflowModel workflowId(String workflowId) {
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

  public ProjectDeploymentWorkflowModel workflowReferenceCode(String workflowReferenceCode) {
    this.workflowReferenceCode = workflowReferenceCode;
    return this;
  }

  /**
   * The workflow reference code
   * @return workflowReferenceCode
   */
  
  @Schema(name = "workflowReferenceCode", accessMode = Schema.AccessMode.READ_ONLY, description = "The workflow reference code", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowReferenceCode")
  public String getWorkflowReferenceCode() {
    return workflowReferenceCode;
  }

  public void setWorkflowReferenceCode(String workflowReferenceCode) {
    this.workflowReferenceCode = workflowReferenceCode;
  }

  public ProjectDeploymentWorkflowModel version(Integer version) {
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
    ProjectDeploymentWorkflowModel projectDeploymentWorkflow = (ProjectDeploymentWorkflowModel) o;
    return Objects.equals(this.createdBy, projectDeploymentWorkflow.createdBy) &&
        Objects.equals(this.createdDate, projectDeploymentWorkflow.createdDate) &&
        Objects.equals(this.inputs, projectDeploymentWorkflow.inputs) &&
        Objects.equals(this.connections, projectDeploymentWorkflow.connections) &&
        Objects.equals(this.enabled, projectDeploymentWorkflow.enabled) &&
        Objects.equals(this.id, projectDeploymentWorkflow.id) &&
        Objects.equals(this.lastExecutionDate, projectDeploymentWorkflow.lastExecutionDate) &&
        Objects.equals(this.lastModifiedBy, projectDeploymentWorkflow.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, projectDeploymentWorkflow.lastModifiedDate) &&
        Objects.equals(this.projectDeploymentId, projectDeploymentWorkflow.projectDeploymentId) &&
        Objects.equals(this.staticWebhookUrl, projectDeploymentWorkflow.staticWebhookUrl) &&
        Objects.equals(this.workflowId, projectDeploymentWorkflow.workflowId) &&
        Objects.equals(this.workflowReferenceCode, projectDeploymentWorkflow.workflowReferenceCode) &&
        Objects.equals(this.version, projectDeploymentWorkflow.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, inputs, connections, enabled, id, lastExecutionDate, lastModifiedBy, lastModifiedDate, projectDeploymentId, staticWebhookUrl, workflowId, workflowReferenceCode, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectDeploymentWorkflowModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    connections: ").append(toIndentedString(connections)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastExecutionDate: ").append(toIndentedString(lastExecutionDate)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    projectDeploymentId: ").append(toIndentedString(projectDeploymentId)).append("\n");
    sb.append("    staticWebhookUrl: ").append(toIndentedString(staticWebhookUrl)).append("\n");
    sb.append("    workflowId: ").append(toIndentedString(workflowId)).append("\n");
    sb.append("    workflowReferenceCode: ").append(toIndentedString(workflowReferenceCode)).append("\n");
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

