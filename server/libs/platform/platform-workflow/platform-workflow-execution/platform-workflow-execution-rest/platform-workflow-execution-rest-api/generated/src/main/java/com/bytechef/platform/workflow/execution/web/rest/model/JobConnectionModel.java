package com.bytechef.platform.workflow.execution.web.rest.model;

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
 * The connection used in a particular task.
 */

@Schema(name = "JobConnection", description = "The connection used in a particular task.")
@JsonTypeName("JobConnection")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-23T12:37:00.511898+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class JobConnectionModel {

  private @Nullable Long id;

  private @Nullable String key;

  private @Nullable String taskName;

  public JobConnectionModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The connection id
   * @return id
   */
  
  @Schema(name = "id", description = "The connection id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public JobConnectionModel key(@Nullable String key) {
    this.key = key;
    return this;
  }

  /**
   * The connection key under which a connection is defined in a workflow definition.
   * @return key
   */
  
  @Schema(name = "key", description = "The connection key under which a connection is defined in a workflow definition.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("key")
  public @Nullable String getKey() {
    return key;
  }

  public void setKey(@Nullable String key) {
    this.key = key;
  }

  public JobConnectionModel taskName(@Nullable String taskName) {
    this.taskName = taskName;
    return this;
  }

  /**
   * The task name to which a connection belongs.
   * @return taskName
   */
  
  @Schema(name = "taskName", description = "The task name to which a connection belongs.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("taskName")
  public @Nullable String getTaskName() {
    return taskName;
  }

  public void setTaskName(@Nullable String taskName) {
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
    JobConnectionModel jobConnection = (JobConnectionModel) o;
    return Objects.equals(this.id, jobConnection.id) &&
        Objects.equals(this.key, jobConnection.key) &&
        Objects.equals(this.taskName, jobConnection.taskName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, key, taskName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JobConnectionModel {\n");
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

