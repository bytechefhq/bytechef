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

@Schema(name = "AutomationWorkflow", description = "A group of tasks that make one logical workflow.")
@JsonTypeName("AutomationWorkflow")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-06-10T07:38:50.872123+02:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
public class AutomationWorkflowModel {

  private @Nullable String description;

  private @Nullable String definition;

  private @Nullable String label;

  private @Nullable String workflowReferenceCode;

  private @Nullable Integer workflowVersion;

  public AutomationWorkflowModel description(String description) {
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

  public AutomationWorkflowModel definition(String definition) {
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

  public AutomationWorkflowModel label(String label) {
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

  public AutomationWorkflowModel workflowReferenceCode(String workflowReferenceCode) {
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

  public AutomationWorkflowModel workflowVersion(Integer workflowVersion) {
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
    AutomationWorkflowModel automationWorkflow = (AutomationWorkflowModel) o;
    return Objects.equals(this.description, automationWorkflow.description) &&
        Objects.equals(this.definition, automationWorkflow.definition) &&
        Objects.equals(this.label, automationWorkflow.label) &&
        Objects.equals(this.workflowReferenceCode, automationWorkflow.workflowReferenceCode) &&
        Objects.equals(this.workflowVersion, automationWorkflow.workflowVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, definition, label, workflowReferenceCode, workflowVersion);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AutomationWorkflowModel {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    definition: ").append(toIndentedString(definition)).append("\n");
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

