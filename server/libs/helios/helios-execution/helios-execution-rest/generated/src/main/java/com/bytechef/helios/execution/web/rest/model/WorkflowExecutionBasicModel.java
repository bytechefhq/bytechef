package com.bytechef.helios.execution.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.helios.execution.web.rest.model.JobBasicModel;
import com.bytechef.helios.execution.web.rest.model.WorkflowBasicModel;
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
 * Contains basic information about execution of a project workflow.
 */

@Schema(name = "WorkflowExecutionBasic", description = "Contains basic information about execution of a project workflow.")
@JsonTypeName("WorkflowExecutionBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-09-21T12:30:39.139558+02:00[Europe/Zagreb]")
public class WorkflowExecutionBasicModel {

  private Long id;

  private com.bytechef.helios.configuration.web.rest.model.ProjectInstanceBasicModel instance;

  private JobBasicModel job;

  private com.bytechef.helios.configuration.web.rest.model.ProjectBasicModel project;

  private WorkflowBasicModel workflow;

  public WorkflowExecutionBasicModel id(Long id) {
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

  public WorkflowExecutionBasicModel instance(com.bytechef.helios.configuration.web.rest.model.ProjectInstanceBasicModel instance) {
    this.instance = instance;
    return this;
  }

  /**
   * Get instance
   * @return instance
  */
  @Valid 
  @Schema(name = "instance", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("instance")
  public com.bytechef.helios.configuration.web.rest.model.ProjectInstanceBasicModel getInstance() {
    return instance;
  }

  public void setInstance(com.bytechef.helios.configuration.web.rest.model.ProjectInstanceBasicModel instance) {
    this.instance = instance;
  }

  public WorkflowExecutionBasicModel job(JobBasicModel job) {
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
  public JobBasicModel getJob() {
    return job;
  }

  public void setJob(JobBasicModel job) {
    this.job = job;
  }

  public WorkflowExecutionBasicModel project(com.bytechef.helios.configuration.web.rest.model.ProjectBasicModel project) {
    this.project = project;
    return this;
  }

  /**
   * Get project
   * @return project
  */
  @Valid 
  @Schema(name = "project", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("project")
  public com.bytechef.helios.configuration.web.rest.model.ProjectBasicModel getProject() {
    return project;
  }

  public void setProject(com.bytechef.helios.configuration.web.rest.model.ProjectBasicModel project) {
    this.project = project;
  }

  public WorkflowExecutionBasicModel workflow(WorkflowBasicModel workflow) {
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
  public WorkflowBasicModel getWorkflow() {
    return workflow;
  }

  public void setWorkflow(WorkflowBasicModel workflow) {
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
    WorkflowExecutionBasicModel workflowExecutionBasic = (WorkflowExecutionBasicModel) o;
    return Objects.equals(this.id, workflowExecutionBasic.id) &&
        Objects.equals(this.instance, workflowExecutionBasic.instance) &&
        Objects.equals(this.job, workflowExecutionBasic.job) &&
        Objects.equals(this.project, workflowExecutionBasic.project) &&
        Objects.equals(this.workflow, workflowExecutionBasic.workflow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, instance, job, project, workflow);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowExecutionBasicModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    instance: ").append(toIndentedString(instance)).append("\n");
    sb.append("    job: ").append(toIndentedString(job)).append("\n");
    sb.append("    project: ").append(toIndentedString(project)).append("\n");
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

