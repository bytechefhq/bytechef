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
 * The connection used in particular task.
 */

@Schema(name = "ProjectInstanceWorkflowConnection", description = "The connection used in particular task.")
@JsonTypeName("ProjectInstanceWorkflowConnection")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-04T12:16:52.208086+02:00[Europe/Zagreb]")
public class ProjectInstanceWorkflowConnectionModel {

  private Long connectionId;

  private String key;

  private String taskName;

  public ProjectInstanceWorkflowConnectionModel connectionId(Long connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  /**
   * The connection id
   * @return connectionId
  */
  
  @Schema(name = "connectionId", description = "The connection id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionId")
  public Long getConnectionId() {
    return connectionId;
  }

  public void setConnectionId(Long connectionId) {
    this.connectionId = connectionId;
  }

  public ProjectInstanceWorkflowConnectionModel key(String key) {
    this.key = key;
    return this;
  }

  /**
   * The connection key under which a connection is defined in a workflow definition.
   * @return key
  */
  
  @Schema(name = "key", description = "The connection key under which a connection is defined in a workflow definition.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("key")
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public ProjectInstanceWorkflowConnectionModel taskName(String taskName) {
    this.taskName = taskName;
    return this;
  }

  /**
   * The task name to which a connection belongs.
   * @return taskName
  */
  
  @Schema(name = "taskName", description = "The task name to which a connection belongs.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("taskName")
  public String getTaskName() {
    return taskName;
  }

  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectInstanceWorkflowConnectionModel projectInstanceWorkflowConnection = (ProjectInstanceWorkflowConnectionModel) o;
    return Objects.equals(this.connectionId, projectInstanceWorkflowConnection.connectionId) &&
        Objects.equals(this.key, projectInstanceWorkflowConnection.key) &&
        Objects.equals(this.taskName, projectInstanceWorkflowConnection.taskName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionId, key, taskName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectInstanceWorkflowConnectionModel {\n");
    sb.append("    connectionId: ").append(toIndentedString(connectionId)).append("\n");
    sb.append("    key: ").append(toIndentedString(key)).append("\n");
    sb.append("    taskName: ").append(toIndentedString(taskName)).append("\n");
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

