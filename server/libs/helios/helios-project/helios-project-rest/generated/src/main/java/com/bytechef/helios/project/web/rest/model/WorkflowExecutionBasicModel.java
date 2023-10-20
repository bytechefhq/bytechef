package com.bytechef.helios.project.web.rest.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.media.Schema;


import jakarta.annotation.Generated;

/**
 * Contains information about execution of one of project workflows.
 */

@Schema(name = "WorkflowExecutionBasic", description = "Contains information about execution of one of project workflows.")
@JsonTypeName("WorkflowExecutionBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-04T12:27:26.489882+02:00[Europe/Zagreb]")
public class WorkflowExecutionBasicModel {

  private Long id;

  private ProjectInstanceBasicModel instance;

  private JobBasicModel job;

  private ProjectBasicModel project;

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

  public WorkflowExecutionBasicModel instance(ProjectInstanceBasicModel instance) {
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
  public ProjectInstanceBasicModel getInstance() {
    return instance;
  }

  public void setInstance(ProjectInstanceBasicModel instance) {
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

  public WorkflowExecutionBasicModel project(ProjectBasicModel project) {
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
  public ProjectBasicModel getProject() {
    return project;
  }

  public void setProject(ProjectBasicModel project) {
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
    WorkflowExecutionBasicModel WorkflowExecutionBasic = (WorkflowExecutionBasicModel) o;
    return Objects.equals(this.id, WorkflowExecutionBasic.id) &&
        Objects.equals(this.instance, WorkflowExecutionBasic.instance) &&
        Objects.equals(this.job, WorkflowExecutionBasic.job) &&
        Objects.equals(this.project, WorkflowExecutionBasic.project) &&
        Objects.equals(this.workflow, WorkflowExecutionBasic.workflow);
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

