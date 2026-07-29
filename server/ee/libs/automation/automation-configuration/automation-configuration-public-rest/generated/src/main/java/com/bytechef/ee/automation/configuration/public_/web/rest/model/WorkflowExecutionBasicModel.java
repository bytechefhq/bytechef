package com.bytechef.ee.automation.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.ExecutionErrorModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.ProjectDeploymentReferenceModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.ProjectReferenceModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.WorkflowExecutionStatusModel;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.WorkflowReferenceModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A workflow execution list row. Never carries execution data - fetch the execution by id for inputs, outputs and task executions.
 */

@Schema(name = "WorkflowExecutionBasic", description = "A workflow execution list row. Never carries execution data - fetch the execution by id for inputs, outputs and task executions.")
@JsonTypeName("WorkflowExecutionBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-18T16:40:02.539074685Z[Etc/UTC]", comments = "Generator version: 7.22.0")
public class WorkflowExecutionBasicModel {

  private Long id;

  private @Nullable WorkflowExecutionStatusModel status;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime startDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime endDate;

  private @Nullable ExecutionErrorModel error;

  private @Nullable ProjectReferenceModel project;

  private @Nullable ProjectDeploymentReferenceModel projectDeployment;

  private @Nullable WorkflowReferenceModel workflow;

  public WorkflowExecutionBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WorkflowExecutionBasicModel(Long id) {
    this.id = id;
  }

  public WorkflowExecutionBasicModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a workflow execution.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a workflow execution.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(Long id) {
    this.id = id;
  }

  public WorkflowExecutionBasicModel status(@Nullable WorkflowExecutionStatusModel status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @Valid 
  @Schema(name = "status", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("status")
  public @Nullable WorkflowExecutionStatusModel getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable WorkflowExecutionStatusModel status) {
    this.status = status;
  }

  public WorkflowExecutionBasicModel startDate(@Nullable OffsetDateTime startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * The instant an execution started.
   * @return startDate
   */
  @Valid 
  @Schema(name = "startDate", description = "The instant an execution started.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("startDate")
  public @Nullable OffsetDateTime getStartDate() {
    return startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(@Nullable OffsetDateTime startDate) {
    this.startDate = startDate;
  }

  public WorkflowExecutionBasicModel endDate(@Nullable OffsetDateTime endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * The instant an execution ended. Null while the execution is running.
   * @return endDate
   */
  @Valid 
  @Schema(name = "endDate", description = "The instant an execution ended. Null while the execution is running.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("endDate")
  public @Nullable OffsetDateTime getEndDate() {
    return endDate;
  }

  @JsonProperty("endDate")
  public void setEndDate(@Nullable OffsetDateTime endDate) {
    this.endDate = endDate;
  }

  public WorkflowExecutionBasicModel error(@Nullable ExecutionErrorModel error) {
    this.error = error;
    return this;
  }

  /**
   * Get error
   * @return error
   */
  @Valid 
  @Schema(name = "error", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("error")
  public @Nullable ExecutionErrorModel getError() {
    return error;
  }

  @JsonProperty("error")
  public void setError(@Nullable ExecutionErrorModel error) {
    this.error = error;
  }

  public WorkflowExecutionBasicModel project(@Nullable ProjectReferenceModel project) {
    this.project = project;
    return this;
  }

  /**
   * Get project
   * @return project
   */
  @Valid 
  @Schema(name = "project", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("project")
  public @Nullable ProjectReferenceModel getProject() {
    return project;
  }

  @JsonProperty("project")
  public void setProject(@Nullable ProjectReferenceModel project) {
    this.project = project;
  }

  public WorkflowExecutionBasicModel projectDeployment(@Nullable ProjectDeploymentReferenceModel projectDeployment) {
    this.projectDeployment = projectDeployment;
    return this;
  }

  /**
   * Get projectDeployment
   * @return projectDeployment
   */
  @Valid 
  @Schema(name = "projectDeployment", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectDeployment")
  public @Nullable ProjectDeploymentReferenceModel getProjectDeployment() {
    return projectDeployment;
  }

  @JsonProperty("projectDeployment")
  public void setProjectDeployment(@Nullable ProjectDeploymentReferenceModel projectDeployment) {
    this.projectDeployment = projectDeployment;
  }

  public WorkflowExecutionBasicModel workflow(@Nullable WorkflowReferenceModel workflow) {
    this.workflow = workflow;
    return this;
  }

  /**
   * Get workflow
   * @return workflow
   */
  @Valid 
  @Schema(name = "workflow", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflow")
  public @Nullable WorkflowReferenceModel getWorkflow() {
    return workflow;
  }

  @JsonProperty("workflow")
  public void setWorkflow(@Nullable WorkflowReferenceModel workflow) {
    this.workflow = workflow;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkflowExecutionBasicModel workflowExecutionBasic = (WorkflowExecutionBasicModel) o;
    return Objects.equals(this.id, workflowExecutionBasic.id) &&
        Objects.equals(this.status, workflowExecutionBasic.status) &&
        Objects.equals(this.startDate, workflowExecutionBasic.startDate) &&
        Objects.equals(this.endDate, workflowExecutionBasic.endDate) &&
        Objects.equals(this.error, workflowExecutionBasic.error) &&
        Objects.equals(this.project, workflowExecutionBasic.project) &&
        Objects.equals(this.projectDeployment, workflowExecutionBasic.projectDeployment) &&
        Objects.equals(this.workflow, workflowExecutionBasic.workflow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, status, startDate, endDate, error, project, projectDeployment, workflow);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowExecutionBasicModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    project: ").append(toIndentedString(project)).append("\n");
    sb.append("    projectDeployment: ").append(toIndentedString(projectDeployment)).append("\n");
    sb.append("    workflow: ").append(toIndentedString(workflow)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

