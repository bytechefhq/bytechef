package com.bytechef.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationWorkflowConnectionModel;
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
 * Contains configuration and connections required for the execution of a particular integration workflow.
 */

@Schema(name = "IntegrationInstanceConfigurationWorkflow", description = "Contains configuration and connections required for the execution of a particular integration workflow.")
@JsonTypeName("IntegrationInstanceConfigurationWorkflow")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-05-10T07:17:06.076692+02:00[Europe/Zagreb]", comments = "Generator version: 7.4.0")
public class IntegrationInstanceConfigurationWorkflowModel {

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  @Valid
  private Map<String, Object> inputs = new HashMap<>();

  @Valid
  private List<@Valid IntegrationInstanceConfigurationWorkflowConnectionModel> connections;

  private Boolean enabled;

  private Long id;

  private Long integrationInstanceConfigurationId;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastExecutionDate;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  private String staticWebhookUrl;

  private String workflowId;

  private Integer version;

  public IntegrationInstanceConfigurationWorkflowModel createdBy(String createdBy) {
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

  public IntegrationInstanceConfigurationWorkflowModel createdDate(LocalDateTime createdDate) {
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

  public IntegrationInstanceConfigurationWorkflowModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If a workflow is enabled or not in the integration instance configuration workflow.
   * @return enabled
  */
  
  @Schema(name = "enabled", description = "If a workflow is enabled or not in the integration instance configuration workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public IntegrationInstanceConfigurationWorkflowModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an integration instance configuration workflow.
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an integration instance configuration workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public IntegrationInstanceConfigurationWorkflowModel integrationInstanceConfigurationId(Long integrationInstanceConfigurationId) {
    this.integrationInstanceConfigurationId = integrationInstanceConfigurationId;
    return this;
  }

  /**
   * The id of an integration instance configuration.
   * @return integrationInstanceConfigurationId
  */
  
  @Schema(name = "integrationInstanceConfigurationId", description = "The id of an integration instance configuration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationInstanceConfigurationId")
  public Long getIntegrationInstanceConfigurationId() {
    return integrationInstanceConfigurationId;
  }

  public void setIntegrationInstanceConfigurationId(Long integrationInstanceConfigurationId) {
    this.integrationInstanceConfigurationId = integrationInstanceConfigurationId;
  }

  public IntegrationInstanceConfigurationWorkflowModel lastExecutionDate(LocalDateTime lastExecutionDate) {
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
  public LocalDateTime getLastExecutionDate() {
    return lastExecutionDate;
  }

  public void setLastExecutionDate(LocalDateTime lastExecutionDate) {
    this.lastExecutionDate = lastExecutionDate;
  }

  public IntegrationInstanceConfigurationWorkflowModel lastModifiedBy(String lastModifiedBy) {
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

  public IntegrationInstanceConfigurationWorkflowModel lastModifiedDate(LocalDateTime lastModifiedDate) {
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

  public IntegrationInstanceConfigurationWorkflowModel staticWebhookUrl(String staticWebhookUrl) {
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

  public IntegrationInstanceConfigurationWorkflowModel workflowId(String workflowId) {
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

  public IntegrationInstanceConfigurationWorkflowModel version(Integer version) {
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
        Objects.equals(this.staticWebhookUrl, integrationInstanceConfigurationWorkflow.staticWebhookUrl) &&
        Objects.equals(this.workflowId, integrationInstanceConfigurationWorkflow.workflowId) &&
        Objects.equals(this.version, integrationInstanceConfigurationWorkflow.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, inputs, connections, enabled, id, integrationInstanceConfigurationId, lastExecutionDate, lastModifiedBy, lastModifiedDate, staticWebhookUrl, workflowId, version);
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
    sb.append("    staticWebhookUrl: ").append(toIndentedString(staticWebhookUrl)).append("\n");
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

