package com.bytechef.ee.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CreateIntegrationInstanceConfigurationWorkflowJob200ResponseModel
 */

@JsonTypeName("createIntegrationInstanceConfigurationWorkflowJob_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:35.556332+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class CreateIntegrationInstanceConfigurationWorkflowJob200ResponseModel {

  private @Nullable Long jobId;

  public CreateIntegrationInstanceConfigurationWorkflowJob200ResponseModel jobId(@Nullable Long jobId) {
    this.jobId = jobId;
    return this;
  }

  /**
   * The id of an executed job.
   * @return jobId
   */
  
  @Schema(name = "jobId", description = "The id of an executed job.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("jobId")
  public @Nullable Long getJobId() {
    return jobId;
  }

  public void setJobId(@Nullable Long jobId) {
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
    CreateIntegrationInstanceConfigurationWorkflowJob200ResponseModel createIntegrationInstanceConfigurationWorkflowJob200Response = (CreateIntegrationInstanceConfigurationWorkflowJob200ResponseModel) o;
    return Objects.equals(this.jobId, createIntegrationInstanceConfigurationWorkflowJob200Response.jobId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateIntegrationInstanceConfigurationWorkflowJob200ResponseModel {\n");
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

