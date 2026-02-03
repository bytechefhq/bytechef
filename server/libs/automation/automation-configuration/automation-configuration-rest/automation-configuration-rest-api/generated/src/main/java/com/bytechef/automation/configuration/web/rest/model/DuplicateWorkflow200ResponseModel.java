package com.bytechef.automation.configuration.web.rest.model;

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
 * DuplicateWorkflow200ResponseModel
 */

@JsonTypeName("duplicateWorkflow_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-03T18:13:00.110740+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public class DuplicateWorkflow200ResponseModel {

  private @Nullable String workflowId;

  public DuplicateWorkflow200ResponseModel workflowId(@Nullable String workflowId) {
    this.workflowId = workflowId;
    return this;
  }

  /**
   * The id of a new duplicated workflow.
   * @return workflowId
   */
  
  @Schema(name = "workflowId", description = "The id of a new duplicated workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowId")
  public @Nullable String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(@Nullable String workflowId) {
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
    DuplicateWorkflow200ResponseModel duplicateWorkflow200Response = (DuplicateWorkflow200ResponseModel) o;
    return Objects.equals(this.workflowId, duplicateWorkflow200Response.workflowId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workflowId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DuplicateWorkflow200ResponseModel {\n");
    sb.append("    workflowId: ").append(toIndentedString(workflowId)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

