package com.bytechef.hermwes.workflow.web.rest.model;

import java.net.URI;
import java.util.Objects;
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
 * WorkflowTestResponseModel
 */

@JsonTypeName("WorkflowTestResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-31T13:38:51.963850+02:00[Europe/Zagreb]")
public class WorkflowTestResponseModel {

  @JsonProperty("job")
  private com.bytechef.atlas.web.rest.model.JobModel job;

  @JsonProperty("taskExecutions")
  @Valid
  private List<com.bytechef.atlas.web.rest.model.TaskExecutionModel> taskExecutions = null;

  public WorkflowTestResponseModel job(com.bytechef.atlas.web.rest.model.JobModel job) {
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

  public WorkflowTestResponseModel taskExecutions(List<com.bytechef.atlas.web.rest.model.TaskExecutionModel> taskExecutions) {
    this.taskExecutions = taskExecutions;
    return this;
  }

  public WorkflowTestResponseModel addTaskExecutionsItem(com.bytechef.atlas.web.rest.model.TaskExecutionModel taskExecutionsItem) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkflowTestResponseModel workflowTestResponse = (WorkflowTestResponseModel) o;
    return Objects.equals(this.job, workflowTestResponse.job) &&
        Objects.equals(this.taskExecutions, workflowTestResponse.taskExecutions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(job, taskExecutions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowTestResponseModel {\n");
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

