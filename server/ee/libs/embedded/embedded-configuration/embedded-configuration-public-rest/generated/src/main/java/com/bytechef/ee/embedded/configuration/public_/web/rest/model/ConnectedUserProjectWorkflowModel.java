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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-12T08:42:40.890500807+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public class ConnectedUserProjectWorkflowModel {

  private @Nullable String description;

  private @Nullable String definition;

  private @Nullable Boolean enabled;

  private @Nullable String label;

  private @Nullable String workflowUuid;

  private @Nullable Integer workflowVersion;

  public ConnectedUserProjectWorkflowModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of a workflow.
   * @return description
   */
  
  @Schema(name = "description", description = "The description of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public ConnectedUserProjectWorkflowModel definition(@Nullable String definition) {
    this.definition = definition;
    return this;
  }

  /**
   * The definition of a workflow.
   * @return definition
   */
  
  @Schema(name = "definition", description = "The definition of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("definition")
  public @Nullable String getDefinition() {
    return definition;
  }

  public void setDefinition(@Nullable String definition) {
    this.definition = definition;
  }

  public ConnectedUserProjectWorkflowModel enabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If a workflow is enabled or not.
   * @return enabled
   */
  
  @Schema(name = "enabled", description = "If a workflow is enabled or not.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public @Nullable Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
  }

  public ConnectedUserProjectWorkflowModel label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * The label of a workflow.
   * @return label
   */
  
  @Schema(name = "label", description = "The label of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  public void setLabel(@Nullable String label) {
    this.label = label;
  }

  public ConnectedUserProjectWorkflowModel workflowUuid(@Nullable String workflowUuid) {
    this.workflowUuid = workflowUuid;
    return this;
  }

  /**
   * The reference code of a workflow.
   * @return workflowUuid
   */
  
  @Schema(name = "workflowUuid", description = "The reference code of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowUuid")
  public @Nullable String getWorkflowUuid() {
    return workflowUuid;
  }

  public void setWorkflowUuid(@Nullable String workflowUuid) {
    this.workflowUuid = workflowUuid;
  }

  public ConnectedUserProjectWorkflowModel workflowVersion(@Nullable Integer workflowVersion) {
    this.workflowVersion = workflowVersion;
    return this;
  }

  /**
   * The workflow version, if null a workflow is not yet published
   * @return workflowVersion
   */
  
  @Schema(name = "workflowVersion", description = "The workflow version, if null a workflow is not yet published", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowVersion")
  public @Nullable Integer getWorkflowVersion() {
    return workflowVersion;
  }

  public void setWorkflowVersion(@Nullable Integer workflowVersion) {
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
        Objects.equals(this.workflowUuid, connectedUserProjectWorkflow.workflowUuid) &&
        Objects.equals(this.workflowVersion, connectedUserProjectWorkflow.workflowVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, definition, enabled, label, workflowUuid, workflowVersion);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectedUserProjectWorkflowModel {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    definition: ").append(toIndentedString(definition)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    workflowUuid: ").append(toIndentedString(workflowUuid)).append("\n");
    sb.append("    workflowVersion: ").append(toIndentedString(workflowVersion)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

