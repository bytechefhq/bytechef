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
 * An element of the path from the root task executions to the task execution to focus.
 */

@Schema(name = "FocusPathElement", description = "An element of the path from the root task executions to the task execution to focus.")
@JsonTypeName("FocusPathElement")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-22T09:01:00.753919+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class FocusPathElementModel {

  private @Nullable Integer iterationIndex;

  private Long taskExecutionId;

  public FocusPathElementModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public FocusPathElementModel(Long taskExecutionId) {
    this.taskExecutionId = taskExecutionId;
  }

  public FocusPathElementModel iterationIndex(@Nullable Integer iterationIndex) {
    this.iterationIndex = iterationIndex;
    return this;
  }

  /**
   * The index of the loop iteration that contains the next element of the path.
   * @return iterationIndex
   */
  
  @Schema(name = "iterationIndex", description = "The index of the loop iteration that contains the next element of the path.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("iterationIndex")
  public @Nullable Integer getIterationIndex() {
    return iterationIndex;
  }

  @JsonProperty("iterationIndex")
  public void setIterationIndex(@Nullable Integer iterationIndex) {
    this.iterationIndex = iterationIndex;
  }

  public FocusPathElementModel taskExecutionId(Long taskExecutionId) {
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
  public Long getTaskExecutionId() {
    return taskExecutionId;
  }

  @JsonProperty("taskExecutionId")
  public void setTaskExecutionId(Long taskExecutionId) {
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
    return Objects.equals(this.iterationIndex, focusPathElement.iterationIndex) &&
        Objects.equals(this.taskExecutionId, focusPathElement.taskExecutionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(iterationIndex, taskExecutionId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FocusPathElementModel {\n");
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

