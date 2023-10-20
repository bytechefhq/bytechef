package com.bytechef.atlas.web.rest.model;

import java.net.URI;
import java.util.Objects;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * PostWorkflowRequestModel
 */

@JsonTypeName("postWorkflow_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-12-18T09:37:42.127301+01:00[Europe/Zagreb]")
public class PostWorkflowRequestModel {

  @JsonProperty("definition")
  private String definition;

  @JsonProperty("createdBy")
  private String createdBy;

  @JsonProperty("createdDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  /**
   * The format of the workflow definition.
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

  /**
   * Gets or Sets providerType
   */
  public enum ProviderTypeEnum {
    JDBC("JDBC");

    private String value;

    ProviderTypeEnum(String value) {
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
    public static ProviderTypeEnum fromValue(String value) {
      for (ProviderTypeEnum b : ProviderTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("providerType")
  private ProviderTypeEnum providerType;

  @JsonProperty("outputs")
  @Valid
  private List<Map<String, Object>> outputs = null;

  @JsonProperty("retry")
  private Integer retry;

  @JsonProperty("tasks")
  @Valid
  private List<WorkflowTaskModel> tasks = null;

  public PostWorkflowRequestModel definition(String definition) {
    this.definition = definition;
    return this;
  }

  /**
   * Definition of the workflow that is executed as a job.
   * @return definition
  */
  
  @Schema(name = "definition", description = "Definition of the workflow that is executed as a job.", required = false)
  public String getDefinition() {
    return definition;
  }

  public void setDefinition(String definition) {
    this.definition = definition;
  }

  public PostWorkflowRequestModel createdBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * The created by.
   * @return createdBy
  */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The created by.", required = false)
  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public PostWorkflowRequestModel createdDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  /**
   * The created date.
   * @return createdDate
  */
  @Valid 
  @Schema(name = "createdDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The created date.", required = false)
  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public PostWorkflowRequestModel format(FormatEnum format) {
    this.format = format;
    return this;
  }

  /**
   * The format of the workflow definition.
   * @return format
  */
  
  @Schema(name = "format", description = "The format of the workflow definition.", required = false)
  public FormatEnum getFormat() {
    return format;
  }

  public void setFormat(FormatEnum format) {
    this.format = format;
  }

  public PostWorkflowRequestModel id(String id) {
    this.id = id;
    return this;
  }

  /**
   * The id of the workflow.
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of the workflow.", required = false)
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public PostWorkflowRequestModel inputs(List<Map<String, Object>> inputs) {
    this.inputs = inputs;
    return this;
  }

  public PostWorkflowRequestModel addInputsItem(Map<String, Object> inputsItem) {
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
  @Schema(name = "inputs", description = "The workflow's expected list of inputs.", required = false)
  public List<Map<String, Object>> getInputs() {
    return inputs;
  }

  public void setInputs(List<Map<String, Object>> inputs) {
    this.inputs = inputs;
  }

  public PostWorkflowRequestModel label(String label) {
    this.label = label;
    return this;
  }

  /**
   * The descriptive name for the workflow
   * @return label
  */
  
  @Schema(name = "label", description = "The descriptive name for the workflow", required = false)
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public PostWorkflowRequestModel lastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  /**
   * The last modified by.
   * @return lastModifiedBy
  */
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified by.", required = false)
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public PostWorkflowRequestModel lastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
    return this;
  }

  /**
   * The last modified date.
   * @return lastModifiedDate
  */
  @Valid 
  @Schema(name = "lastModifiedDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified date.", required = false)
  public LocalDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public PostWorkflowRequestModel providerType(ProviderTypeEnum providerType) {
    this.providerType = providerType;
    return this;
  }

  /**
   * Get providerType
   * @return providerType
  */
  
  @Schema(name = "providerType", required = false)
  public ProviderTypeEnum getProviderType() {
    return providerType;
  }

  public void setProviderType(ProviderTypeEnum providerType) {
    this.providerType = providerType;
  }

  public PostWorkflowRequestModel outputs(List<Map<String, Object>> outputs) {
    this.outputs = outputs;
    return this;
  }

  public PostWorkflowRequestModel addOutputsItem(Map<String, Object> outputsItem) {
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
  @Schema(name = "outputs", description = "The workflow's list of expected outputs.", required = false)
  public List<Map<String, Object>> getOutputs() {
    return outputs;
  }

  public void setOutputs(List<Map<String, Object>> outputs) {
    this.outputs = outputs;
  }

  public PostWorkflowRequestModel retry(Integer retry) {
    this.retry = retry;
    return this;
  }

  /**
   * The maximum number of times a task may retry.
   * @return retry
  */
  
  @Schema(name = "retry", description = "The maximum number of times a task may retry.", required = false)
  public Integer getRetry() {
    return retry;
  }

  public void setRetry(Integer retry) {
    this.retry = retry;
  }

  public PostWorkflowRequestModel tasks(List<WorkflowTaskModel> tasks) {
    this.tasks = tasks;
    return this;
  }

  public PostWorkflowRequestModel addTasksItem(WorkflowTaskModel tasksItem) {
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
  @Schema(name = "tasks", description = "The steps that make up the workflow.", required = false)
  public List<WorkflowTaskModel> getTasks() {
    return tasks;
  }

  public void setTasks(List<WorkflowTaskModel> tasks) {
    this.tasks = tasks;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PostWorkflowRequestModel postWorkflowRequest = (PostWorkflowRequestModel) o;
    return Objects.equals(this.definition, postWorkflowRequest.definition) &&
        Objects.equals(this.createdBy, postWorkflowRequest.createdBy) &&
        Objects.equals(this.createdDate, postWorkflowRequest.createdDate) &&
        Objects.equals(this.format, postWorkflowRequest.format) &&
        Objects.equals(this.id, postWorkflowRequest.id) &&
        Objects.equals(this.inputs, postWorkflowRequest.inputs) &&
        Objects.equals(this.label, postWorkflowRequest.label) &&
        Objects.equals(this.lastModifiedBy, postWorkflowRequest.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, postWorkflowRequest.lastModifiedDate) &&
        Objects.equals(this.providerType, postWorkflowRequest.providerType) &&
        Objects.equals(this.outputs, postWorkflowRequest.outputs) &&
        Objects.equals(this.retry, postWorkflowRequest.retry) &&
        Objects.equals(this.tasks, postWorkflowRequest.tasks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(definition, createdBy, createdDate, format, id, inputs, label, lastModifiedBy, lastModifiedDate, providerType, outputs, retry, tasks);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostWorkflowRequestModel {\n");
    sb.append("    definition: ").append(toIndentedString(definition)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    format: ").append(toIndentedString(format)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    providerType: ").append(toIndentedString(providerType)).append("\n");
    sb.append("    outputs: ").append(toIndentedString(outputs)).append("\n");
    sb.append("    retry: ").append(toIndentedString(retry)).append("\n");
    sb.append("    tasks: ").append(toIndentedString(tasks)).append("\n");
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

