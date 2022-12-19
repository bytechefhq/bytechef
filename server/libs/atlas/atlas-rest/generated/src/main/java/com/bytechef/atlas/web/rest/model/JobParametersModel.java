package com.bytechef.atlas.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Defines parameters used to execute a job.
 */

@Schema(name = "JobParameters", description = "Defines parameters used to execute a job.")
@JsonTypeName("JobParameters")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-12-13T14:42:06.111940+01:00[Europe/Zagreb]")
public class JobParametersModel {

  @JsonProperty("inputs")
  @Valid
  private Map<String, Object> inputs = null;

  @JsonProperty("label")
  private String label;

  @JsonProperty("outputs")
  @Valid
  private Map<String, Object> outputs = null;

  @JsonProperty("parentTaskExecutionId")
  private String parentTaskExecutionId;

  @JsonProperty("priority")
  private Integer priority;

  @JsonProperty("workflowId")
  private String workflowId;

  @JsonProperty("webhooks")
  @Valid
  private List<Map<String, Object>> webhooks = null;

  public JobParametersModel inputs(Map<String, Object> inputs) {
    this.inputs = inputs;
    return this;
  }

  public JobParametersModel putInputsItem(String key, Object inputsItem) {
    if (this.inputs == null) {
      this.inputs = new HashMap<>();
    }
    this.inputs.put(key, inputsItem);
    return this;
  }

  /**
   * The inputs expected by the workflow
   * @return inputs
  */
  
  @Schema(name = "inputs", description = "The inputs expected by the workflow", required = false)
  public Map<String, Object> getInputs() {
    return inputs;
  }

  public void setInputs(Map<String, Object> inputs) {
    this.inputs = inputs;
  }

  public JobParametersModel label(String label) {
    this.label = label;
    return this;
  }

  /**
   * The job's human-readable name
   * @return label
  */
  
  @Schema(name = "label", description = "The job's human-readable name", required = false)
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public JobParametersModel outputs(Map<String, Object> outputs) {
    this.outputs = outputs;
    return this;
  }

  public JobParametersModel putOutputsItem(String key, Object outputsItem) {
    if (this.outputs == null) {
      this.outputs = new HashMap<>();
    }
    this.outputs.put(key, outputsItem);
    return this;
  }

  /**
   * The outputs expected by the workflow.
   * @return outputs
  */
  
  @Schema(name = "outputs", description = "The outputs expected by the workflow.", required = false)
  public Map<String, Object> getOutputs() {
    return outputs;
  }

  public void setOutputs(Map<String, Object> outputs) {
    this.outputs = outputs;
  }

  public JobParametersModel parentTaskExecutionId(String parentTaskExecutionId) {
    this.parentTaskExecutionId = parentTaskExecutionId;
    return this;
  }

  /**
   * The id of the parent task that created this job. Used for sub-flows.
   * @return parentTaskExecutionId
  */
  
  @Schema(name = "parentTaskExecutionId", description = "The id of the parent task that created this job. Used for sub-flows.", required = false)
  public String getParentTaskExecutionId() {
    return parentTaskExecutionId;
  }

  public void setParentTaskExecutionId(String parentTaskExecutionId) {
    this.parentTaskExecutionId = parentTaskExecutionId;
  }

  public JobParametersModel priority(Integer priority) {
    this.priority = priority;
    return this;
  }

  /**
   * The priority value used during execution of individual tasks.
   * @return priority
  */
  
  @Schema(name = "priority", description = "The priority value used during execution of individual tasks.", required = false)
  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public JobParametersModel workflowId(String workflowId) {
    this.workflowId = workflowId;
    return this;
  }

  /**
   * Id of the workflow to execute.
   * @return workflowId
  */
  @NotNull 
  @Schema(name = "workflowId", description = "Id of the workflow to execute.", required = true)
  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public JobParametersModel webhooks(List<Map<String, Object>> webhooks) {
    this.webhooks = webhooks;
    return this;
  }

  public JobParametersModel addWebhooksItem(Map<String, Object> webhooksItem) {
    if (this.webhooks == null) {
      this.webhooks = new ArrayList<>();
    }
    this.webhooks.add(webhooksItem);
    return this;
  }

  /**
   * The list of webhooks to register to receive notifications for certain events.
   * @return webhooks
  */
  @Valid 
  @Schema(name = "webhooks", description = "The list of webhooks to register to receive notifications for certain events.", required = false)
  public List<Map<String, Object>> getWebhooks() {
    return webhooks;
  }

  public void setWebhooks(List<Map<String, Object>> webhooks) {
    this.webhooks = webhooks;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JobParametersModel jobParameters = (JobParametersModel) o;
    return Objects.equals(this.inputs, jobParameters.inputs) &&
        Objects.equals(this.label, jobParameters.label) &&
        Objects.equals(this.outputs, jobParameters.outputs) &&
        Objects.equals(this.parentTaskExecutionId, jobParameters.parentTaskExecutionId) &&
        Objects.equals(this.priority, jobParameters.priority) &&
        Objects.equals(this.workflowId, jobParameters.workflowId) &&
        Objects.equals(this.webhooks, jobParameters.webhooks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(inputs, label, outputs, parentTaskExecutionId, priority, workflowId, webhooks);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JobParametersModel {\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    outputs: ").append(toIndentedString(outputs)).append("\n");
    sb.append("    parentTaskExecutionId: ").append(toIndentedString(parentTaskExecutionId)).append("\n");
    sb.append("    priority: ").append(toIndentedString(priority)).append("\n");
    sb.append("    workflowId: ").append(toIndentedString(workflowId)).append("\n");
    sb.append("    webhooks: ").append(toIndentedString(webhooks)).append("\n");
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

