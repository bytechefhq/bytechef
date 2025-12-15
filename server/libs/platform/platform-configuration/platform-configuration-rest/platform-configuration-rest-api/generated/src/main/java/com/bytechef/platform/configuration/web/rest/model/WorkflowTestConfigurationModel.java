package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.WorkflowTestConfigurationConnectionModel;
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
 * Contains configuration and connections required for the test execution of a particular workflow.
 */

@Schema(name = "WorkflowTestConfiguration", description = "Contains configuration and connections required for the test execution of a particular workflow.")
@JsonTypeName("WorkflowTestConfiguration")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-15T09:52:48.574632+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class WorkflowTestConfigurationModel {

  @Valid
  private List<@Valid WorkflowTestConfigurationConnectionModel> connections = new ArrayList<>();

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private Long environmentId;

  @Valid
  private Map<String, String> inputs = new HashMap<>();

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  private @Nullable String workflowId;

  private @Nullable Integer version;

  public WorkflowTestConfigurationModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WorkflowTestConfigurationModel(Long environmentId) {
    this.environmentId = environmentId;
  }

  public WorkflowTestConfigurationModel connections(List<@Valid WorkflowTestConfigurationConnectionModel> connections) {
    this.connections = connections;
    return this;
  }

  public WorkflowTestConfigurationModel addConnectionsItem(WorkflowTestConfigurationConnectionModel connectionsItem) {
    if (this.connections == null) {
      this.connections = new ArrayList<>();
    }
    this.connections.add(connectionsItem);
    return this;
  }

  /**
   * The connections used by workflow test.
   * @return connections
   */
  @Valid 
  @Schema(name = "connections", description = "The connections used by workflow test.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connections")
  public List<@Valid WorkflowTestConfigurationConnectionModel> getConnections() {
    return connections;
  }

  public void setConnections(List<@Valid WorkflowTestConfigurationConnectionModel> connections) {
    this.connections = connections;
  }

  public WorkflowTestConfigurationModel createdBy(@Nullable String createdBy) {
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

  public WorkflowTestConfigurationModel createdDate(@Nullable OffsetDateTime createdDate) {
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

  public WorkflowTestConfigurationModel environmentId(Long environmentId) {
    this.environmentId = environmentId;
    return this;
  }

  /**
   * The id of an environment.
   * @return environmentId
   */
  @NotNull 
  @Schema(name = "environmentId", description = "The id of an environment.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("environmentId")
  public Long getEnvironmentId() {
    return environmentId;
  }

  public void setEnvironmentId(Long environmentId) {
    this.environmentId = environmentId;
  }

  public WorkflowTestConfigurationModel inputs(Map<String, String> inputs) {
    this.inputs = inputs;
    return this;
  }

  public WorkflowTestConfigurationModel putInputsItem(String key, String inputsItem) {
    if (this.inputs == null) {
      this.inputs = new HashMap<>();
    }
    this.inputs.put(key, inputsItem);
    return this;
  }

  /**
   * The input parameters used as workflow input values.
   * @return inputs
   */
  
  @Schema(name = "inputs", description = "The input parameters used as workflow input values.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
  public Map<String, String> getInputs() {
    return inputs;
  }

  public void setInputs(Map<String, String> inputs) {
    this.inputs = inputs;
  }

  public WorkflowTestConfigurationModel lastModifiedBy(@Nullable String lastModifiedBy) {
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

  public WorkflowTestConfigurationModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
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

  public WorkflowTestConfigurationModel workflowId(@Nullable String workflowId) {
    this.workflowId = workflowId;
    return this;
  }

  /**
   * The id of a workflow.
   * @return workflowId
   */
  
  @Schema(name = "workflowId", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowId")
  public @Nullable String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(@Nullable String workflowId) {
    this.workflowId = workflowId;
  }

  public WorkflowTestConfigurationModel version(@Nullable Integer version) {
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
    WorkflowTestConfigurationModel workflowTestConfiguration = (WorkflowTestConfigurationModel) o;
    return Objects.equals(this.connections, workflowTestConfiguration.connections) &&
        Objects.equals(this.createdBy, workflowTestConfiguration.createdBy) &&
        Objects.equals(this.createdDate, workflowTestConfiguration.createdDate) &&
        Objects.equals(this.environmentId, workflowTestConfiguration.environmentId) &&
        Objects.equals(this.inputs, workflowTestConfiguration.inputs) &&
        Objects.equals(this.lastModifiedBy, workflowTestConfiguration.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, workflowTestConfiguration.lastModifiedDate) &&
        Objects.equals(this.workflowId, workflowTestConfiguration.workflowId) &&
        Objects.equals(this.version, workflowTestConfiguration.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connections, createdBy, createdDate, environmentId, inputs, lastModifiedBy, lastModifiedDate, workflowId, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowTestConfigurationModel {\n");
    sb.append("    connections: ").append(toIndentedString(connections)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    environmentId: ").append(toIndentedString(environmentId)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
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

