package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A group of tasks that make one logical workflow.
 */

@Schema(name = "ConnectedUserProjectWorkflow", description = "A group of tasks that make one logical workflow.")
@JsonTypeName("ConnectedUserProjectWorkflow")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-06-24T21:36:46.689421+02:00[Europe/Zagreb]", comments = "Generator version: 7.13.0")
public class ConnectedUserProjectWorkflowModel {

  private @Nullable String description;

  private @Nullable String definition;

  private @Nullable Boolean enabled;

  private @Nullable String label;

  private @Nullable String workflowReferenceCode;

  private @Nullable Integer workflowVersion;

  public ConnectedUserProjectWorkflowModel description(String description) {
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

  public ConnectedUserProjectWorkflowModel definition(String definition) {
    this.definition = definition;
    return this;
  }

  /**
   * The definition of a workflow.
   * @return definition
   */
  
  @Schema(name = "definition", description = "The definition of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("definition")
  public String getDefinition() {
    return definition;
  }

  public void setDefinition(String definition) {
    this.definition = definition;
  }

  public ConnectedUserProjectWorkflowModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If a workflow is enabled or not.
   * @return enabled
   */
  
  @Schema(name = "enabled", description = "If a workflow is enabled or not.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public ConnectedUserProjectWorkflowModel label(String label) {
    this.label = label;
    return this;
  }

  /**
   * The label of a workflow.
   * @return label
   */
  
  @Schema(name = "label", description = "The label of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public ConnectedUserProjectWorkflowModel workflowReferenceCode(String workflowReferenceCode) {
    this.workflowReferenceCode = workflowReferenceCode;
    return this;
  }

  /**
   * The reference code of a workflow.
   * @return workflowReferenceCode
   */
  
  @Schema(name = "workflowReferenceCode", description = "The reference code of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowReferenceCode")
  public String getWorkflowReferenceCode() {
    return workflowReferenceCode;
  }

  public void setWorkflowReferenceCode(String workflowReferenceCode) {
    this.workflowReferenceCode = workflowReferenceCode;
  }

  public ConnectedUserProjectWorkflowModel workflowVersion(Integer workflowVersion) {
    this.workflowVersion = workflowVersion;
    return this;
  }

  /**
   * The workflow version, if null a workflow is not yet published
   * @return workflowVersion
   */
  
  @Schema(name = "workflowVersion", description = "The workflow version, if null a workflow is not yet published", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowVersion")
  public Integer getWorkflowVersion() {
    return workflowVersion;
  }

  public void setWorkflowVersion(Integer workflowVersion) {
    this.workflowVersion = workflowVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConnectedUserProjectWorkflowModel connectedUserProjectWorkflow = (ConnectedUserProjectWorkflowModel) o;
    return Objects.equals(this.description, connectedUserProjectWorkflow.description) &&
        Objects.equals(this.definition, connectedUserProjectWorkflow.definition) &&
        Objects.equals(this.enabled, connectedUserProjectWorkflow.enabled) &&
        Objects.equals(this.label, connectedUserProjectWorkflow.label) &&
        Objects.equals(this.workflowReferenceCode, connectedUserProjectWorkflow.workflowReferenceCode) &&
        Objects.equals(this.workflowVersion, connectedUserProjectWorkflow.workflowVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, definition, enabled, label, workflowReferenceCode, workflowVersion);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectedUserProjectWorkflowModel {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    definition: ").append(toIndentedString(definition)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    workflowReferenceCode: ").append(toIndentedString(workflowReferenceCode)).append("\n");
    sb.append("    workflowVersion: ").append(toIndentedString(workflowVersion)).append("\n");
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

