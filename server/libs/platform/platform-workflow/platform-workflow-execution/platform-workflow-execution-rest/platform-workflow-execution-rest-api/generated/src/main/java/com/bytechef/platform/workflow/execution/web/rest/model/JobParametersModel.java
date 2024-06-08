package com.bytechef.platform.workflow.execution.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.workflow.execution.web.rest.model.JobConnectionModel;
import com.bytechef.platform.workflow.execution.web.rest.model.TriggerOutputModel;
import com.bytechef.platform.workflow.execution.web.rest.model.WebhookModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-08T11:00:32.662245+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public class JobParametersModel {

  @Valid
  private List<@Valid JobConnectionModel> connections = new ArrayList<>();

  @Valid
  private Map<String, Object> inputs = new HashMap<>();

  private String label;

  private String parentTaskExecutionId;

  private Integer priority;

  @Valid
  private List<@Valid TriggerOutputModel> triggerOutputs = new ArrayList<>();

  private String workflowId;

  @Valid
  private List<@Valid WebhookModel> webhooks = new ArrayList<>();

  public JobParametersModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public JobParametersModel(String workflowId) {
    this.workflowId = workflowId;
  }

  public JobParametersModel connections(List<@Valid JobConnectionModel> connections) {
    this.connections = connections;
    return this;
  }

  public JobParametersModel addConnectionsItem(JobConnectionModel connectionsItem) {
    if (this.connections == null) {
      this.connections = new ArrayList<>();
    }
    this.connections.add(connectionsItem);
    return this;
  }

  /**
   * Get connections
   * @return connections
  */
  @Valid 
  @Schema(name = "connections", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connections")
  public List<@Valid JobConnectionModel> getConnections() {
    return connections;
  }

  public void setConnections(List<@Valid JobConnectionModel> connections) {
    this.connections = connections;
  }

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
  
  @Schema(name = "inputs", description = "The inputs expected by the workflow", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
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
  
  @Schema(name = "label", description = "The job's human-readable name", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public JobParametersModel parentTaskExecutionId(String parentTaskExecutionId) {
    this.parentTaskExecutionId = parentTaskExecutionId;
    return this;
  }

  /**
   * The id of the parent task that created this job. Used for sub-flows.
   * @return parentTaskExecutionId
  */
  
  @Schema(name = "parentTaskExecutionId", description = "The id of the parent task that created this job. Used for sub-flows.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("parentTaskExecutionId")
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
  
  @Schema(name = "priority", description = "The priority value used during execution of individual tasks.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("priority")
  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public JobParametersModel triggerOutputs(List<@Valid TriggerOutputModel> triggerOutputs) {
    this.triggerOutputs = triggerOutputs;
    return this;
  }

  public JobParametersModel addTriggerOutputsItem(TriggerOutputModel triggerOutputsItem) {
    if (this.triggerOutputs == null) {
      this.triggerOutputs = new ArrayList<>();
    }
    this.triggerOutputs.add(triggerOutputsItem);
    return this;
  }

  /**
   * Get triggerOutputs
   * @return triggerOutputs
  */
  @Valid 
  @Schema(name = "triggerOutputs", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("triggerOutputs")
  public List<@Valid TriggerOutputModel> getTriggerOutputs() {
    return triggerOutputs;
  }

  public void setTriggerOutputs(List<@Valid TriggerOutputModel> triggerOutputs) {
    this.triggerOutputs = triggerOutputs;
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
  @Schema(name = "workflowId", description = "Id of the workflow to execute.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workflowId")
  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public JobParametersModel webhooks(List<@Valid WebhookModel> webhooks) {
    this.webhooks = webhooks;
    return this;
  }

  public JobParametersModel addWebhooksItem(WebhookModel webhooksItem) {
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
  @Schema(name = "webhooks", description = "The list of webhooks to register to receive notifications for certain events.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("webhooks")
  public List<@Valid WebhookModel> getWebhooks() {
    return webhooks;
  }

  public void setWebhooks(List<@Valid WebhookModel> webhooks) {
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
    return Objects.equals(this.connections, jobParameters.connections) &&
        Objects.equals(this.inputs, jobParameters.inputs) &&
        Objects.equals(this.label, jobParameters.label) &&
        Objects.equals(this.parentTaskExecutionId, jobParameters.parentTaskExecutionId) &&
        Objects.equals(this.priority, jobParameters.priority) &&
        Objects.equals(this.triggerOutputs, jobParameters.triggerOutputs) &&
        Objects.equals(this.workflowId, jobParameters.workflowId) &&
        Objects.equals(this.webhooks, jobParameters.webhooks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connections, inputs, label, parentTaskExecutionId, priority, triggerOutputs, workflowId, webhooks);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JobParametersModel {\n");
    sb.append("    connections: ").append(toIndentedString(connections)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    parentTaskExecutionId: ").append(toIndentedString(parentTaskExecutionId)).append("\n");
    sb.append("    priority: ").append(toIndentedString(priority)).append("\n");
    sb.append("    triggerOutputs: ").append(toIndentedString(triggerOutputs)).append("\n");
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

