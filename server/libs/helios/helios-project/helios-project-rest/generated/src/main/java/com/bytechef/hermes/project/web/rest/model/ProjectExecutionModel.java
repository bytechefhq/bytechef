package com.bytechef.hermes.project.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.project.web.rest.model.ProjectInstanceModel;
import com.bytechef.hermes.project.web.rest.model.ProjectModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Contains information about execution of one of project workflows.
 */

@Schema(name = "ProjectExecution", description = "Contains information about execution of one of project workflows.")
@JsonTypeName("ProjectExecution")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-08T07:37:11.195591+01:00[Europe/Zagreb]")
public class ProjectExecutionModel {

  @JsonProperty("instance")
  private ProjectInstanceModel instance;

  @JsonProperty("job")
  private com.bytechef.atlas.web.rest.model.JobModel job;

  @JsonProperty("project")
  private ProjectModel project;

  @JsonProperty("taskExecutions")
  @Valid
  private List<com.bytechef.atlas.web.rest.model.TaskExecutionModel> taskExecutions = null;

  @JsonProperty("workflow")
  private com.bytechef.atlas.web.rest.model.WorkflowModel workflow;

  public ProjectExecutionModel instance(ProjectInstanceModel instance) {
    this.instance = instance;
    return this;
  }

  /**
   * Get instance
   * @return instance
  */
  @Valid 
  @Schema(name = "instance", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public ProjectInstanceModel getInstance() {
    return instance;
  }

  public void setInstance(ProjectInstanceModel instance) {
    this.instance = instance;
  }

  public ProjectExecutionModel job(com.bytechef.atlas.web.rest.model.JobModel job) {
    this.job = job;
    return this;
  }

  /**
   * Get job
   * @return job
  */
  @Valid 
  @Schema(name = "job", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public com.bytechef.atlas.web.rest.model.JobModel getJob() {
    return job;
  }

  public void setJob(com.bytechef.atlas.web.rest.model.JobModel job) {
    this.job = job;
  }

  public ProjectExecutionModel project(ProjectModel project) {
    this.project = project;
    return this;
  }

  /**
   * Get project
   * @return project
  */
  @Valid 
  @Schema(name = "project", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public ProjectModel getProject() {
    return project;
  }

  public void setProject(ProjectModel project) {
    this.project = project;
  }

  public ProjectExecutionModel taskExecutions(List<com.bytechef.atlas.web.rest.model.TaskExecutionModel> taskExecutions) {
    this.taskExecutions = taskExecutions;
    return this;
  }

  public ProjectExecutionModel addTaskExecutionsItem(com.bytechef.atlas.web.rest.model.TaskExecutionModel taskExecutionsItem) {
    if (this.taskExecutions == null) {
      this.taskExecutions = new ArrayList<>();
    }
    this.taskExecutions.add(taskExecutionsItem);
    return this;
  }

  /**
   * Get taskExecutions
   * @return taskExecutions
  */
  @Valid 
  @Schema(name = "taskExecutions", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public List<com.bytechef.atlas.web.rest.model.TaskExecutionModel> getTaskExecutions() {
    return taskExecutions;
  }

  public void setTaskExecutions(List<com.bytechef.atlas.web.rest.model.TaskExecutionModel> taskExecutions) {
    this.taskExecutions = taskExecutions;
  }

  public ProjectExecutionModel workflow(com.bytechef.atlas.web.rest.model.WorkflowModel workflow) {
    this.workflow = workflow;
    return this;
  }

  /**
   * Get workflow
   * @return workflow
  */
  @Valid 
  @Schema(name = "workflow", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public com.bytechef.atlas.web.rest.model.WorkflowModel getWorkflow() {
    return workflow;
  }

  public void setWorkflow(com.bytechef.atlas.web.rest.model.WorkflowModel workflow) {
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
    ProjectExecutionModel projectExecution = (ProjectExecutionModel) o;
    return Objects.equals(this.instance, projectExecution.instance) &&
        Objects.equals(this.job, projectExecution.job) &&
        Objects.equals(this.project, projectExecution.project) &&
        Objects.equals(this.taskExecutions, projectExecution.taskExecutions) &&
        Objects.equals(this.workflow, projectExecution.workflow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(instance, job, project, taskExecutions, workflow);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectExecutionModel {\n");
    sb.append("    instance: ").append(toIndentedString(instance)).append("\n");
    sb.append("    job: ").append(toIndentedString(job)).append("\n");
    sb.append("    project: ").append(toIndentedString(project)).append("\n");
    sb.append("    taskExecutions: ").append(toIndentedString(taskExecutions)).append("\n");
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

