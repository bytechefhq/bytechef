package com.bytechef.platform.workflow.execution.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.workflow.execution.web.rest.model.TaskExecutionModel;
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
 * A page of the children of a task execution. At most one of children and iterations is non-empty; both are empty on an empty page.
 */

@Schema(name = "TaskExecutionChildrenPage", description = "A page of the children of a task execution. At most one of children and iterations is non-empty; both are empty on an empty page.")
@JsonTypeName("TaskExecutionChildrenPage")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-22T16:26:57.995256+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class TaskExecutionChildrenPageModel {

  private List<@Valid TaskExecutionModel> children = new ArrayList<>();

  private List<List<@Valid TaskExecutionModel>> iterations = new ArrayList<>();

  private Integer number;

  private Integer numberOfElements;

  private Integer size;

  private Long totalElements;

  private Integer totalPages;

  public TaskExecutionChildrenPageModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TaskExecutionChildrenPageModel(List<@Valid TaskExecutionModel> children, List<List<@Valid TaskExecutionModel>> iterations, Integer number, Integer numberOfElements, Integer size, Long totalElements, Integer totalPages) {
    this.children = children;
    this.iterations = iterations;
    this.number = number;
    this.numberOfElements = numberOfElements;
    this.size = size;
    this.totalElements = totalElements;
    this.totalPages = totalPages;
  }

  public TaskExecutionChildrenPageModel children(List<@Valid TaskExecutionModel> children) {
    this.children = children;
    return this;
  }

  public TaskExecutionChildrenPageModel addChildrenItem(TaskExecutionModel childrenItem) {
    if (this.children == null) {
      this.children = new ArrayList<>();
    }
    this.children.add(childrenItem);
    return this;
  }

  /**
   * The child task executions on this page, filled for a task that is neither a loop nor an on-error task.
   * @return children
   */
  @NotNull 
  @Schema(name = "children", description = "The child task executions on this page, filled for a task that is neither a loop nor an on-error task.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("children")
  public List<@Valid TaskExecutionModel> getChildren() {
    return children;
  }

  @JsonProperty("children")
  public void setChildren(List<@Valid TaskExecutionModel> children) {
    this.children = children;
  }

  public TaskExecutionChildrenPageModel iterations(List<List<@Valid TaskExecutionModel>> iterations) {
    this.iterations = iterations;
    return this;
  }

  public TaskExecutionChildrenPageModel addIterationsItem(List<@Valid TaskExecutionModel> iterationsItem) {
    if (this.iterations == null) {
      this.iterations = new ArrayList<>();
    }
    this.iterations.add(iterationsItem);
    return this;
  }

  /**
   * The loop iterations or on-error branches on this page, each a list of task executions.
   * @return iterations
   */
  @NotNull 
  @Schema(name = "iterations", description = "The loop iterations or on-error branches on this page, each a list of task executions.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("iterations")
  public List<List<@Valid TaskExecutionModel>> getIterations() {
    return iterations;
  }

  @JsonProperty("iterations")
  public void setIterations(List<List<@Valid TaskExecutionModel>> iterations) {
    this.iterations = iterations;
  }

  public TaskExecutionChildrenPageModel number(Integer number) {
    this.number = number;
    return this;
  }

  /**
   * The number of the page.
   * @return number
   */
  @NotNull 
  @Schema(name = "number", description = "The number of the page.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("number")
  public Integer getNumber() {
    return number;
  }

  @JsonProperty("number")
  public void setNumber(Integer number) {
    this.number = number;
  }

  public TaskExecutionChildrenPageModel numberOfElements(Integer numberOfElements) {
    this.numberOfElements = numberOfElements;
    return this;
  }

  /**
   * The number of children, iterations or branches on this page.
   * @return numberOfElements
   */
  @NotNull 
  @Schema(name = "numberOfElements", description = "The number of children, iterations or branches on this page.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("numberOfElements")
  public Integer getNumberOfElements() {
    return numberOfElements;
  }

  @JsonProperty("numberOfElements")
  public void setNumberOfElements(Integer numberOfElements) {
    this.numberOfElements = numberOfElements;
  }

  public TaskExecutionChildrenPageModel size(Integer size) {
    this.size = size;
    return this;
  }

  /**
   * The size of the page.
   * @return size
   */
  @NotNull 
  @Schema(name = "size", description = "The size of the page.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("size")
  public Integer getSize() {
    return size;
  }

  @JsonProperty("size")
  public void setSize(Integer size) {
    this.size = size;
  }

  public TaskExecutionChildrenPageModel totalElements(Long totalElements) {
    this.totalElements = totalElements;
    return this;
  }

  /**
   * The total number of children, iterations or branches.
   * @return totalElements
   */
  @NotNull 
  @Schema(name = "totalElements", description = "The total number of children, iterations or branches.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("totalElements")
  public Long getTotalElements() {
    return totalElements;
  }

  @JsonProperty("totalElements")
  public void setTotalElements(Long totalElements) {
    this.totalElements = totalElements;
  }

  public TaskExecutionChildrenPageModel totalPages(Integer totalPages) {
    this.totalPages = totalPages;
    return this;
  }

  /**
   * The total number of pages.
   * @return totalPages
   */
  @NotNull 
  @Schema(name = "totalPages", description = "The total number of pages.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("totalPages")
  public Integer getTotalPages() {
    return totalPages;
  }

  @JsonProperty("totalPages")
  public void setTotalPages(Integer totalPages) {
    this.totalPages = totalPages;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TaskExecutionChildrenPageModel taskExecutionChildrenPage = (TaskExecutionChildrenPageModel) o;
    return Objects.equals(this.children, taskExecutionChildrenPage.children) &&
        Objects.equals(this.iterations, taskExecutionChildrenPage.iterations) &&
        Objects.equals(this.number, taskExecutionChildrenPage.number) &&
        Objects.equals(this.numberOfElements, taskExecutionChildrenPage.numberOfElements) &&
        Objects.equals(this.size, taskExecutionChildrenPage.size) &&
        Objects.equals(this.totalElements, taskExecutionChildrenPage.totalElements) &&
        Objects.equals(this.totalPages, taskExecutionChildrenPage.totalPages);
  }

  @Override
  public int hashCode() {
    return Objects.hash(children, iterations, number, numberOfElements, size, totalElements, totalPages);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TaskExecutionChildrenPageModel {\n");
    sb.append("    children: ").append(toIndentedString(children)).append("\n");
    sb.append("    iterations: ").append(toIndentedString(iterations)).append("\n");
    sb.append("    number: ").append(toIndentedString(number)).append("\n");
    sb.append("    numberOfElements: ").append(toIndentedString(numberOfElements)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    totalElements: ").append(toIndentedString(totalElements)).append("\n");
    sb.append("    totalPages: ").append(toIndentedString(totalPages)).append("\n");
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

