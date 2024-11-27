package com.bytechef.embedded.workflow.execution.web.rest.model;

import java.net.URI;
import java.util.Objects;
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
 * Contains information about execution of a Integration workflow.
 */

@Schema(name = "WorkflowExecution", description = "Contains information about execution of a Integration workflow.")
@JsonTypeName("WorkflowExecution")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-11-26T21:19:58.983340+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public class WorkflowExecutionModel {

  private Long id;

  private com.bytechef.embedded.configuration.web.rest.model.IntegrationBasicModel integration;

  private com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationModel integrationInstanceConfiguration;

  private com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceBasicModel integrationInstance;

  private com.bytechef.platform.workflow.execution.web.rest.model.JobModel job;

  private com.bytechef.platform.workflow.execution.web.rest.model.TriggerExecutionModel triggerExecution;

  private com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel workflow;

  public WorkflowExecutionModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a workflow execution.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a workflow execution.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public WorkflowExecutionModel integration(com.bytechef.embedded.configuration.web.rest.model.IntegrationBasicModel integration) {
    this.integration = integration;
    return this;
  }

  /**
   * Get integration
   * @return integration
   */
  @Valid 
  @Schema(name = "integration", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integration")
  public com.bytechef.embedded.configuration.web.rest.model.IntegrationBasicModel getIntegration() {
    return integration;
  }

  public void setIntegration(com.bytechef.embedded.configuration.web.rest.model.IntegrationBasicModel integration) {
    this.integration = integration;
  }

  public WorkflowExecutionModel integrationInstanceConfiguration(com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationModel integrationInstanceConfiguration) {
    this.integrationInstanceConfiguration = integrationInstanceConfiguration;
    return this;
  }

  /**
   * Get integrationInstanceConfiguration
   * @return integrationInstanceConfiguration
   */
  @Valid 
  @Schema(name = "integrationInstanceConfiguration", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationInstanceConfiguration")
  public com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationModel getIntegrationInstanceConfiguration() {
    return integrationInstanceConfiguration;
  }

  public void setIntegrationInstanceConfiguration(com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationModel integrationInstanceConfiguration) {
    this.integrationInstanceConfiguration = integrationInstanceConfiguration;
  }

  public WorkflowExecutionModel integrationInstance(com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceBasicModel integrationInstance) {
    this.integrationInstance = integrationInstance;
    return this;
  }

  /**
   * Get integrationInstance
   * @return integrationInstance
   */
  @Valid 
  @Schema(name = "integrationInstance", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationInstance")
  public com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceBasicModel getIntegrationInstance() {
    return integrationInstance;
  }

  public void setIntegrationInstance(com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceBasicModel integrationInstance) {
    this.integrationInstance = integrationInstance;
  }

  public WorkflowExecutionModel job(com.bytechef.platform.workflow.execution.web.rest.model.JobModel job) {
    this.job = job;
    return this;
  }

  /**
   * Get job
   * @return job
   */
  @Valid 
  @Schema(name = "job", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("job")
  public com.bytechef.platform.workflow.execution.web.rest.model.JobModel getJob() {
    return job;
  }

  public void setJob(com.bytechef.platform.workflow.execution.web.rest.model.JobModel job) {
    this.job = job;
  }

  public WorkflowExecutionModel triggerExecution(com.bytechef.platform.workflow.execution.web.rest.model.TriggerExecutionModel triggerExecution) {
    this.triggerExecution = triggerExecution;
    return this;
  }

  /**
   * Get triggerExecution
   * @return triggerExecution
   */
  @Valid 
  @Schema(name = "triggerExecution", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("triggerExecution")
  public com.bytechef.platform.workflow.execution.web.rest.model.TriggerExecutionModel getTriggerExecution() {
    return triggerExecution;
  }

  public void setTriggerExecution(com.bytechef.platform.workflow.execution.web.rest.model.TriggerExecutionModel triggerExecution) {
    this.triggerExecution = triggerExecution;
  }

  public WorkflowExecutionModel workflow(com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel workflow) {
    this.workflow = workflow;
    return this;
  }

  /**
   * Get workflow
   * @return workflow
   */
  @Valid 
  @Schema(name = "workflow", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflow")
  public com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel getWorkflow() {
    return workflow;
  }

  public void setWorkflow(com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel workflow) {
    this.workflow = workflow;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkflowExecutionModel workflowExecution = (WorkflowExecutionModel) o;
    return Objects.equals(this.id, workflowExecution.id) &&
        Objects.equals(this.integration, workflowExecution.integration) &&
        Objects.equals(this.integrationInstanceConfiguration, workflowExecution.integrationInstanceConfiguration) &&
        Objects.equals(this.integrationInstance, workflowExecution.integrationInstance) &&
        Objects.equals(this.job, workflowExecution.job) &&
        Objects.equals(this.triggerExecution, workflowExecution.triggerExecution) &&
        Objects.equals(this.workflow, workflowExecution.workflow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, integration, integrationInstanceConfiguration, integrationInstance, job, triggerExecution, workflow);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowExecutionModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    integration: ").append(toIndentedString(integration)).append("\n");
    sb.append("    integrationInstanceConfiguration: ").append(toIndentedString(integrationInstanceConfiguration)).append("\n");
    sb.append("    integrationInstance: ").append(toIndentedString(integrationInstance)).append("\n");
    sb.append("    job: ").append(toIndentedString(job)).append("\n");
    sb.append("    triggerExecution: ").append(toIndentedString(triggerExecution)).append("\n");
    sb.append("    workflow: ").append(toIndentedString(workflow)).append("\n");
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

