package com.bytechef.ee.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.OffsetDateTime;
import java.util.HashMap;
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
 * Contains user configurations for the execution of a particular integration workflow.
 */

@Schema(name = "IntegrationInstanceWorkflow", description = "Contains user configurations for the execution of a particular integration workflow.")
@JsonTypeName("IntegrationInstanceWorkflow")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-23T12:37:00.328650+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class IntegrationInstanceWorkflowModel {

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  @Valid
  private Map<String, Object> inputs = new HashMap<>();

  private Boolean enabled;

  private @Nullable Long id;

  private Long integrationInstanceConfigurationWorkflowId;

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  private String workflowId;

  public IntegrationInstanceWorkflowModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public IntegrationInstanceWorkflowModel(Boolean enabled, Long integrationInstanceConfigurationWorkflowId, String workflowId) {
    this.enabled = enabled;
    this.integrationInstanceConfigurationWorkflowId = integrationInstanceConfigurationWorkflowId;
    this.workflowId = workflowId;
  }

  public IntegrationInstanceWorkflowModel createdBy(@Nullable String createdBy) {
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

  public IntegrationInstanceWorkflowModel createdDate(@Nullable OffsetDateTime createdDate) {
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

  public IntegrationInstanceWorkflowModel inputs(Map<String, Object> inputs) {
    this.inputs = inputs;
    return this;
  }

  public IntegrationInstanceWorkflowModel putInputsItem(String key, Object inputsItem) {
    if (this.inputs == null) {
      this.inputs = new HashMap<>();
    }
    this.inputs.put(key, inputsItem);
    return this;
  }

  /**
   * The input parameters of an integration instance used as workflow input values.
   * @return inputs
   */
  
  @Schema(name = "inputs", description = "The input parameters of an integration instance used as workflow input values.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
  public Map<String, Object> getInputs() {
    return inputs;
  }

  public void setInputs(Map<String, Object> inputs) {
    this.inputs = inputs;
  }

  public IntegrationInstanceWorkflowModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If a workflow is enabled or not in the integration instance workflow.
   * @return enabled
   */
  @NotNull 
  @Schema(name = "enabled", description = "If a workflow is enabled or not in the integration instance workflow.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public IntegrationInstanceWorkflowModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a project deployment workflow.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a project deployment workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public IntegrationInstanceWorkflowModel integrationInstanceConfigurationWorkflowId(Long integrationInstanceConfigurationWorkflowId) {
    this.integrationInstanceConfigurationWorkflowId = integrationInstanceConfigurationWorkflowId;
    return this;
  }

  /**
   * The id of a integration instance configuration workflow.
   * @return integrationInstanceConfigurationWorkflowId
   */
  @NotNull 
  @Schema(name = "integrationInstanceConfigurationWorkflowId", description = "The id of a integration instance configuration workflow.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("integrationInstanceConfigurationWorkflowId")
  public Long getIntegrationInstanceConfigurationWorkflowId() {
    return integrationInstanceConfigurationWorkflowId;
  }

  public void setIntegrationInstanceConfigurationWorkflowId(Long integrationInstanceConfigurationWorkflowId) {
    this.integrationInstanceConfigurationWorkflowId = integrationInstanceConfigurationWorkflowId;
  }

  public IntegrationInstanceWorkflowModel lastModifiedBy(@Nullable String lastModifiedBy) {
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

  public IntegrationInstanceWorkflowModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
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

  public IntegrationInstanceWorkflowModel workflowId(String workflowId) {
    this.workflowId = workflowId;
    return this;
  }

  /**
   * The id of a workflow.
   * @return workflowId
   */
  @NotNull 
  @Schema(name = "workflowId", description = "The id of a workflow.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workflowId")
  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IntegrationInstanceWorkflowModel integrationInstanceWorkflow = (IntegrationInstanceWorkflowModel) o;
    return Objects.equals(this.createdBy, integrationInstanceWorkflow.createdBy) &&
        Objects.equals(this.createdDate, integrationInstanceWorkflow.createdDate) &&
        Objects.equals(this.inputs, integrationInstanceWorkflow.inputs) &&
        Objects.equals(this.enabled, integrationInstanceWorkflow.enabled) &&
        Objects.equals(this.id, integrationInstanceWorkflow.id) &&
        Objects.equals(this.integrationInstanceConfigurationWorkflowId, integrationInstanceWorkflow.integrationInstanceConfigurationWorkflowId) &&
        Objects.equals(this.lastModifiedBy, integrationInstanceWorkflow.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, integrationInstanceWorkflow.lastModifiedDate) &&
        Objects.equals(this.workflowId, integrationInstanceWorkflow.workflowId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, inputs, enabled, id, integrationInstanceConfigurationWorkflowId, lastModifiedBy, lastModifiedDate, workflowId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationInstanceWorkflowModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    integrationInstanceConfigurationWorkflowId: ").append(toIndentedString(integrationInstanceConfigurationWorkflowId)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    workflowId: ").append(toIndentedString(workflowId)).append("\n");
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

