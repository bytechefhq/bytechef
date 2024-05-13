package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.ActionDefinitionModel;
import com.bytechef.platform.configuration.web.rest.model.PropertyModel;
import com.bytechef.platform.configuration.web.rest.model.TaskDispatcherDefinitionModel;
import com.bytechef.platform.configuration.web.rest.model.TriggerDefinitionModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-05-13T21:52:05.180663+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public class WorkflowNodeOutputModel {

  private ActionDefinitionModel actionDefinition;

  private PropertyModel outputSchema;

  private Object sampleOutput;

  private TaskDispatcherDefinitionModel taskDispatcherDefinition;

  private TriggerDefinitionModel triggerDefinition;

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

  public WorkflowNodeOutputModel actionDefinition(ActionDefinitionModel actionDefinition) {
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
  public ActionDefinitionModel getActionDefinition() {
    return actionDefinition;
  }

  public void setActionDefinition(ActionDefinitionModel actionDefinition) {
    this.actionDefinition = actionDefinition;
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

  public WorkflowNodeOutputModel taskDispatcherDefinition(TaskDispatcherDefinitionModel taskDispatcherDefinition) {
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
  public TaskDispatcherDefinitionModel getTaskDispatcherDefinition() {
    return taskDispatcherDefinition;
  }

  public void setTaskDispatcherDefinition(TaskDispatcherDefinitionModel taskDispatcherDefinition) {
    this.taskDispatcherDefinition = taskDispatcherDefinition;
  }

  public WorkflowNodeOutputModel triggerDefinition(TriggerDefinitionModel triggerDefinition) {
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
  public TriggerDefinitionModel getTriggerDefinition() {
    return triggerDefinition;
  }

  public void setTriggerDefinition(TriggerDefinitionModel triggerDefinition) {
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
    return Objects.equals(this.actionDefinition, workflowNodeOutput.actionDefinition) &&
        Objects.equals(this.outputSchema, workflowNodeOutput.outputSchema) &&
        Objects.equals(this.sampleOutput, workflowNodeOutput.sampleOutput) &&
        Objects.equals(this.taskDispatcherDefinition, workflowNodeOutput.taskDispatcherDefinition) &&
        Objects.equals(this.triggerDefinition, workflowNodeOutput.triggerDefinition) &&
        Objects.equals(this.workflowNodeName, workflowNodeOutput.workflowNodeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(actionDefinition, outputSchema, sampleOutput, taskDispatcherDefinition, triggerDefinition, workflowNodeName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowNodeOutputModel {\n");
    sb.append("    actionDefinition: ").append(toIndentedString(actionDefinition)).append("\n");
    sb.append("    outputSchema: ").append(toIndentedString(outputSchema)).append("\n");
    sb.append("    sampleOutput: ").append(toIndentedString(sampleOutput)).append("\n");
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

