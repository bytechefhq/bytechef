package com.bytechef.automation.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.WorkflowInputModel;
import com.bytechef.platform.configuration.web.rest.model.WorkflowOutputModel;
import com.bytechef.platform.configuration.web.rest.model.WorkflowTaskModel;
import com.bytechef.platform.configuration.web.rest.model.WorkflowTriggerModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-11-26T21:19:57.797159+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public class WorkflowModel implements com.bytechef.platform.configuration.web.rest.model.WorkflowModelAware {

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  private Integer connectionsCount;

  private String definition;

  private String description;

  private com.bytechef.platform.configuration.web.rest.model.WorkflowFormatModel format;

  private String id;

  @Valid
  private List<@Valid WorkflowInputModel> inputs = new ArrayList<>();

  private Integer inputsCount;

  private String label;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  @Valid
  private List<@Valid WorkflowOutputModel> outputs = new ArrayList<>();

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

  private SourceTypeEnum sourceType;

  private Integer maxRetries;

  @Valid
  private List<String> workflowTaskComponentNames = new ArrayList<>();

  @Valid
  private List<String> workflowTriggerComponentNames = new ArrayList<>();

  @Valid
  private List<@Valid WorkflowTaskModel> tasks = new ArrayList<>();

  @Valid
  private List<@Valid WorkflowTriggerModel> triggers = new ArrayList<>();

  private Integer version;

  private Long projectWorkflowId;

  private String workflowReferenceCode;

  public WorkflowModel createdBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * The created by.
   * @return createdBy
   */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The created by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdBy")
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
  @JsonProperty("createdDate")
  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public WorkflowModel connectionsCount(Integer connectionsCount) {
    this.connectionsCount = connectionsCount;
    return this;
  }

  /**
   * The number of workflow connections
   * @return connectionsCount
   */
  
  @Schema(name = "connectionsCount", accessMode = Schema.AccessMode.READ_ONLY, description = "The number of workflow connections", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionsCount")
  public Integer getConnectionsCount() {
    return connectionsCount;
  }

  public void setConnectionsCount(Integer connectionsCount) {
    this.connectionsCount = connectionsCount;
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
  @JsonProperty("definition")
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
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public WorkflowModel format(com.bytechef.platform.configuration.web.rest.model.WorkflowFormatModel format) {
    this.format = format;
    return this;
  }

  /**
   * Get format
   * @return format
   */
  @Valid 
  @Schema(name = "format", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("format")
  public com.bytechef.platform.configuration.web.rest.model.WorkflowFormatModel getFormat() {
    return format;
  }

  public void setFormat(com.bytechef.platform.configuration.web.rest.model.WorkflowFormatModel format) {
    this.format = format;
  }

  public WorkflowModel id(String id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a workflow.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public WorkflowModel inputs(List<@Valid WorkflowInputModel> inputs) {
    this.inputs = inputs;
    return this;
  }

  public WorkflowModel addInputsItem(WorkflowInputModel inputsItem) {
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
  @JsonProperty("inputs")
  public List<@Valid WorkflowInputModel> getInputs() {
    return inputs;
  }

  public void setInputs(List<@Valid WorkflowInputModel> inputs) {
    this.inputs = inputs;
  }

  public WorkflowModel inputsCount(Integer inputsCount) {
    this.inputsCount = inputsCount;
    return this;
  }

  /**
   * The number of workflow inputs
   * @return inputsCount
   */
  
  @Schema(name = "inputsCount", accessMode = Schema.AccessMode.READ_ONLY, description = "The number of workflow inputs", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputsCount")
  public Integer getInputsCount() {
    return inputsCount;
  }

  public void setInputsCount(Integer inputsCount) {
    this.inputsCount = inputsCount;
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
  @JsonProperty("label")
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
  @JsonProperty("lastModifiedBy")
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
  @JsonProperty("lastModifiedDate")
  public LocalDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public WorkflowModel outputs(List<@Valid WorkflowOutputModel> outputs) {
    this.outputs = outputs;
    return this;
  }

  public WorkflowModel addOutputsItem(WorkflowOutputModel outputsItem) {
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
  @JsonProperty("outputs")
  public List<@Valid WorkflowOutputModel> getOutputs() {
    return outputs;
  }

  public void setOutputs(List<@Valid WorkflowOutputModel> outputs) {
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
  @JsonProperty("sourceType")
  public SourceTypeEnum getSourceType() {
    return sourceType;
  }

  public void setSourceType(SourceTypeEnum sourceType) {
    this.sourceType = sourceType;
  }

  public WorkflowModel maxRetries(Integer maxRetries) {
    this.maxRetries = maxRetries;
    return this;
  }

  /**
   * The maximum number of times a task may retry.
   * @return maxRetries
   */
  
  @Schema(name = "maxRetries", accessMode = Schema.AccessMode.READ_ONLY, description = "The maximum number of times a task may retry.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("maxRetries")
  public Integer getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(Integer maxRetries) {
    this.maxRetries = maxRetries;
  }

  public WorkflowModel workflowTaskComponentNames(List<String> workflowTaskComponentNames) {
    this.workflowTaskComponentNames = workflowTaskComponentNames;
    return this;
  }

  public WorkflowModel addWorkflowTaskComponentNamesItem(String workflowTaskComponentNamesItem) {
    if (this.workflowTaskComponentNames == null) {
      this.workflowTaskComponentNames = new ArrayList<>();
    }
    this.workflowTaskComponentNames.add(workflowTaskComponentNamesItem);
    return this;
  }

  /**
   * Get workflowTaskComponentNames
   * @return workflowTaskComponentNames
   */
  
  @Schema(name = "workflowTaskComponentNames", accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowTaskComponentNames")
  public List<String> getWorkflowTaskComponentNames() {
    return workflowTaskComponentNames;
  }

  public void setWorkflowTaskComponentNames(List<String> workflowTaskComponentNames) {
    this.workflowTaskComponentNames = workflowTaskComponentNames;
  }

  public WorkflowModel workflowTriggerComponentNames(List<String> workflowTriggerComponentNames) {
    this.workflowTriggerComponentNames = workflowTriggerComponentNames;
    return this;
  }

  public WorkflowModel addWorkflowTriggerComponentNamesItem(String workflowTriggerComponentNamesItem) {
    if (this.workflowTriggerComponentNames == null) {
      this.workflowTriggerComponentNames = new ArrayList<>();
    }
    this.workflowTriggerComponentNames.add(workflowTriggerComponentNamesItem);
    return this;
  }

  /**
   * Get workflowTriggerComponentNames
   * @return workflowTriggerComponentNames
   */
  
  @Schema(name = "workflowTriggerComponentNames", accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowTriggerComponentNames")
  public List<String> getWorkflowTriggerComponentNames() {
    return workflowTriggerComponentNames;
  }

  public void setWorkflowTriggerComponentNames(List<String> workflowTriggerComponentNames) {
    this.workflowTriggerComponentNames = workflowTriggerComponentNames;
  }

  public WorkflowModel tasks(List<@Valid WorkflowTaskModel> tasks) {
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
  @JsonProperty("tasks")
  public List<@Valid WorkflowTaskModel> getTasks() {
    return tasks;
  }

  public void setTasks(List<@Valid WorkflowTaskModel> tasks) {
    this.tasks = tasks;
  }

  public WorkflowModel triggers(List<@Valid WorkflowTriggerModel> triggers) {
    this.triggers = triggers;
    return this;
  }

  public WorkflowModel addTriggersItem(WorkflowTriggerModel triggersItem) {
    if (this.triggers == null) {
      this.triggers = new ArrayList<>();
    }
    this.triggers.add(triggersItem);
    return this;
  }

  /**
   * The steps that make up the workflow.
   * @return triggers
   */
  @Valid 
  @Schema(name = "triggers", accessMode = Schema.AccessMode.READ_ONLY, description = "The steps that make up the workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("triggers")
  public List<@Valid WorkflowTriggerModel> getTriggers() {
    return triggers;
  }

  public void setTriggers(List<@Valid WorkflowTriggerModel> triggers) {
    this.triggers = triggers;
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
  @JsonProperty("__version")
  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public WorkflowModel projectWorkflowId(Long projectWorkflowId) {
    this.projectWorkflowId = projectWorkflowId;
    return this;
  }

  /**
   * The project workflow id
   * @return projectWorkflowId
   */
  
  @Schema(name = "projectWorkflowId", accessMode = Schema.AccessMode.READ_ONLY, description = "The project workflow id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectWorkflowId")
  public Long getProjectWorkflowId() {
    return projectWorkflowId;
  }

  public void setProjectWorkflowId(Long projectWorkflowId) {
    this.projectWorkflowId = projectWorkflowId;
  }

  public WorkflowModel workflowReferenceCode(String workflowReferenceCode) {
    this.workflowReferenceCode = workflowReferenceCode;
    return this;
  }

  /**
   * The workflow reference code
   * @return workflowReferenceCode
   */
  
  @Schema(name = "workflowReferenceCode", accessMode = Schema.AccessMode.READ_ONLY, description = "The workflow reference code", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowReferenceCode")
  public String getWorkflowReferenceCode() {
    return workflowReferenceCode;
  }

  public void setWorkflowReferenceCode(String workflowReferenceCode) {
    this.workflowReferenceCode = workflowReferenceCode;
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
        Objects.equals(this.connectionsCount, workflow.connectionsCount) &&
        Objects.equals(this.definition, workflow.definition) &&
        Objects.equals(this.description, workflow.description) &&
        Objects.equals(this.format, workflow.format) &&
        Objects.equals(this.id, workflow.id) &&
        Objects.equals(this.inputs, workflow.inputs) &&
        Objects.equals(this.inputsCount, workflow.inputsCount) &&
        Objects.equals(this.label, workflow.label) &&
        Objects.equals(this.lastModifiedBy, workflow.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, workflow.lastModifiedDate) &&
        Objects.equals(this.outputs, workflow.outputs) &&
        Objects.equals(this.sourceType, workflow.sourceType) &&
        Objects.equals(this.maxRetries, workflow.maxRetries) &&
        Objects.equals(this.workflowTaskComponentNames, workflow.workflowTaskComponentNames) &&
        Objects.equals(this.workflowTriggerComponentNames, workflow.workflowTriggerComponentNames) &&
        Objects.equals(this.tasks, workflow.tasks) &&
        Objects.equals(this.triggers, workflow.triggers) &&
        Objects.equals(this.version, workflow.version) &&
        Objects.equals(this.projectWorkflowId, workflow.projectWorkflowId) &&
        Objects.equals(this.workflowReferenceCode, workflow.workflowReferenceCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, connectionsCount, definition, description, format, id, inputs, inputsCount, label, lastModifiedBy, lastModifiedDate, outputs, sourceType, maxRetries, workflowTaskComponentNames, workflowTriggerComponentNames, tasks, triggers, version, projectWorkflowId, workflowReferenceCode);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    connectionsCount: ").append(toIndentedString(connectionsCount)).append("\n");
    sb.append("    definition: ").append(toIndentedString(definition)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    format: ").append(toIndentedString(format)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    inputsCount: ").append(toIndentedString(inputsCount)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    outputs: ").append(toIndentedString(outputs)).append("\n");
    sb.append("    sourceType: ").append(toIndentedString(sourceType)).append("\n");
    sb.append("    maxRetries: ").append(toIndentedString(maxRetries)).append("\n");
    sb.append("    workflowTaskComponentNames: ").append(toIndentedString(workflowTaskComponentNames)).append("\n");
    sb.append("    workflowTriggerComponentNames: ").append(toIndentedString(workflowTriggerComponentNames)).append("\n");
    sb.append("    tasks: ").append(toIndentedString(tasks)).append("\n");
    sb.append("    triggers: ").append(toIndentedString(triggers)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    projectWorkflowId: ").append(toIndentedString(projectWorkflowId)).append("\n");
    sb.append("    workflowReferenceCode: ").append(toIndentedString(workflowReferenceCode)).append("\n");
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

