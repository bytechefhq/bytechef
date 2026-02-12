package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.InputModel;
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
 * A group of tasks that make one logical workflow.
 */

@Schema(name = "IntegrationWorkflow", description = "A group of tasks that make one logical workflow.")
@JsonTypeName("IntegrationWorkflow")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-12T08:42:40.890500807+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public class IntegrationWorkflowModel {

  private @Nullable String description;

  @Valid
  private List<@Valid InputModel> inputs = new ArrayList<>();

  private @Nullable String label;

  private @Nullable String workflowUuid;

  public IntegrationWorkflowModel description(@Nullable String description) {
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

  public IntegrationWorkflowModel inputs(List<@Valid InputModel> inputs) {
    this.inputs = inputs;
    return this;
  }

  public IntegrationWorkflowModel addInputsItem(InputModel inputsItem) {
    if (this.inputs == null) {
      this.inputs = new ArrayList<>();
    }
    this.inputs.add(inputsItem);
    return this;
  }

  /**
   * Get inputs
   * @return inputs
   */
  @Valid 
  @Schema(name = "inputs", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
  public List<@Valid InputModel> getInputs() {
    return inputs;
  }

  public void setInputs(List<@Valid InputModel> inputs) {
    this.inputs = inputs;
  }

  public IntegrationWorkflowModel label(@Nullable String label) {
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

  public IntegrationWorkflowModel workflowUuid(@Nullable String workflowUuid) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IntegrationWorkflowModel integrationWorkflow = (IntegrationWorkflowModel) o;
    return Objects.equals(this.description, integrationWorkflow.description) &&
        Objects.equals(this.inputs, integrationWorkflow.inputs) &&
        Objects.equals(this.label, integrationWorkflow.label) &&
        Objects.equals(this.workflowUuid, integrationWorkflow.workflowUuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, inputs, label, workflowUuid);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationWorkflowModel {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    workflowUuid: ").append(toIndentedString(workflowUuid)).append("\n");
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

