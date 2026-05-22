package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ConnectedUserProjectWorkflowModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A connected user project with its workflows.
 */

@Schema(name = "Project", description = "A connected user project with its workflows.")
@JsonTypeName("Project")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-05-22T10:23:02.305542+02:00[Europe/Zagreb]", comments = "Generator version: 7.21.0")
public class ProjectModel {

  private @Nullable Long id;

  private @Nullable Long projectId;

  @Valid
  private List<@Valid ConnectedUserProjectWorkflowModel> workflows = new ArrayList<>();

  public ProjectModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of the connected user project.
   * @return id
   */
  
  @Schema(name = "id", description = "The id of the connected user project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public ProjectModel projectId(@Nullable Long projectId) {
    this.projectId = projectId;
    return this;
  }

  /**
   * The id of the underlying project.
   * @return projectId
   */
  
  @Schema(name = "projectId", description = "The id of the underlying project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectId")
  public @Nullable Long getProjectId() {
    return projectId;
  }

  @JsonProperty("projectId")
  public void setProjectId(@Nullable Long projectId) {
    this.projectId = projectId;
  }

  public ProjectModel workflows(List<@Valid ConnectedUserProjectWorkflowModel> workflows) {
    this.workflows = workflows;
    return this;
  }

  public ProjectModel addWorkflowsItem(ConnectedUserProjectWorkflowModel workflowsItem) {
    if (this.workflows == null) {
      this.workflows = new ArrayList<>();
    }
    this.workflows.add(workflowsItem);
    return this;
  }

  /**
   * The list of workflows belonging to this project.
   * @return workflows
   */
  @Valid 
  @Schema(name = "workflows", description = "The list of workflows belonging to this project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflows")
  public List<@Valid ConnectedUserProjectWorkflowModel> getWorkflows() {
    return workflows;
  }

  @JsonProperty("workflows")
  public void setWorkflows(List<@Valid ConnectedUserProjectWorkflowModel> workflows) {
    this.workflows = workflows;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectModel project = (ProjectModel) o;
    return Objects.equals(this.id, project.id) &&
        Objects.equals(this.projectId, project.projectId) &&
        Objects.equals(this.workflows, project.workflows);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, projectId, workflows);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    workflows: ").append(toIndentedString(workflows)).append("\n");
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

