package com.bytechef.platform.configuration.web.rest.model;

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
 * Contains test output of a workflow node.
 */

@Schema(name = "WorkflowNodeTestOutput", description = "Contains test output of a workflow node.")
@JsonTypeName("WorkflowNodeTestOutput")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-11-26T21:19:59.239958+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public class WorkflowNodeTestOutputModel {

  private Long id;

  private String workflowNodeName;

  private String workflowId;

  public WorkflowNodeTestOutputModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The workflow test node output id
   * @return id
   */
  
  @Schema(name = "id", description = "The workflow test node output id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public WorkflowNodeTestOutputModel workflowNodeName(String workflowNodeName) {
    this.workflowNodeName = workflowNodeName;
    return this;
  }

  /**
   * The workflow node name.
   * @return workflowNodeName
   */
  
  @Schema(name = "workflowNodeName", description = "The workflow node name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowNodeName")
  public String getWorkflowNodeName() {
    return workflowNodeName;
  }

  public void setWorkflowNodeName(String workflowNodeName) {
    this.workflowNodeName = workflowNodeName;
  }

  public WorkflowNodeTestOutputModel workflowId(String workflowId) {
    this.workflowId = workflowId;
    return this;
  }

  /**
   * The workflow id.
   * @return workflowId
   */
  
  @Schema(name = "workflowId", description = "The workflow id.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    WorkflowNodeTestOutputModel workflowNodeTestOutput = (WorkflowNodeTestOutputModel) o;
    return Objects.equals(this.id, workflowNodeTestOutput.id) &&
        Objects.equals(this.workflowNodeName, workflowNodeTestOutput.workflowNodeName) &&
        Objects.equals(this.workflowId, workflowNodeTestOutput.workflowId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, workflowNodeName, workflowId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowNodeTestOutputModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    workflowNodeName: ").append(toIndentedString(workflowNodeName)).append("\n");
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

