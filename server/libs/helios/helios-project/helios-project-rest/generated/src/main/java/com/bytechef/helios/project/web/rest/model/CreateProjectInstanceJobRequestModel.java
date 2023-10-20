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
 * CreateProjectInstanceJobRequestModel
 */

@JsonTypeName("createProjectInstanceJob_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-25T15:46:34.901574+02:00[Europe/Zagreb]")
public class CreateProjectInstanceJobRequestModel {

  private String workflowId;

  public CreateProjectInstanceJobRequestModel workflowId(String workflowId) {
    this.workflowId = workflowId;
    return this;
  }

  /**
   * The id of the workflow to execute.
   * @return workflowId
  */
  
  @Schema(name = "workflowId", description = "The id of the workflow to execute.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowId")
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
    CreateProjectInstanceJobRequestModel createProjectInstanceJobRequest = (CreateProjectInstanceJobRequestModel) o;
    return Objects.equals(this.workflowId, createProjectInstanceJobRequest.workflowId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workflowId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateProjectInstanceJobRequestModel {\n");
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

