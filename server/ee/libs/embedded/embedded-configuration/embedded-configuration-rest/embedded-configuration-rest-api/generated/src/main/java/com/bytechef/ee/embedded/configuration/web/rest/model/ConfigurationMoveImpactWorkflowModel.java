package com.bytechef.ee.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A workflow of the target configuration that needs new required inputs or connections.
 */

@Schema(name = "ConfigurationMoveImpactWorkflow", description = "A workflow of the target configuration that needs new required inputs or connections.")
@JsonTypeName("ConfigurationMoveImpactWorkflow")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-03T02:20:55.684519+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ConfigurationMoveImpactWorkflowModel {

  private Integer affectedConnectedUserCount;

  private String label;

  private List<String> newRequiredConnections = new ArrayList<>();

  private List<String> newRequiredInputs = new ArrayList<>();

  private String workflowUuid;

  public ConfigurationMoveImpactWorkflowModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ConfigurationMoveImpactWorkflowModel(Integer affectedConnectedUserCount, String label, List<String> newRequiredConnections, List<String> newRequiredInputs, String workflowUuid) {
    this.affectedConnectedUserCount = affectedConnectedUserCount;
    this.label = label;
    this.newRequiredConnections = newRequiredConnections;
    this.newRequiredInputs = newRequiredInputs;
    this.workflowUuid = workflowUuid;
  }

  public ConfigurationMoveImpactWorkflowModel affectedConnectedUserCount(Integer affectedConnectedUserCount) {
    this.affectedConnectedUserCount = affectedConnectedUserCount;
    return this;
  }

  /**
   * The number of the moved connected users affected.
   * @return affectedConnectedUserCount
   */
  @NotNull 
  @Schema(name = "affectedConnectedUserCount", description = "The number of the moved connected users affected.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("affectedConnectedUserCount")
  public Integer getAffectedConnectedUserCount() {
    return affectedConnectedUserCount;
  }

  @JsonProperty("affectedConnectedUserCount")
  public void setAffectedConnectedUserCount(Integer affectedConnectedUserCount) {
    this.affectedConnectedUserCount = affectedConnectedUserCount;
  }

  public ConfigurationMoveImpactWorkflowModel label(String label) {
    this.label = label;
    return this;
  }

  /**
   * The label of the workflow.
   * @return label
   */
  @NotNull 
  @Schema(name = "label", description = "The label of the workflow.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(String label) {
    this.label = label;
  }

  public ConfigurationMoveImpactWorkflowModel newRequiredConnections(List<String> newRequiredConnections) {
    this.newRequiredConnections = newRequiredConnections;
    return this;
  }

  public ConfigurationMoveImpactWorkflowModel addNewRequiredConnectionsItem(String newRequiredConnectionsItem) {
    if (this.newRequiredConnections == null) {
      this.newRequiredConnections = new ArrayList<>();
    }
    this.newRequiredConnections.add(newRequiredConnectionsItem);
    return this;
  }

  /**
   * The components of the new required connections.
   * @return newRequiredConnections
   */
  @NotNull 
  @Schema(name = "newRequiredConnections", description = "The components of the new required connections.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("newRequiredConnections")
  public List<String> getNewRequiredConnections() {
    return newRequiredConnections;
  }

  @JsonProperty("newRequiredConnections")
  public void setNewRequiredConnections(List<String> newRequiredConnections) {
    this.newRequiredConnections = newRequiredConnections;
  }

  public ConfigurationMoveImpactWorkflowModel newRequiredInputs(List<String> newRequiredInputs) {
    this.newRequiredInputs = newRequiredInputs;
    return this;
  }

  public ConfigurationMoveImpactWorkflowModel addNewRequiredInputsItem(String newRequiredInputsItem) {
    if (this.newRequiredInputs == null) {
      this.newRequiredInputs = new ArrayList<>();
    }
    this.newRequiredInputs.add(newRequiredInputsItem);
    return this;
  }

  /**
   * The names of the new required inputs.
   * @return newRequiredInputs
   */
  @NotNull 
  @Schema(name = "newRequiredInputs", description = "The names of the new required inputs.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("newRequiredInputs")
  public List<String> getNewRequiredInputs() {
    return newRequiredInputs;
  }

  @JsonProperty("newRequiredInputs")
  public void setNewRequiredInputs(List<String> newRequiredInputs) {
    this.newRequiredInputs = newRequiredInputs;
  }

  public ConfigurationMoveImpactWorkflowModel workflowUuid(String workflowUuid) {
    this.workflowUuid = workflowUuid;
    return this;
  }

  /**
   * The uuid of the workflow.
   * @return workflowUuid
   */
  @NotNull 
  @Schema(name = "workflowUuid", description = "The uuid of the workflow.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workflowUuid")
  public String getWorkflowUuid() {
    return workflowUuid;
  }

  @JsonProperty("workflowUuid")
  public void setWorkflowUuid(String workflowUuid) {
    this.workflowUuid = workflowUuid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConfigurationMoveImpactWorkflowModel configurationMoveImpactWorkflow = (ConfigurationMoveImpactWorkflowModel) o;
    return Objects.equals(this.affectedConnectedUserCount, configurationMoveImpactWorkflow.affectedConnectedUserCount) &&
        Objects.equals(this.label, configurationMoveImpactWorkflow.label) &&
        Objects.equals(this.newRequiredConnections, configurationMoveImpactWorkflow.newRequiredConnections) &&
        Objects.equals(this.newRequiredInputs, configurationMoveImpactWorkflow.newRequiredInputs) &&
        Objects.equals(this.workflowUuid, configurationMoveImpactWorkflow.workflowUuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(affectedConnectedUserCount, label, newRequiredConnections, newRequiredInputs, workflowUuid);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConfigurationMoveImpactWorkflowModel {\n");
    sb.append("    affectedConnectedUserCount: ").append(toIndentedString(affectedConnectedUserCount)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    newRequiredConnections: ").append(toIndentedString(newRequiredConnections)).append("\n");
    sb.append("    newRequiredInputs: ").append(toIndentedString(newRequiredInputs)).append("\n");
    sb.append("    workflowUuid: ").append(toIndentedString(workflowUuid)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

