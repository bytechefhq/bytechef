package com.bytechef.automation.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The blueprint that describe the execution of a job.
 */

@Schema(name = "WorkflowBasic_1", description = "The blueprint that describe the execution of a job.")
@JsonTypeName("WorkflowBasic_1")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-05-13T21:52:04.260626+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public class WorkflowBasic1Model {

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  private Integer connectionsCount;

  private String description;

  private String id;

  private Integer inputsCount;

  private String label;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  private Boolean manualTrigger;

  @Valid
  private List<String> workflowTaskComponentNames = new ArrayList<>();

  @Valid
  private List<String> workflowTriggerComponentNames = new ArrayList<>();

  private Integer version;

  public WorkflowBasic1Model createdBy(String createdBy) {
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

  public WorkflowBasic1Model createdDate(LocalDateTime createdDate) {
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

  public WorkflowBasic1Model connectionsCount(Integer connectionsCount) {
    this.connectionsCount = connectionsCount;
    return this;
  }

  /**
   * The number of workflow connections
   * @return connectionsCount
  */
  
  @Schema(name = "connectionsCount", accessMode = Schema.AccessMode.READ_ONLY, description = "The number of workflow connections", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionsCount")
  public Integer getConnectionsCount() {
    return connectionsCount;
  }

  public void setConnectionsCount(Integer connectionsCount) {
    this.connectionsCount = connectionsCount;
  }

  public WorkflowBasic1Model description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of a workflow.
   * @return description
  */
  
  @Schema(name = "description", description = "The description of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public WorkflowBasic1Model id(String id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a workflow.
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public WorkflowBasic1Model inputsCount(Integer inputsCount) {
    this.inputsCount = inputsCount;
    return this;
  }

  /**
   * The number of workflow inputs
   * @return inputsCount
  */
  
  @Schema(name = "inputsCount", accessMode = Schema.AccessMode.READ_ONLY, description = "The number of workflow inputs", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputsCount")
  public Integer getInputsCount() {
    return inputsCount;
  }

  public void setInputsCount(Integer inputsCount) {
    this.inputsCount = inputsCount;
  }

  public WorkflowBasic1Model label(String label) {
    this.label = label;
    return this;
  }

  /**
   * The descriptive name for the workflow
   * @return label
  */
  
  @Schema(name = "label", accessMode = Schema.AccessMode.READ_ONLY, description = "The descriptive name for the workflow", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public WorkflowBasic1Model lastModifiedBy(String lastModifiedBy) {
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

  public WorkflowBasic1Model lastModifiedDate(LocalDateTime lastModifiedDate) {
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

  public WorkflowBasic1Model manualTrigger(Boolean manualTrigger) {
    this.manualTrigger = manualTrigger;
    return this;
  }

  /**
   * Does this workflow have a manual trigger or not
   * @return manualTrigger
  */
  
  @Schema(name = "manualTrigger", accessMode = Schema.AccessMode.READ_ONLY, description = "Does this workflow have a manual trigger or not", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("manualTrigger")
  public Boolean getManualTrigger() {
    return manualTrigger;
  }

  public void setManualTrigger(Boolean manualTrigger) {
    this.manualTrigger = manualTrigger;
  }

  public WorkflowBasic1Model workflowTaskComponentNames(List<String> workflowTaskComponentNames) {
    this.workflowTaskComponentNames = workflowTaskComponentNames;
    return this;
  }

  public WorkflowBasic1Model addWorkflowTaskComponentNamesItem(String workflowTaskComponentNamesItem) {
    if (this.workflowTaskComponentNames == null) {
      this.workflowTaskComponentNames = new ArrayList<>();
    }
    this.workflowTaskComponentNames.add(workflowTaskComponentNamesItem);
    return this;
  }

  /**
   * Get workflowTaskComponentNames
   * @return workflowTaskComponentNames
  */
  
  @Schema(name = "workflowTaskComponentNames", accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowTaskComponentNames")
  public List<String> getWorkflowTaskComponentNames() {
    return workflowTaskComponentNames;
  }

  public void setWorkflowTaskComponentNames(List<String> workflowTaskComponentNames) {
    this.workflowTaskComponentNames = workflowTaskComponentNames;
  }

  public WorkflowBasic1Model workflowTriggerComponentNames(List<String> workflowTriggerComponentNames) {
    this.workflowTriggerComponentNames = workflowTriggerComponentNames;
    return this;
  }

  public WorkflowBasic1Model addWorkflowTriggerComponentNamesItem(String workflowTriggerComponentNamesItem) {
    if (this.workflowTriggerComponentNames == null) {
      this.workflowTriggerComponentNames = new ArrayList<>();
    }
    this.workflowTriggerComponentNames.add(workflowTriggerComponentNamesItem);
    return this;
  }

  /**
   * Get workflowTriggerComponentNames
   * @return workflowTriggerComponentNames
  */
  
  @Schema(name = "workflowTriggerComponentNames", accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowTriggerComponentNames")
  public List<String> getWorkflowTriggerComponentNames() {
    return workflowTriggerComponentNames;
  }

  public void setWorkflowTriggerComponentNames(List<String> workflowTriggerComponentNames) {
    this.workflowTriggerComponentNames = workflowTriggerComponentNames;
  }

  public WorkflowBasic1Model version(Integer version) {
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
    WorkflowBasic1Model workflowBasic1 = (WorkflowBasic1Model) o;
    return Objects.equals(this.createdBy, workflowBasic1.createdBy) &&
        Objects.equals(this.createdDate, workflowBasic1.createdDate) &&
        Objects.equals(this.connectionsCount, workflowBasic1.connectionsCount) &&
        Objects.equals(this.description, workflowBasic1.description) &&
        Objects.equals(this.id, workflowBasic1.id) &&
        Objects.equals(this.inputsCount, workflowBasic1.inputsCount) &&
        Objects.equals(this.label, workflowBasic1.label) &&
        Objects.equals(this.lastModifiedBy, workflowBasic1.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, workflowBasic1.lastModifiedDate) &&
        Objects.equals(this.manualTrigger, workflowBasic1.manualTrigger) &&
        Objects.equals(this.workflowTaskComponentNames, workflowBasic1.workflowTaskComponentNames) &&
        Objects.equals(this.workflowTriggerComponentNames, workflowBasic1.workflowTriggerComponentNames) &&
        Objects.equals(this.version, workflowBasic1.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, connectionsCount, description, id, inputsCount, label, lastModifiedBy, lastModifiedDate, manualTrigger, workflowTaskComponentNames, workflowTriggerComponentNames, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowBasic1Model {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    connectionsCount: ").append(toIndentedString(connectionsCount)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    inputsCount: ").append(toIndentedString(inputsCount)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    manualTrigger: ").append(toIndentedString(manualTrigger)).append("\n");
    sb.append("    workflowTaskComponentNames: ").append(toIndentedString(workflowTaskComponentNames)).append("\n");
    sb.append("    workflowTriggerComponentNames: ").append(toIndentedString(workflowTriggerComponentNames)).append("\n");
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

