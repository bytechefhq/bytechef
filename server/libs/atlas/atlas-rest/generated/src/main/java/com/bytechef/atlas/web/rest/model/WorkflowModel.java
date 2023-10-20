package com.bytechef.atlas.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.atlas.web.rest.model.ExecutionErrorModel;
import com.bytechef.atlas.web.rest.model.WorkflowTaskModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * WorkflowModel
 */

@JsonTypeName("Workflow")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-27T10:08:31.057495+02:00[Europe/Zagreb]")
public class WorkflowModel {

  @JsonProperty("content")
  private String content;

  @JsonProperty("createdBy")
  private String createdBy;

  @JsonProperty("createdDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  @JsonProperty("error")
  private ExecutionErrorModel error;

  /**
   * Gets or Sets format
   */
  public enum FormatEnum {
    JSON("JSON"),
    
    YML("YML"),
    
    YAML("YAML");

    private String value;

    FormatEnum(String value) {
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
    public static FormatEnum fromValue(String value) {
      for (FormatEnum b : FormatEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("format")
  private FormatEnum format;

  @JsonProperty("id")
  private String id;

  @JsonProperty("inputs")
  @Valid
  private List<Map<String, Object>> inputs = null;

  @JsonProperty("label")
  private String label;

  @JsonProperty("lastModifiedBy")
  private String lastModifiedBy;

  @JsonProperty("lastModifiedDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  @JsonProperty("outputs")
  @Valid
  private List<Map<String, Object>> outputs = null;

  @JsonProperty("tasks")
  @Valid
  private List<WorkflowTaskModel> tasks = null;

  @JsonProperty("retry")
  private Integer retry;

  public WorkflowModel content(String content) {
    this.content = content;
    return this;
  }

  /**
   * Get content
   * @return content
  */
  
  @Schema(name = "content", required = false)
  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public WorkflowModel createdBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * Get createdBy
   * @return createdBy
  */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, required = false)
  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public WorkflowModel createdDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  /**
   * Get createdDate
   * @return createdDate
  */
  @Valid 
  @Schema(name = "createdDate", accessMode = Schema.AccessMode.READ_ONLY, required = false)
  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public WorkflowModel error(ExecutionErrorModel error) {
    this.error = error;
    return this;
  }

  /**
   * Get error
   * @return error
  */
  @Valid 
  @Schema(name = "error", required = false)
  public ExecutionErrorModel getError() {
    return error;
  }

  public void setError(ExecutionErrorModel error) {
    this.error = error;
  }

  public WorkflowModel format(FormatEnum format) {
    this.format = format;
    return this;
  }

  /**
   * Get format
   * @return format
  */
  
  @Schema(name = "format", required = false)
  public FormatEnum getFormat() {
    return format;
  }

  public void setFormat(FormatEnum format) {
    this.format = format;
  }

  public WorkflowModel id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, required = false)
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public WorkflowModel inputs(List<Map<String, Object>> inputs) {
    this.inputs = inputs;
    return this;
  }

  public WorkflowModel addInputsItem(Map<String, Object> inputsItem) {
    if (this.inputs == null) {
      this.inputs = new ArrayList<>();
    }
    this.inputs.add(inputsItem);
    return this;
  }

  /**
   * Get inputs
   * @return inputs
  */
  @Valid 
  @Schema(name = "inputs", required = false)
  public List<Map<String, Object>> getInputs() {
    return inputs;
  }

  public void setInputs(List<Map<String, Object>> inputs) {
    this.inputs = inputs;
  }

  public WorkflowModel label(String label) {
    this.label = label;
    return this;
  }

  /**
   * Get label
   * @return label
  */
  
  @Schema(name = "label", required = false)
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public WorkflowModel lastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  /**
   * Get lastModifiedBy
   * @return lastModifiedBy
  */
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, required = false)
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public WorkflowModel lastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
    return this;
  }

  /**
   * Get lastModifiedDate
   * @return lastModifiedDate
  */
  @Valid 
  @Schema(name = "lastModifiedDate", accessMode = Schema.AccessMode.READ_ONLY, required = false)
  public LocalDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public WorkflowModel outputs(List<Map<String, Object>> outputs) {
    this.outputs = outputs;
    return this;
  }

  public WorkflowModel addOutputsItem(Map<String, Object> outputsItem) {
    if (this.outputs == null) {
      this.outputs = new ArrayList<>();
    }
    this.outputs.add(outputsItem);
    return this;
  }

  /**
   * Get outputs
   * @return outputs
  */
  @Valid 
  @Schema(name = "outputs", required = false)
  public List<Map<String, Object>> getOutputs() {
    return outputs;
  }

  public void setOutputs(List<Map<String, Object>> outputs) {
    this.outputs = outputs;
  }

  public WorkflowModel tasks(List<WorkflowTaskModel> tasks) {
    this.tasks = tasks;
    return this;
  }

  public WorkflowModel addTasksItem(WorkflowTaskModel tasksItem) {
    if (this.tasks == null) {
      this.tasks = new ArrayList<>();
    }
    this.tasks.add(tasksItem);
    return this;
  }

  /**
   * Get tasks
   * @return tasks
  */
  @Valid 
  @Schema(name = "tasks", required = false)
  public List<WorkflowTaskModel> getTasks() {
    return tasks;
  }

  public void setTasks(List<WorkflowTaskModel> tasks) {
    this.tasks = tasks;
  }

  public WorkflowModel retry(Integer retry) {
    this.retry = retry;
    return this;
  }

  /**
   * Get retry
   * @return retry
  */
  
  @Schema(name = "retry", required = false)
  public Integer getRetry() {
    return retry;
  }

  public void setRetry(Integer retry) {
    this.retry = retry;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkflowModel workflow = (WorkflowModel) o;
    return Objects.equals(this.content, workflow.content) &&
        Objects.equals(this.createdBy, workflow.createdBy) &&
        Objects.equals(this.createdDate, workflow.createdDate) &&
        Objects.equals(this.error, workflow.error) &&
        Objects.equals(this.format, workflow.format) &&
        Objects.equals(this.id, workflow.id) &&
        Objects.equals(this.inputs, workflow.inputs) &&
        Objects.equals(this.label, workflow.label) &&
        Objects.equals(this.lastModifiedBy, workflow.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, workflow.lastModifiedDate) &&
        Objects.equals(this.outputs, workflow.outputs) &&
        Objects.equals(this.tasks, workflow.tasks) &&
        Objects.equals(this.retry, workflow.retry);
  }

  @Override
  public int hashCode() {
    return Objects.hash(content, createdBy, createdDate, error, format, id, inputs, label, lastModifiedBy, lastModifiedDate, outputs, tasks, retry);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowModel {\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    format: ").append(toIndentedString(format)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    outputs: ").append(toIndentedString(outputs)).append("\n");
    sb.append("    tasks: ").append(toIndentedString(tasks)).append("\n");
    sb.append("    retry: ").append(toIndentedString(retry)).append("\n");
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

