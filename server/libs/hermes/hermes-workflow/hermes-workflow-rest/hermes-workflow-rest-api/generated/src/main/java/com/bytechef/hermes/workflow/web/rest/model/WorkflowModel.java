package com.bytechef.hermes.workflow.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.workflow.web.rest.model.InputModel;
import com.bytechef.hermes.workflow.web.rest.model.OutputModel;
import com.bytechef.hermes.workflow.web.rest.model.WorkflowFormatModel;
import com.bytechef.hermes.workflow.web.rest.model.WorkflowTaskModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The blueprint that describe the execution of a job.
 */

@Schema(name = "Workflow", description = "The blueprint that describe the execution of a job.")
@JsonTypeName("Workflow")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-01T14:11:10.397866+02:00[Europe/Zagreb]")
public class WorkflowModel {

  @JsonProperty("createdBy")
  private String createdBy;

  @JsonProperty("createdDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  @JsonProperty("definition")
  private String definition;

  @JsonProperty("description")
  private String description;

  @JsonProperty("format")
  private WorkflowFormatModel format;

  @JsonProperty("id")
  private String id;

  @JsonProperty("inputs")
  @Valid
  private List<InputModel> inputs = null;

  @JsonProperty("label")
  private String label;

  @JsonProperty("lastModifiedBy")
  private String lastModifiedBy;

  @JsonProperty("lastModifiedDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  @JsonProperty("outputs")
  @Valid
  private List<OutputModel> outputs = null;

  /**
   * The type of the source which stores the workflow definition.
   */
  public enum SourceTypeEnum {
    CLASSPATH("CLASSPATH"),
    
    FILESYSTEM("FILESYSTEM"),
    
    GIT("GIT"),
    
    JDBC("JDBC");

    private String value;

    SourceTypeEnum(String value) {
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
    public static SourceTypeEnum fromValue(String value) {
      for (SourceTypeEnum b : SourceTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("sourceType")
  private SourceTypeEnum sourceType;

  @JsonProperty("retry")
  private Integer retry;

  @JsonProperty("tasks")
  @Valid
  private List<WorkflowTaskModel> tasks = null;

  @JsonProperty("__version")
  private Integer version;

  public WorkflowModel createdBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * The created by.
   * @return createdBy
  */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The created by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
   * The created date.
   * @return createdDate
  */
  @Valid 
  @Schema(name = "createdDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The created date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public WorkflowModel definition(String definition) {
    this.definition = definition;
    return this;
  }

  /**
   * The definition of a workflow.
   * @return definition
  */
  
  @Schema(name = "definition", description = "The definition of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getDefinition() {
    return definition;
  }

  public void setDefinition(String definition) {
    this.definition = definition;
  }

  public WorkflowModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of a workflow.
   * @return description
  */
  
  @Schema(name = "description", description = "The description of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public WorkflowModel format(WorkflowFormatModel format) {
    this.format = format;
    return this;
  }

  /**
   * Get format
   * @return format
  */
  @Valid 
  @Schema(name = "format", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public WorkflowFormatModel getFormat() {
    return format;
  }

  public void setFormat(WorkflowFormatModel format) {
    this.format = format;
  }

  public WorkflowModel id(String id) {
    this.id = id;
    return this;
  }

  /**
   * The id of the workflow.
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of the workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public WorkflowModel inputs(List<InputModel> inputs) {
    this.inputs = inputs;
    return this;
  }

  public WorkflowModel addInputsItem(InputModel inputsItem) {
    if (this.inputs == null) {
      this.inputs = new ArrayList<>();
    }
    this.inputs.add(inputsItem);
    return this;
  }

  /**
   * The workflow's expected list of inputs.
   * @return inputs
  */
  @Valid 
  @Schema(name = "inputs", accessMode = Schema.AccessMode.READ_ONLY, description = "The workflow's expected list of inputs.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public List<InputModel> getInputs() {
    return inputs;
  }

  public void setInputs(List<InputModel> inputs) {
    this.inputs = inputs;
  }

  public WorkflowModel label(String label) {
    this.label = label;
    return this;
  }

  /**
   * The descriptive name for the workflow
   * @return label
  */
  
  @Schema(name = "label", accessMode = Schema.AccessMode.READ_ONLY, description = "The descriptive name for the workflow", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
   * The last modified by.
   * @return lastModifiedBy
  */
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
   * The last modified date.
   * @return lastModifiedDate
  */
  @Valid 
  @Schema(name = "lastModifiedDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public LocalDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public WorkflowModel outputs(List<OutputModel> outputs) {
    this.outputs = outputs;
    return this;
  }

  public WorkflowModel addOutputsItem(OutputModel outputsItem) {
    if (this.outputs == null) {
      this.outputs = new ArrayList<>();
    }
    this.outputs.add(outputsItem);
    return this;
  }

  /**
   * The workflow's list of expected outputs.
   * @return outputs
  */
  @Valid 
  @Schema(name = "outputs", accessMode = Schema.AccessMode.READ_ONLY, description = "The workflow's list of expected outputs.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public List<OutputModel> getOutputs() {
    return outputs;
  }

  public void setOutputs(List<OutputModel> outputs) {
    this.outputs = outputs;
  }

  public WorkflowModel sourceType(SourceTypeEnum sourceType) {
    this.sourceType = sourceType;
    return this;
  }

  /**
   * The type of the source which stores the workflow definition.
   * @return sourceType
  */
  
  @Schema(name = "sourceType", description = "The type of the source which stores the workflow definition.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public SourceTypeEnum getSourceType() {
    return sourceType;
  }

  public void setSourceType(SourceTypeEnum sourceType) {
    this.sourceType = sourceType;
  }

  public WorkflowModel retry(Integer retry) {
    this.retry = retry;
    return this;
  }

  /**
   * The maximum number of times a task may retry.
   * @return retry
  */
  
  @Schema(name = "retry", accessMode = Schema.AccessMode.READ_ONLY, description = "The maximum number of times a task may retry.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Integer getRetry() {
    return retry;
  }

  public void setRetry(Integer retry) {
    this.retry = retry;
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
   * The steps that make up the workflow.
   * @return tasks
  */
  @Valid 
  @Schema(name = "tasks", accessMode = Schema.AccessMode.READ_ONLY, description = "The steps that make up the workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public List<WorkflowTaskModel> getTasks() {
    return tasks;
  }

  public void setTasks(List<WorkflowTaskModel> tasks) {
    this.tasks = tasks;
  }

  public WorkflowModel version(Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * @return version
  */
  
  @Schema(name = "__version", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
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
    return Objects.equals(this.createdBy, workflow.createdBy) &&
        Objects.equals(this.createdDate, workflow.createdDate) &&
        Objects.equals(this.definition, workflow.definition) &&
        Objects.equals(this.description, workflow.description) &&
        Objects.equals(this.format, workflow.format) &&
        Objects.equals(this.id, workflow.id) &&
        Objects.equals(this.inputs, workflow.inputs) &&
        Objects.equals(this.label, workflow.label) &&
        Objects.equals(this.lastModifiedBy, workflow.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, workflow.lastModifiedDate) &&
        Objects.equals(this.outputs, workflow.outputs) &&
        Objects.equals(this.sourceType, workflow.sourceType) &&
        Objects.equals(this.retry, workflow.retry) &&
        Objects.equals(this.tasks, workflow.tasks) &&
        Objects.equals(this.version, workflow.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, definition, description, format, id, inputs, label, lastModifiedBy, lastModifiedDate, outputs, sourceType, retry, tasks, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    definition: ").append(toIndentedString(definition)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    format: ").append(toIndentedString(format)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    outputs: ").append(toIndentedString(outputs)).append("\n");
    sb.append("    sourceType: ").append(toIndentedString(sourceType)).append("\n");
    sb.append("    retry: ").append(toIndentedString(retry)).append("\n");
    sb.append("    tasks: ").append(toIndentedString(tasks)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
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

