package com.bytechef.atlas.web.rest.model;

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
 * PostJob200ResponseModel
 */

@JsonTypeName("postJob_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-01T09:06:53.695754+01:00[Europe/Zagreb]")
public class PostJob200ResponseModel {

  @JsonProperty("jobId")
  private Long jobId;

  public PostJob200ResponseModel jobId(Long jobId) {
    this.jobId = jobId;
    return this;
  }

  /**
   * Get jobId
   * @return jobId
  */
  
  @Schema(name = "jobId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    PostJob200ResponseModel postJob200Response = (PostJob200ResponseModel) o;
    return Objects.equals(this.jobId, postJob200Response.jobId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostJob200ResponseModel {\n");
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

