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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-06-10T07:38:50.872123+02:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
public class IntegrationWorkflowModel {

  private @Nullable String description;

  private @Nullable String definition;

  private @Nullable String label;

  private @Nullable String workflowReferenceCode;

  @Valid
  private List<@Valid InputModel> inputs = new ArrayList<>();

  public IntegrationWorkflowModel description(String description) {
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

  public IntegrationWorkflowModel definition(String definition) {
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

  public IntegrationWorkflowModel label(String label) {
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

  public IntegrationWorkflowModel workflowReferenceCode(String workflowReferenceCode) {
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
        Objects.equals(this.definition, integrationWorkflow.definition) &&
        Objects.equals(this.label, integrationWorkflow.label) &&
        Objects.equals(this.workflowReferenceCode, integrationWorkflow.workflowReferenceCode) &&
        Objects.equals(this.inputs, integrationWorkflow.inputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, definition, label, workflowReferenceCode, inputs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationWorkflowModel {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    definition: ").append(toIndentedString(definition)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    workflowReferenceCode: ").append(toIndentedString(workflowReferenceCode)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
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

