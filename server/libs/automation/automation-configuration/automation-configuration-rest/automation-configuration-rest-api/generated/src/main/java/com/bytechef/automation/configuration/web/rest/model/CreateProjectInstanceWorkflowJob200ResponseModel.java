package com.bytechef.automation.configuration.web.rest.model;

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
 * CreateProjectInstanceWorkflowJob200ResponseModel
 */

@JsonTypeName("createProjectInstanceWorkflowJob_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-10T12:18:14.007003+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public class CreateProjectInstanceWorkflowJob200ResponseModel {

  private Long jobId;

  public CreateProjectInstanceWorkflowJob200ResponseModel jobId(Long jobId) {
    this.jobId = jobId;
    return this;
  }

  /**
   * The id of an executed job.
   * @return jobId
  */
  
  @Schema(name = "jobId", description = "The id of an executed job.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("jobId")
  public Long getJobId() {
    return jobId;
  }

  public void setJobId(Long jobId) {
    this.jobId = jobId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateProjectInstanceWorkflowJob200ResponseModel createProjectInstanceWorkflowJob200Response = (CreateProjectInstanceWorkflowJob200ResponseModel) o;
    return Objects.equals(this.jobId, createProjectInstanceWorkflowJob200Response.jobId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateProjectInstanceWorkflowJob200ResponseModel {\n");
    sb.append("    jobId: ").append(toIndentedString(jobId)).append("\n");
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

