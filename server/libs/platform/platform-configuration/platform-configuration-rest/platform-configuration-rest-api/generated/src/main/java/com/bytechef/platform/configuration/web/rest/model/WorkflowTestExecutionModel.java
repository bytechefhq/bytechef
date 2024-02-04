package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.JobModel;
import com.bytechef.platform.configuration.web.rest.model.TriggerExecutionModel;
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
 * Contains information about test execution of a workflow.
 */

@Schema(name = "WorkflowTestExecution", description = "Contains information about test execution of a workflow.")
@JsonTypeName("WorkflowTestExecution")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-02-04T12:40:35.730095+01:00[Europe/Zagreb]")
public class WorkflowTestExecutionModel {

  private JobModel job;

  private TriggerExecutionModel triggerExecution;

  public WorkflowTestExecutionModel job(JobModel job) {
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

  public WorkflowTestExecutionModel triggerExecution(TriggerExecutionModel triggerExecution) {
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
  public TriggerExecutionModel getTriggerExecution() {
    return triggerExecution;
  }

  public void setTriggerExecution(TriggerExecutionModel triggerExecution) {
    this.triggerExecution = triggerExecution;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkflowTestExecutionModel workflowTestExecution = (WorkflowTestExecutionModel) o;
    return Objects.equals(this.job, workflowTestExecution.job) &&
        Objects.equals(this.triggerExecution, workflowTestExecution.triggerExecution);
  }

  @Override
  public int hashCode() {
    return Objects.hash(job, triggerExecution);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowTestExecutionModel {\n");
    sb.append("    job: ").append(toIndentedString(job)).append("\n");
    sb.append("    triggerExecution: ").append(toIndentedString(triggerExecution)).append("\n");
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

