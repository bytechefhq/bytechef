package com.bytechef.ee.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationWorkflowConnectionModel;
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
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Contains configuration and connections required for the execution of a particular integration workflow.
 */

@Schema(name = "IntegrationInstanceConfigurationWorkflow", description = "Contains configuration and connections required for the execution of a particular integration workflow.")
@JsonTypeName("IntegrationInstanceConfigurationWorkflow")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:35.556332+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class IntegrationInstanceConfigurationWorkflowModel {

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  @Valid
  private Map<String, Object> inputs = new HashMap<>();

  @Valid
  private List<@Valid IntegrationInstanceConfigurationWorkflowConnectionModel> connections = new ArrayList<>();

  private @Nullable Boolean enabled;

  private @Nullable Long id;

  private @Nullable Long integrationInstanceConfigurationId;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastExecutionDate;

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  private @Nullable String workflowId;

  private @Nullable String workflowUuid;

  private @Nullable Integer version;

  public IntegrationInstanceConfigurationWorkflowModel createdBy(@Nullable String createdBy) {
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

  public IntegrationInstanceConfigurationWorkflowModel createdDate(@Nullable OffsetDateTime createdDate) {
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

  public IntegrationInstanceConfigurationWorkflowModel inputs(Map<String, Object> inputs) {
    this.inputs = inputs;
    return this;
  }

  public IntegrationInstanceConfigurationWorkflowModel putInputsItem(String key, Object inputsItem) {
    if (this.inputs == null) {
      this.inputs = new HashMap<>();
    }
    this.inputs.put(key, inputsItem);
    return this;
  }

  /**
   * The input parameters of an integration instance configuration used as workflow input values.
   * @return inputs
   */
  
  @Schema(name = "inputs", description = "The input parameters of an integration instance configuration used as workflow input values.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
  public Map<String, Object> getInputs() {
    return inputs;
  }

  public void setInputs(Map<String, Object> inputs) {
    this.inputs = inputs;
  }

  public IntegrationInstanceConfigurationWorkflowModel connections(List<@Valid IntegrationInstanceConfigurationWorkflowConnectionModel> connections) {
    this.connections = connections;
    return this;
  }

  public IntegrationInstanceConfigurationWorkflowModel addConnectionsItem(IntegrationInstanceConfigurationWorkflowConnectionModel connectionsItem) {
    if (this.connections == null) {
      this.connections = new ArrayList<>();
    }
    this.connections.add(connectionsItem);
    return this;
  }

  /**
   * The connections used by an integration instance configuration.
   * @return connections
   */
  @Valid 
  @Schema(name = "connections", description = "The connections used by an integration instance configuration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connections")
  public List<@Valid IntegrationInstanceConfigurationWorkflowConnectionModel> getConnections() {
    return connections;
  }

  public void setConnections(List<@Valid IntegrationInstanceConfigurationWorkflowConnectionModel> connections) {
    this.connections = connections;
  }

  public IntegrationInstanceConfigurationWorkflowModel enabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If a workflow is enabled or not in the integration instance configuration workflow.
   * @return enabled
   */
  
  @Schema(name = "enabled", description = "If a workflow is enabled or not in the integration instance configuration workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public @Nullable Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
  }

  public IntegrationInstanceConfigurationWorkflowModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an integration instance configuration workflow.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an integration instance configuration workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public IntegrationInstanceConfigurationWorkflowModel integrationInstanceConfigurationId(@Nullable Long integrationInstanceConfigurationId) {
    this.integrationInstanceConfigurationId = integrationInstanceConfigurationId;
    return this;
  }

  /**
   * The id of an integration instance configuration.
   * @return integrationInstanceConfigurationId
   */
  
  @Schema(name = "integrationInstanceConfigurationId", description = "The id of an integration instance configuration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationInstanceConfigurationId")
  public @Nullable Long getIntegrationInstanceConfigurationId() {
    return integrationInstanceConfigurationId;
  }

  public void setIntegrationInstanceConfigurationId(@Nullable Long integrationInstanceConfigurationId) {
    this.integrationInstanceConfigurationId = integrationInstanceConfigurationId;
  }

  public IntegrationInstanceConfigurationWorkflowModel lastExecutionDate(@Nullable OffsetDateTime lastExecutionDate) {
    this.lastExecutionDate = lastExecutionDate;
    return this;
  }

  /**
   * The last execution date of an integration instance configuration workflow.
   * @return lastExecutionDate
   */
  @Valid 
  @Schema(name = "lastExecutionDate", description = "The last execution date of an integration instance configuration workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastExecutionDate")
  public @Nullable OffsetDateTime getLastExecutionDate() {
    return lastExecutionDate;
  }

  public void setLastExecutionDate(@Nullable OffsetDateTime lastExecutionDate) {
    this.lastExecutionDate = lastExecutionDate;
  }

  public IntegrationInstanceConfigurationWorkflowModel lastModifiedBy(@Nullable String lastModifiedBy) {
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

  public IntegrationInstanceConfigurationWorkflowModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
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

  public IntegrationInstanceConfigurationWorkflowModel workflowId(@Nullable String workflowId) {
    this.workflowId = workflowId;
    return this;
  }

  /**
   * The id of a workflow.
   * @return workflowId
   */
  
  @Schema(name = "workflowId", description = "The id of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowId")
  public @Nullable String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(@Nullable String workflowId) {
    this.workflowId = workflowId;
  }

  public IntegrationInstanceConfigurationWorkflowModel workflowUuid(@Nullable String workflowUuid) {
    this.workflowUuid = workflowUuid;
    return this;
  }

  /**
   * The workflow uuid
   * @return workflowUuid
   */
  
  @Schema(name = "workflowUuid", accessMode = Schema.AccessMode.READ_ONLY, description = "The workflow uuid", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowUuid")
  public @Nullable String getWorkflowUuid() {
    return workflowUuid;
  }

  public void setWorkflowUuid(@Nullable String workflowUuid) {
    this.workflowUuid = workflowUuid;
  }

  public IntegrationInstanceConfigurationWorkflowModel version(@Nullable Integer version) {
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
    IntegrationInstanceConfigurationWorkflowModel integrationInstanceConfigurationWorkflow = (IntegrationInstanceConfigurationWorkflowModel) o;
    return Objects.equals(this.createdBy, integrationInstanceConfigurationWorkflow.createdBy) &&
        Objects.equals(this.createdDate, integrationInstanceConfigurationWorkflow.createdDate) &&
        Objects.equals(this.inputs, integrationInstanceConfigurationWorkflow.inputs) &&
        Objects.equals(this.connections, integrationInstanceConfigurationWorkflow.connections) &&
        Objects.equals(this.enabled, integrationInstanceConfigurationWorkflow.enabled) &&
        Objects.equals(this.id, integrationInstanceConfigurationWorkflow.id) &&
        Objects.equals(this.integrationInstanceConfigurationId, integrationInstanceConfigurationWorkflow.integrationInstanceConfigurationId) &&
        Objects.equals(this.lastExecutionDate, integrationInstanceConfigurationWorkflow.lastExecutionDate) &&
        Objects.equals(this.lastModifiedBy, integrationInstanceConfigurationWorkflow.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, integrationInstanceConfigurationWorkflow.lastModifiedDate) &&
        Objects.equals(this.workflowId, integrationInstanceConfigurationWorkflow.workflowId) &&
        Objects.equals(this.workflowUuid, integrationInstanceConfigurationWorkflow.workflowUuid) &&
        Objects.equals(this.version, integrationInstanceConfigurationWorkflow.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, inputs, connections, enabled, id, integrationInstanceConfigurationId, lastExecutionDate, lastModifiedBy, lastModifiedDate, workflowId, workflowUuid, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationInstanceConfigurationWorkflowModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    connections: ").append(toIndentedString(connections)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    integrationInstanceConfigurationId: ").append(toIndentedString(integrationInstanceConfigurationId)).append("\n");
    sb.append("    lastExecutionDate: ").append(toIndentedString(lastExecutionDate)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    workflowId: ").append(toIndentedString(workflowId)).append("\n");
    sb.append("    workflowUuid: ").append(toIndentedString(workflowUuid)).append("\n");
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

