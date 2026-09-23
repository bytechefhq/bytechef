package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.AutomationWorkflowProjectComponentModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.AutomationWorkflowProjectWorkflowInputModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * A group of tasks that make one logical workflow.
 */

@Schema(name = "ConnectedUserProjectWorkflow", description = "A group of tasks that make one logical workflow.")
@JsonTypeName("ConnectedUserProjectWorkflow")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-23T21:34:31.743535+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class ConnectedUserProjectWorkflowModel {

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private @Nullable String description;

  private @Nullable String definition;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  private @Nullable Boolean enabled;

  private @Nullable String label;

  private @Nullable String workflowUuid;

  private @Nullable Integer workflowVersion;

  /**
   * COPY when the workflow is the user's own editable copy; REFERENCE when it points at a shared automation workflow.
   */
  public enum KindEnum {
    COPY("COPY"),
    
    REFERENCE("REFERENCE");

    private final String value;

    KindEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static KindEnum fromValue(String value) {
      for (KindEnum b : KindEnum.values()) {
        if (b.value.equalsIgnoreCase(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private @Nullable KindEnum kind;

  private @Nullable String automationWorkflowUuid;

  private @Nullable String copiedFromWorkflowUuid;

  private @Nullable Boolean dangling;

  @Valid
  private List<@Valid AutomationWorkflowProjectComponentModel> components = new ArrayList<>();

  @Valid
  private List<@Valid AutomationWorkflowProjectWorkflowInputModel> inputs = new ArrayList<>();

  @Valid
  private Map<String, Object> inputValues = new HashMap<>();

  private @Nullable String attentionReason;

  public ConnectedUserProjectWorkflowModel createdDate(@Nullable OffsetDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  /**
   * The created date of a workflow.
   * @return createdDate
   */
  @Valid 
  @Schema(name = "createdDate", description = "The created date of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdDate")
  public @Nullable OffsetDateTime getCreatedDate() {
    return createdDate;
  }

  @JsonProperty("createdDate")
  public void setCreatedDate(@Nullable OffsetDateTime createdDate) {
    this.createdDate = createdDate;
  }

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

  @JsonProperty("description")
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

  @JsonProperty("definition")
  public void setDefinition(@Nullable String definition) {
    this.definition = definition;
  }

  public ConnectedUserProjectWorkflowModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
    return this;
  }

  /**
   * The last modified date of a workflow.
   * @return lastModifiedDate
   */
  @Valid 
  @Schema(name = "lastModifiedDate", description = "The last modified date of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedDate")
  public @Nullable OffsetDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  @JsonProperty("lastModifiedDate")
  public void setLastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
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

  @JsonProperty("enabled")
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

  @JsonProperty("label")
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

  @JsonProperty("workflowUuid")
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

  @JsonProperty("workflowVersion")
  public void setWorkflowVersion(@Nullable Integer workflowVersion) {
    this.workflowVersion = workflowVersion;
  }

  public ConnectedUserProjectWorkflowModel kind(@Nullable KindEnum kind) {
    this.kind = kind;
    return this;
  }

  /**
   * COPY when the workflow is the user's own editable copy; REFERENCE when it points at a shared automation workflow.
   * @return kind
   */
  
  @Schema(name = "kind", description = "COPY when the workflow is the user's own editable copy; REFERENCE when it points at a shared automation workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("kind")
  public @Nullable KindEnum getKind() {
    return kind;
  }

  @JsonProperty("kind")
  public void setKind(@Nullable KindEnum kind) {
    this.kind = kind;
  }

  public ConnectedUserProjectWorkflowModel automationWorkflowUuid(@Nullable String automationWorkflowUuid) {
    this.automationWorkflowUuid = automationWorkflowUuid;
    return this;
  }

  /**
   * For REFERENCE rows, the uuid of the automation workflow being referenced.
   * @return automationWorkflowUuid
   */
  
  @Schema(name = "automationWorkflowUuid", description = "For REFERENCE rows, the uuid of the automation workflow being referenced.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("automationWorkflowUuid")
  public @Nullable String getAutomationWorkflowUuid() {
    return automationWorkflowUuid;
  }

  @JsonProperty("automationWorkflowUuid")
  public void setAutomationWorkflowUuid(@Nullable String automationWorkflowUuid) {
    this.automationWorkflowUuid = automationWorkflowUuid;
  }

  public ConnectedUserProjectWorkflowModel copiedFromWorkflowUuid(@Nullable String copiedFromWorkflowUuid) {
    this.copiedFromWorkflowUuid = copiedFromWorkflowUuid;
    return this;
  }

  /**
   * For COPY rows, the uuid of the automation workflow template the copy was created from. Null otherwise.
   * @return copiedFromWorkflowUuid
   */
  
  @Schema(name = "copiedFromWorkflowUuid", description = "For COPY rows, the uuid of the automation workflow template the copy was created from. Null otherwise.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("copiedFromWorkflowUuid")
  public @Nullable String getCopiedFromWorkflowUuid() {
    return copiedFromWorkflowUuid;
  }

  @JsonProperty("copiedFromWorkflowUuid")
  public void setCopiedFromWorkflowUuid(@Nullable String copiedFromWorkflowUuid) {
    this.copiedFromWorkflowUuid = copiedFromWorkflowUuid;
  }

  public ConnectedUserProjectWorkflowModel dangling(@Nullable Boolean dangling) {
    this.dangling = dangling;
    return this;
  }

  /**
   * True when a REFERENCE points at an automation workflow that is no longer served.
   * @return dangling
   */
  
  @Schema(name = "dangling", description = "True when a REFERENCE points at an automation workflow that is no longer served.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("dangling")
  public @Nullable Boolean getDangling() {
    return dangling;
  }

  @JsonProperty("dangling")
  public void setDangling(@Nullable Boolean dangling) {
    this.dangling = dangling;
  }

  public ConnectedUserProjectWorkflowModel components(List<@Valid AutomationWorkflowProjectComponentModel> components) {
    this.components = components;
    return this;
  }

  public ConnectedUserProjectWorkflowModel addComponentsItem(AutomationWorkflowProjectComponentModel componentsItem) {
    if (this.components == null) {
      this.components = new ArrayList<>();
    }
    this.components.add(componentsItem);
    return this;
  }

  /**
   * The components used by the workflow.
   * @return components
   */
  @Valid 
  @Schema(name = "components", description = "The components used by the workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("components")
  public List<@Valid AutomationWorkflowProjectComponentModel> getComponents() {
    return components;
  }

  @JsonProperty("components")
  public void setComponents(List<@Valid AutomationWorkflowProjectComponentModel> components) {
    this.components = components;
  }

  public ConnectedUserProjectWorkflowModel inputs(List<@Valid AutomationWorkflowProjectWorkflowInputModel> inputs) {
    this.inputs = inputs;
    return this;
  }

  public ConnectedUserProjectWorkflowModel addInputsItem(AutomationWorkflowProjectWorkflowInputModel inputsItem) {
    if (this.inputs == null) {
      this.inputs = new ArrayList<>();
    }
    this.inputs.add(inputsItem);
    return this;
  }

  /**
   * The values the connected user is asked for before this workflow runs.
   * @return inputs
   */
  @Valid 
  @Schema(name = "inputs", description = "The values the connected user is asked for before this workflow runs.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
  public List<@Valid AutomationWorkflowProjectWorkflowInputModel> getInputs() {
    return inputs;
  }

  @JsonProperty("inputs")
  public void setInputs(List<@Valid AutomationWorkflowProjectWorkflowInputModel> inputs) {
    this.inputs = inputs;
  }

  public ConnectedUserProjectWorkflowModel inputValues(Map<String, Object> inputValues) {
    this.inputValues = inputValues;
    return this;
  }

  public ConnectedUserProjectWorkflowModel putInputValuesItem(String key, Object inputValuesItem) {
    if (this.inputValues == null) {
      this.inputValues = new HashMap<>();
    }
    this.inputValues.put(key, inputValuesItem);
    return this;
  }

  /**
   * The values the connected user has already supplied, keyed by input name.
   * @return inputValues
   */
  
  @Schema(name = "inputValues", description = "The values the connected user has already supplied, keyed by input name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputValues")
  public Map<String, Object> getInputValues() {
    return inputValues;
  }

  @JsonProperty("inputValues")
  public void setInputValues(Map<String, Object> inputValues) {
    this.inputValues = inputValues;
  }

  public ConnectedUserProjectWorkflowModel attentionReason(@Nullable String attentionReason) {
    this.attentionReason = attentionReason;
    return this;
  }

  /**
   * Why a REFERENCE needs attention: MISSING_CONNECTION:<component>, INPUT_REQUIRED:<input> or UPDATE_PENDING. Null when healthy or for COPY rows.
   * @return attentionReason
   */
  
  @Schema(name = "attentionReason", description = "Why a REFERENCE needs attention: MISSING_CONNECTION:<component>, INPUT_REQUIRED:<input> or UPDATE_PENDING. Null when healthy or for COPY rows.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("attentionReason")
  public @Nullable String getAttentionReason() {
    return attentionReason;
  }

  @JsonProperty("attentionReason")
  public void setAttentionReason(@Nullable String attentionReason) {
    this.attentionReason = attentionReason;
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
    return Objects.equals(this.createdDate, connectedUserProjectWorkflow.createdDate) &&
        Objects.equals(this.description, connectedUserProjectWorkflow.description) &&
        Objects.equals(this.definition, connectedUserProjectWorkflow.definition) &&
        Objects.equals(this.lastModifiedDate, connectedUserProjectWorkflow.lastModifiedDate) &&
        Objects.equals(this.enabled, connectedUserProjectWorkflow.enabled) &&
        Objects.equals(this.label, connectedUserProjectWorkflow.label) &&
        Objects.equals(this.workflowUuid, connectedUserProjectWorkflow.workflowUuid) &&
        Objects.equals(this.workflowVersion, connectedUserProjectWorkflow.workflowVersion) &&
        Objects.equals(this.kind, connectedUserProjectWorkflow.kind) &&
        Objects.equals(this.automationWorkflowUuid, connectedUserProjectWorkflow.automationWorkflowUuid) &&
        Objects.equals(this.copiedFromWorkflowUuid, connectedUserProjectWorkflow.copiedFromWorkflowUuid) &&
        Objects.equals(this.dangling, connectedUserProjectWorkflow.dangling) &&
        Objects.equals(this.components, connectedUserProjectWorkflow.components) &&
        Objects.equals(this.inputs, connectedUserProjectWorkflow.inputs) &&
        Objects.equals(this.inputValues, connectedUserProjectWorkflow.inputValues) &&
        Objects.equals(this.attentionReason, connectedUserProjectWorkflow.attentionReason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdDate, description, definition, lastModifiedDate, enabled, label, workflowUuid, workflowVersion, kind, automationWorkflowUuid, copiedFromWorkflowUuid, dangling, components, inputs, inputValues, attentionReason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectedUserProjectWorkflowModel {\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    definition: ").append(toIndentedString(definition)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    workflowUuid: ").append(toIndentedString(workflowUuid)).append("\n");
    sb.append("    workflowVersion: ").append(toIndentedString(workflowVersion)).append("\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    automationWorkflowUuid: ").append(toIndentedString(automationWorkflowUuid)).append("\n");
    sb.append("    copiedFromWorkflowUuid: ").append(toIndentedString(copiedFromWorkflowUuid)).append("\n");
    sb.append("    dangling: ").append(toIndentedString(dangling)).append("\n");
    sb.append("    components: ").append(toIndentedString(components)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    inputValues: ").append(toIndentedString(inputValues)).append("\n");
    sb.append("    attentionReason: ").append(toIndentedString(attentionReason)).append("\n");
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

