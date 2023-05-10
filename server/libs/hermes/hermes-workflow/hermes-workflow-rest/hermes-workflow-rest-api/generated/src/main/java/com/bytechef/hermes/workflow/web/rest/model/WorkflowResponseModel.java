package com.bytechef.hermes.workflow.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.workflow.web.rest.model.JobModel;
import com.bytechef.hermes.workflow.web.rest.model.TaskExecutionModel;
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
 * WorkflowResponseModel
 */

@JsonTypeName("WorkflowResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-10T20:32:26.667648+02:00[Europe/Zagreb]")
public class WorkflowResponseModel {

  private JobModel job;

  @Valid
  private List<@Valid TaskExecutionModel> taskExecutions;

  public WorkflowResponseModel job(JobModel job) {
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
  public JobModel getJob() {
    return job;
  }

  public void setJob(JobModel job) {
    this.job = job;
  }

  public WorkflowResponseModel taskExecutions(List<@Valid TaskExecutionModel> taskExecutions) {
    this.taskExecutions = taskExecutions;
    return this;
  }

  public WorkflowResponseModel addTaskExecutionsItem(TaskExecutionModel taskExecutionsItem) {
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
  @JsonProperty("taskExecutions")
  public List<@Valid TaskExecutionModel> getTaskExecutions() {
    return taskExecutions;
  }

  public void setTaskExecutions(List<@Valid TaskExecutionModel> taskExecutions) {
    this.taskExecutions = taskExecutions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkflowResponseModel workflowResponse = (WorkflowResponseModel) o;
    return Objects.equals(this.job, workflowResponse.job) &&
        Objects.equals(this.taskExecutions, workflowResponse.taskExecutions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(job, taskExecutions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowResponseModel {\n");
    sb.append("    job: ").append(toIndentedString(job)).append("\n");
    sb.append("    taskExecutions: ").append(toIndentedString(taskExecutions)).append("\n");
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

