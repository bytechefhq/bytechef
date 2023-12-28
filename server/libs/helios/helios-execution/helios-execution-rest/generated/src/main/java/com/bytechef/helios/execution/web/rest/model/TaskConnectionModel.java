package com.bytechef.helios.execution.web.rest.model;

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
 * The connection used in a particular task.
 */

@Schema(name = "TaskConnection", description = "The connection used in a particular task.")
@JsonTypeName("TaskConnection")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-12-28T18:24:22.622648+01:00[Europe/Zagreb]")
public class TaskConnectionModel {

  private Long id;

  private String key;

  private String taskName;

  public TaskConnectionModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The connection id
   * @return id
  */
  
  @Schema(name = "id", description = "The connection id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public TaskConnectionModel key(String key) {
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

  public TaskConnectionModel taskName(String taskName) {
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
    TaskConnectionModel taskConnection = (TaskConnectionModel) o;
    return Objects.equals(this.id, taskConnection.id) &&
        Objects.equals(this.key, taskConnection.key) &&
        Objects.equals(this.taskName, taskConnection.taskName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, key, taskName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TaskConnectionModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
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

