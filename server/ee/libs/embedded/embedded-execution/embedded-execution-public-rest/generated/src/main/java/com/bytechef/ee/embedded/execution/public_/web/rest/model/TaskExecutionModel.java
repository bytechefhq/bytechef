package com.bytechef.ee.embedded.execution.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ExecutionErrorModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The execution of a single workflow task, including its input and output.
 */

@Schema(name = "TaskExecution", description = "The execution of a single workflow task, including its input and output.")
@JsonTypeName("TaskExecution")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-18T16:40:13.932099944Z[Etc/UTC]", comments = "Generator version: 7.22.0")
public class TaskExecutionModel {

  private @Nullable String id;

  private @Nullable String name;

  private @Nullable String title;

  private @Nullable String type;

  /**
   * The status of a task execution.
   */
  public enum StatusEnum {
    CREATED("CREATED"),
    
    STARTED("STARTED"),
    
    COMPLETED("COMPLETED"),
    
    FAILED("FAILED"),
    
    CANCELLED("CANCELLED");

    private final String value;

    StatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equalsIgnoreCase(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private @Nullable StatusEnum status;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime startDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime endDate;

  private @Nullable ExecutionErrorModel error;

  @Valid
  private Map<String, Object> input = new HashMap<>();

  private JsonNullable<Object> output = JsonNullable.<Object>undefined();

  public TaskExecutionModel id(@Nullable String id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a task execution.
   * @return id
   */
  
  @Schema(name = "id", description = "The id of a task execution.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable String getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable String id) {
    this.id = id;
  }

  public TaskExecutionModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the executed workflow task.
   * @return name
   */
  
  @Schema(name = "name", description = "The name of the executed workflow task.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public TaskExecutionModel title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * The title of the executed workflow task.
   * @return title
   */
  
  @Schema(name = "title", description = "The title of the executed workflow task.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public TaskExecutionModel type(@Nullable String type) {
    this.type = type;
    return this;
  }

  /**
   * The type of the executed workflow task.
   * @return type
   */
  
  @Schema(name = "type", description = "The type of the executed workflow task.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("type")
  public @Nullable String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(@Nullable String type) {
    this.type = type;
  }

  public TaskExecutionModel status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * The status of a task execution.
   * @return status
   */
  
  @Schema(name = "status", description = "The status of a task execution.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("status")
  public @Nullable StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable StatusEnum status) {
    this.status = status;
  }

  public TaskExecutionModel startDate(@Nullable OffsetDateTime startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * The instant a task execution started.
   * @return startDate
   */
  @Valid 
  @Schema(name = "startDate", description = "The instant a task execution started.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("startDate")
  public @Nullable OffsetDateTime getStartDate() {
    return startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(@Nullable OffsetDateTime startDate) {
    this.startDate = startDate;
  }

  public TaskExecutionModel endDate(@Nullable OffsetDateTime endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * The instant a task execution ended.
   * @return endDate
   */
  @Valid 
  @Schema(name = "endDate", description = "The instant a task execution ended.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("endDate")
  public @Nullable OffsetDateTime getEndDate() {
    return endDate;
  }

  @JsonProperty("endDate")
  public void setEndDate(@Nullable OffsetDateTime endDate) {
    this.endDate = endDate;
  }

  public TaskExecutionModel error(@Nullable ExecutionErrorModel error) {
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

  public TaskExecutionModel input(Map<String, Object> input) {
    this.input = input;
    return this;
  }

  public TaskExecutionModel putInputItem(String key, Object inputItem) {
    if (this.input == null) {
      this.input = new HashMap<>();
    }
    this.input.put(key, inputItem);
    return this;
  }

  /**
   * The resolved input parameters of a task execution.
   * @return input
   */
  
  @Schema(name = "input", description = "The resolved input parameters of a task execution.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("input")
  public Map<String, Object> getInput() {
    return input;
  }

  @JsonProperty("input")
  public void setInput(Map<String, Object> input) {
    this.input = input;
  }

  public TaskExecutionModel output(Object output) {
    this.output = JsonNullable.of(output);
    return this;
  }

  /**
   * The output of a task execution.
   * @return output
   */
  
  @Schema(name = "output", description = "The output of a task execution.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("output")
  public JsonNullable<Object> getOutput() {
    return output;
  }

  public void setOutput(JsonNullable<Object> output) {
    this.output = output;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TaskExecutionModel taskExecution = (TaskExecutionModel) o;
    return Objects.equals(this.id, taskExecution.id) &&
        Objects.equals(this.name, taskExecution.name) &&
        Objects.equals(this.title, taskExecution.title) &&
        Objects.equals(this.type, taskExecution.type) &&
        Objects.equals(this.status, taskExecution.status) &&
        Objects.equals(this.startDate, taskExecution.startDate) &&
        Objects.equals(this.endDate, taskExecution.endDate) &&
        Objects.equals(this.error, taskExecution.error) &&
        Objects.equals(this.input, taskExecution.input) &&
        equalsNullable(this.output, taskExecution.output);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, title, type, status, startDate, endDate, error, input, hashCodeNullable(output));
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TaskExecutionModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    input: ").append(toIndentedString(input)).append("\n");
    sb.append("    output: ").append(toIndentedString(output)).append("\n");
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

