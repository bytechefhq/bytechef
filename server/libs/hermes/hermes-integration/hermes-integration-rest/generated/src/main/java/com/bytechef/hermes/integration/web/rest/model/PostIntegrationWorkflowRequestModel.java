package com.bytechef.hermes.integration.web.rest.model;

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
 * PostIntegrationWorkflowRequestModel
 */

@JsonTypeName("postIntegrationWorkflow_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-01-10T12:28:55.699686+01:00[Europe/Zagreb]")
public class PostIntegrationWorkflowRequestModel {

  @JsonProperty("workflowName")
  private String workflowName;

  public PostIntegrationWorkflowRequestModel workflowName(String workflowName) {
    this.workflowName = workflowName;
    return this;
  }

  /**
   * Get workflowName
   * @return workflowName
  */
  @NotNull 
  @Schema(name = "workflowName", required = true)
  public String getWorkflowName() {
    return workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PostIntegrationWorkflowRequestModel postIntegrationWorkflowRequest = (PostIntegrationWorkflowRequestModel) o;
    return Objects.equals(this.workflowName, postIntegrationWorkflowRequest.workflowName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workflowName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostIntegrationWorkflowRequestModel {\n");
    sb.append("    workflowName: ").append(toIndentedString(workflowName)).append("\n");
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

