package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.ActionDefinitionBasicModel;
import com.bytechef.platform.configuration.web.rest.model.PropertyModel;
import com.bytechef.platform.configuration.web.rest.model.TaskDispatcherDefinitionBasicModel;
import com.bytechef.platform.configuration.web.rest.model.TriggerDefinitionBasicModel;
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
 * The workflow node output
 */

@Schema(name = "WorkflowNodeOutput", description = "The workflow node output")
@JsonTypeName("WorkflowNodeOutput")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-06-08T22:18:25.217051+02:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
public class WorkflowNodeOutputModel {

  private PropertyModel outputSchema;

  private @Nullable Object sampleOutput;

  private @Nullable Object placeholder;

  private @Nullable ActionDefinitionBasicModel actionDefinition;

  private @Nullable TaskDispatcherDefinitionBasicModel taskDispatcherDefinition;

  private @Nullable TriggerDefinitionBasicModel triggerDefinition;

  private String workflowNodeName;

  public WorkflowNodeOutputModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WorkflowNodeOutputModel(PropertyModel outputSchema, String workflowNodeName) {
    this.outputSchema = outputSchema;
    this.workflowNodeName = workflowNodeName;
  }

  public WorkflowNodeOutputModel outputSchema(PropertyModel outputSchema) {
    this.outputSchema = outputSchema;
    return this;
  }

  /**
   * Get outputSchema
   * @return outputSchema
   */
  @NotNull @Valid 
  @Schema(name = "outputSchema", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("outputSchema")
  public PropertyModel getOutputSchema() {
    return outputSchema;
  }

  public void setOutputSchema(PropertyModel outputSchema) {
    this.outputSchema = outputSchema;
  }

  public WorkflowNodeOutputModel sampleOutput(Object sampleOutput) {
    this.sampleOutput = sampleOutput;
    return this;
  }

  /**
   * The sample value of an output.
   * @return sampleOutput
   */
  
  @Schema(name = "sampleOutput", description = "The sample value of an output.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("sampleOutput")
  public Object getSampleOutput() {
    return sampleOutput;
  }

  public void setSampleOutput(Object sampleOutput) {
    this.sampleOutput = sampleOutput;
  }

  public WorkflowNodeOutputModel placeholder(Object placeholder) {
    this.placeholder = placeholder;
    return this;
  }

  /**
   * The placeholder of an output.
   * @return placeholder
   */
  
  @Schema(name = "placeholder", description = "The placeholder of an output.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("placeholder")
  public Object getPlaceholder() {
    return placeholder;
  }

  public void setPlaceholder(Object placeholder) {
    this.placeholder = placeholder;
  }

  public WorkflowNodeOutputModel actionDefinition(ActionDefinitionBasicModel actionDefinition) {
    this.actionDefinition = actionDefinition;
    return this;
  }

  /**
   * Get actionDefinition
   * @return actionDefinition
   */
  @Valid 
  @Schema(name = "actionDefinition", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("actionDefinition")
  public ActionDefinitionBasicModel getActionDefinition() {
    return actionDefinition;
  }

  public void setActionDefinition(ActionDefinitionBasicModel actionDefinition) {
    this.actionDefinition = actionDefinition;
  }

  public WorkflowNodeOutputModel taskDispatcherDefinition(TaskDispatcherDefinitionBasicModel taskDispatcherDefinition) {
    this.taskDispatcherDefinition = taskDispatcherDefinition;
    return this;
  }

  /**
   * Get taskDispatcherDefinition
   * @return taskDispatcherDefinition
   */
  @Valid 
  @Schema(name = "taskDispatcherDefinition", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("taskDispatcherDefinition")
  public TaskDispatcherDefinitionBasicModel getTaskDispatcherDefinition() {
    return taskDispatcherDefinition;
  }

  public void setTaskDispatcherDefinition(TaskDispatcherDefinitionBasicModel taskDispatcherDefinition) {
    this.taskDispatcherDefinition = taskDispatcherDefinition;
  }

  public WorkflowNodeOutputModel triggerDefinition(TriggerDefinitionBasicModel triggerDefinition) {
    this.triggerDefinition = triggerDefinition;
    return this;
  }

  /**
   * Get triggerDefinition
   * @return triggerDefinition
   */
  @Valid 
  @Schema(name = "triggerDefinition", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("triggerDefinition")
  public TriggerDefinitionBasicModel getTriggerDefinition() {
    return triggerDefinition;
  }

  public void setTriggerDefinition(TriggerDefinitionBasicModel triggerDefinition) {
    this.triggerDefinition = triggerDefinition;
  }

  public WorkflowNodeOutputModel workflowNodeName(String workflowNodeName) {
    this.workflowNodeName = workflowNodeName;
    return this;
  }

  /**
   * The workflow node name
   * @return workflowNodeName
   */
  @NotNull 
  @Schema(name = "workflowNodeName", description = "The workflow node name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workflowNodeName")
  public String getWorkflowNodeName() {
    return workflowNodeName;
  }

  public void setWorkflowNodeName(String workflowNodeName) {
    this.workflowNodeName = workflowNodeName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkflowNodeOutputModel workflowNodeOutput = (WorkflowNodeOutputModel) o;
    return Objects.equals(this.outputSchema, workflowNodeOutput.outputSchema) &&
        Objects.equals(this.sampleOutput, workflowNodeOutput.sampleOutput) &&
        Objects.equals(this.placeholder, workflowNodeOutput.placeholder) &&
        Objects.equals(this.actionDefinition, workflowNodeOutput.actionDefinition) &&
        Objects.equals(this.taskDispatcherDefinition, workflowNodeOutput.taskDispatcherDefinition) &&
        Objects.equals(this.triggerDefinition, workflowNodeOutput.triggerDefinition) &&
        Objects.equals(this.workflowNodeName, workflowNodeOutput.workflowNodeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(outputSchema, sampleOutput, placeholder, actionDefinition, taskDispatcherDefinition, triggerDefinition, workflowNodeName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowNodeOutputModel {\n");
    sb.append("    outputSchema: ").append(toIndentedString(outputSchema)).append("\n");
    sb.append("    sampleOutput: ").append(toIndentedString(sampleOutput)).append("\n");
    sb.append("    placeholder: ").append(toIndentedString(placeholder)).append("\n");
    sb.append("    actionDefinition: ").append(toIndentedString(actionDefinition)).append("\n");
    sb.append("    taskDispatcherDefinition: ").append(toIndentedString(taskDispatcherDefinition)).append("\n");
    sb.append("    triggerDefinition: ").append(toIndentedString(triggerDefinition)).append("\n");
    sb.append("    workflowNodeName: ").append(toIndentedString(workflowNodeName)).append("\n");
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

