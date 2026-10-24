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
 * An element of the path from a top-level task execution to the task execution to focus.
 */

@Schema(name = "FocusPathElement", description = "An element of the path from a top-level task execution to the task execution to focus.")
@JsonTypeName("FocusPathElement")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-22T23:22:06.180830+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class FocusPathElementModel {

  private @Nullable Integer childIndex;

  private @Nullable Integer iterationIndex;

  private String taskExecutionId;

  public FocusPathElementModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public FocusPathElementModel(String taskExecutionId) {
    this.taskExecutionId = taskExecutionId;
  }

  public FocusPathElementModel childIndex(@Nullable Integer childIndex) {
    this.childIndex = childIndex;
    return this;
  }

  /**
   * The position of this task execution among the children of the previous element of the path, in the order the children endpoint returns them; absent for the first element and set only when the previous element is neither a loop nor an on-error task.
   * @return childIndex
   */
  
  @Schema(name = "childIndex", description = "The position of this task execution among the children of the previous element of the path, in the order the children endpoint returns them; absent for the first element and set only when the previous element is neither a loop nor an on-error task.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("childIndex")
  public @Nullable Integer getChildIndex() {
    return childIndex;
  }

  @JsonProperty("childIndex")
  public void setChildIndex(@Nullable Integer childIndex) {
    this.childIndex = childIndex;
  }

  public FocusPathElementModel iterationIndex(@Nullable Integer iterationIndex) {
    this.iterationIndex = iterationIndex;
    return this;
  }

  /**
   * The index of the loop iteration, or of the on-error branch (0 for the main branch, 1 for the error branch), of the previous element of the path that contains this task execution; absent for the first element and for children of other tasks.
   * @return iterationIndex
   */
  
  @Schema(name = "iterationIndex", description = "The index of the loop iteration, or of the on-error branch (0 for the main branch, 1 for the error branch), of the previous element of the path that contains this task execution; absent for the first element and for children of other tasks.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("iterationIndex")
  public @Nullable Integer getIterationIndex() {
    return iterationIndex;
  }

  @JsonProperty("iterationIndex")
  public void setIterationIndex(@Nullable Integer iterationIndex) {
    this.iterationIndex = iterationIndex;
  }

  public FocusPathElementModel taskExecutionId(String taskExecutionId) {
    this.taskExecutionId = taskExecutionId;
    return this;
  }

  /**
   * The id of a task execution.
   * @return taskExecutionId
   */
  @NotNull 
  @Schema(name = "taskExecutionId", description = "The id of a task execution.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("taskExecutionId")
  public String getTaskExecutionId() {
    return taskExecutionId;
  }

  @JsonProperty("taskExecutionId")
  public void setTaskExecutionId(String taskExecutionId) {
    this.taskExecutionId = taskExecutionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FocusPathElementModel focusPathElement = (FocusPathElementModel) o;
    return Objects.equals(this.childIndex, focusPathElement.childIndex) &&
        Objects.equals(this.iterationIndex, focusPathElement.iterationIndex) &&
        Objects.equals(this.taskExecutionId, focusPathElement.taskExecutionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(childIndex, iterationIndex, taskExecutionId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FocusPathElementModel {\n");
    sb.append("    childIndex: ").append(toIndentedString(childIndex)).append("\n");
    sb.append("    iterationIndex: ").append(toIndentedString(iterationIndex)).append("\n");
    sb.append("    taskExecutionId: ").append(toIndentedString(taskExecutionId)).append("\n");
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

