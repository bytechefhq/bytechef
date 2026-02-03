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
 * CreateProjectWorkflow200ResponseModel
 */

@JsonTypeName("createProjectWorkflow_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-03T18:13:00.110740+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public class CreateProjectWorkflow200ResponseModel {

  private @Nullable Long projectWorkflowId;

  public CreateProjectWorkflow200ResponseModel projectWorkflowId(@Nullable Long projectWorkflowId) {
    this.projectWorkflowId = projectWorkflowId;
    return this;
  }

  /**
   * The id of a created project workflow.
   * @return projectWorkflowId
   */
  
  @Schema(name = "projectWorkflowId", description = "The id of a created project workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectWorkflowId")
  public @Nullable Long getProjectWorkflowId() {
    return projectWorkflowId;
  }

  public void setProjectWorkflowId(@Nullable Long projectWorkflowId) {
    this.projectWorkflowId = projectWorkflowId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateProjectWorkflow200ResponseModel createProjectWorkflow200Response = (CreateProjectWorkflow200ResponseModel) o;
    return Objects.equals(this.projectWorkflowId, createProjectWorkflow200Response.projectWorkflowId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectWorkflowId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateProjectWorkflow200ResponseModel {\n");
    sb.append("    projectWorkflowId: ").append(toIndentedString(projectWorkflowId)).append("\n");
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

