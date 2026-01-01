package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.ActionDefinitionBasicModel;
import com.bytechef.platform.configuration.web.rest.model.OutputResponseModel;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-02T08:44:56.413708+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class WorkflowNodeOutputModel {

  private @Nullable ActionDefinitionBasicModel actionDefinition;

  private @Nullable OutputResponseModel outputResponse;

  private @Nullable TaskDispatcherDefinitionBasicModel taskDispatcherDefinition;

  private Boolean testOutputResponse = false;

  private @Nullable TriggerDefinitionBasicModel triggerDefinition;

  private @Nullable OutputResponseModel variableOutputResponse;

  private String workflowNodeName;

  public WorkflowNodeOutputModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WorkflowNodeOutputModel(String workflowNodeName) {
    this.workflowNodeName = workflowNodeName;
  }

  public WorkflowNodeOutputModel actionDefinition(@Nullable ActionDefinitionBasicModel actionDefinition) {
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
  public @Nullable ActionDefinitionBasicModel getActionDefinition() {
    return actionDefinition;
  }

  public void setActionDefinition(@Nullable ActionDefinitionBasicModel actionDefinition) {
    this.actionDefinition = actionDefinition;
  }

  public WorkflowNodeOutputModel outputResponse(@Nullable OutputResponseModel outputResponse) {
    this.outputResponse = outputResponse;
    return this;
  }

  /**
   * Get outputResponse
   * @return outputResponse
   */
  @Valid 
  @Schema(name = "outputResponse", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("outputResponse")
  public @Nullable OutputResponseModel getOutputResponse() {
    return outputResponse;
  }

  public void setOutputResponse(@Nullable OutputResponseModel outputResponse) {
    this.outputResponse = outputResponse;
  }

  public WorkflowNodeOutputModel taskDispatcherDefinition(@Nullable TaskDispatcherDefinitionBasicModel taskDispatcherDefinition) {
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
  public @Nullable TaskDispatcherDefinitionBasicModel getTaskDispatcherDefinition() {
    return taskDispatcherDefinition;
  }

  public void setTaskDispatcherDefinition(@Nullable TaskDispatcherDefinitionBasicModel taskDispatcherDefinition) {
    this.taskDispatcherDefinition = taskDispatcherDefinition;
  }

  public WorkflowNodeOutputModel testOutputResponse(Boolean testOutputResponse) {
    this.testOutputResponse = testOutputResponse;
    return this;
  }

  /**
   * If the output response is a sample or the real one
   * @return testOutputResponse
   */
  
  @Schema(name = "testOutputResponse", description = "If the output response is a sample or the real one", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("testOutputResponse")
  public Boolean getTestOutputResponse() {
    return testOutputResponse;
  }

  public void setTestOutputResponse(Boolean testOutputResponse) {
    this.testOutputResponse = testOutputResponse;
  }

  public WorkflowNodeOutputModel triggerDefinition(@Nullable TriggerDefinitionBasicModel triggerDefinition) {
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
  public @Nullable TriggerDefinitionBasicModel getTriggerDefinition() {
    return triggerDefinition;
  }

  public void setTriggerDefinition(@Nullable TriggerDefinitionBasicModel triggerDefinition) {
    this.triggerDefinition = triggerDefinition;
  }

  public WorkflowNodeOutputModel variableOutputResponse(@Nullable OutputResponseModel variableOutputResponse) {
    this.variableOutputResponse = variableOutputResponse;
    return this;
  }

  /**
   * Get variableOutputResponse
   * @return variableOutputResponse
   */
  @Valid 
  @Schema(name = "variableOutputResponse", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("variableOutputResponse")
  public @Nullable OutputResponseModel getVariableOutputResponse() {
    return variableOutputResponse;
  }

  public void setVariableOutputResponse(@Nullable OutputResponseModel variableOutputResponse) {
    this.variableOutputResponse = variableOutputResponse;
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
        Objects.equals(this.outputResponse, workflowNodeOutput.outputResponse) &&
        Objects.equals(this.taskDispatcherDefinition, workflowNodeOutput.taskDispatcherDefinition) &&
        Objects.equals(this.testOutputResponse, workflowNodeOutput.testOutputResponse) &&
        Objects.equals(this.triggerDefinition, workflowNodeOutput.triggerDefinition) &&
        Objects.equals(this.variableOutputResponse, workflowNodeOutput.variableOutputResponse) &&
        Objects.equals(this.workflowNodeName, workflowNodeOutput.workflowNodeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(actionDefinition, outputResponse, taskDispatcherDefinition, testOutputResponse, triggerDefinition, variableOutputResponse, workflowNodeName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowNodeOutputModel {\n");
    sb.append("    actionDefinition: ").append(toIndentedString(actionDefinition)).append("\n");
    sb.append("    outputResponse: ").append(toIndentedString(outputResponse)).append("\n");
    sb.append("    taskDispatcherDefinition: ").append(toIndentedString(taskDispatcherDefinition)).append("\n");
    sb.append("    testOutputResponse: ").append(toIndentedString(testOutputResponse)).append("\n");
    sb.append("    triggerDefinition: ").append(toIndentedString(triggerDefinition)).append("\n");
    sb.append("    variableOutputResponse: ").append(toIndentedString(variableOutputResponse)).append("\n");
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

