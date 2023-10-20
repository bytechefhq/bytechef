package com.bytechef.helios.project.web.rest.model;

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
 * Defines parameters used to execute a job.
 */

@Schema(name = "JobParameters", description = "Defines parameters used to execute a job.")
@JsonTypeName("JobParameters")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-30T10:07:11.328260+02:00[Europe/Zagreb]")
public class JobParametersModel {

  @JsonProperty("workflowId")
  private String workflowId;

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
  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
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
    return Objects.equals(this.workflowId, jobParameters.workflowId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workflowId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JobParametersModel {\n");
    sb.append("    workflowId: ").append(toIndentedString(workflowId)).append("\n");
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

