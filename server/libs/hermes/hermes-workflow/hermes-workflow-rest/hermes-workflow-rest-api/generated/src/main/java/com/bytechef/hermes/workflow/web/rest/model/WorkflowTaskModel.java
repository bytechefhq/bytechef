package com.bytechef.hermes.workflow.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Represents a definition of the task.
 */

@Schema(name = "WorkflowTask", description = "Represents a definition of the task.")
@JsonTypeName("WorkflowTask")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-06T08:20:36.906696+02:00[Europe/Zagreb]")
public class WorkflowTaskModel {

  @Valid
  private List<@Valid WorkflowTaskModel> finalize;

  private String label;

  private String name;

  private String node;

  @Valid
  private Map<String, Object> parameters = new HashMap<>();

  @Valid
  private List<@Valid WorkflowTaskModel> post;

  @Valid
  private List<@Valid WorkflowTaskModel> pre;

  private String timeout;

  private String type;

  /**
   * Default constructor
   * @deprecated Use {@link WorkflowTaskModel#WorkflowTaskModel(String, String)}
   */
  @Deprecated
  public WorkflowTaskModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WorkflowTaskModel(String name, String type) {
    this.name = name;
    this.type = type;
  }

  public WorkflowTaskModel finalize(List<@Valid WorkflowTaskModel> finalize) {
    this.finalize = finalize;
    return this;
  }

  public WorkflowTaskModel addFinalizeItem(WorkflowTaskModel finalizeItem) {
    if (this.finalize == null) {
      this.finalize = new ArrayList<>();
    }
    this.finalize.add(finalizeItem);
    return this;
  }

  /**
   * The (optional) list of tasks that are to be executed after execution of a task -- regardless of whether it had failed or not.
   * @return finalize
  */
  @Valid 
  @Schema(name = "finalize", description = "The (optional) list of tasks that are to be executed after execution of a task -- regardless of whether it had failed or not.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("finalize")
  public List<@Valid WorkflowTaskModel> getFinalize() {
    return finalize;
  }

  public void setFinalize(List<@Valid WorkflowTaskModel> finalize) {
    this.finalize = finalize;
  }

  public WorkflowTaskModel label(String label) {
    this.label = label;
    return this;
  }

  /**
   * The human-readable description of the task.
   * @return label
  */
  
  @Schema(name = "label", description = "The human-readable description of the task.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public WorkflowTaskModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The identifier name of the task. Task names are used for assigning the output of one task so it can be later used by subsequent tasks.
   * @return name
  */
  @NotNull 
  @Schema(name = "name", description = "The identifier name of the task. Task names are used for assigning the output of one task so it can be later used by subsequent tasks.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public WorkflowTaskModel node(String node) {
    this.node = node;
    return this;
  }

  /**
   * Defines the name of the type of the node that the task execution will be routed to. For instance, if the node value is \"encoder\", then the task will be routed to the \"encoder\" queue which is presumably subscribed to by worker nodes of \"encoder\" type.
   * @return node
  */
  
  @Schema(name = "node", description = "Defines the name of the type of the node that the task execution will be routed to. For instance, if the node value is \"encoder\", then the task will be routed to the \"encoder\" queue which is presumably subscribed to by worker nodes of \"encoder\" type.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("node")
  public String getNode() {
    return node;
  }

  public void setNode(String node) {
    this.node = node;
  }

  public WorkflowTaskModel parameters(Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  public WorkflowTaskModel putParametersItem(String key, Object parametersItem) {
    if (this.parameters == null) {
      this.parameters = new HashMap<>();
    }
    this.parameters.put(key, parametersItem);
    return this;
  }

  /**
   * Key-value map of task parameters.
   * @return parameters
  */
  
  @Schema(name = "parameters", description = "Key-value map of task parameters.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("parameters")
  public Map<String, Object> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  public WorkflowTaskModel post(List<@Valid WorkflowTaskModel> post) {
    this.post = post;
    return this;
  }

  public WorkflowTaskModel addPostItem(WorkflowTaskModel postItem) {
    if (this.post == null) {
      this.post = new ArrayList<>();
    }
    this.post.add(postItem);
    return this;
  }

  /**
   * The (optional) list of tasks that are to be executed after the successful execution of a task.
   * @return post
  */
  @Valid 
  @Schema(name = "post", description = "The (optional) list of tasks that are to be executed after the successful execution of a task.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("post")
  public List<@Valid WorkflowTaskModel> getPost() {
    return post;
  }

  public void setPost(List<@Valid WorkflowTaskModel> post) {
    this.post = post;
  }

  public WorkflowTaskModel pre(List<@Valid WorkflowTaskModel> pre) {
    this.pre = pre;
    return this;
  }

  public WorkflowTaskModel addPreItem(WorkflowTaskModel preItem) {
    if (this.pre == null) {
      this.pre = new ArrayList<>();
    }
    this.pre.add(preItem);
    return this;
  }

  /**
   * The (optional) list of tasks that are to be executed prior to a task.
   * @return pre
  */
  @Valid 
  @Schema(name = "pre", description = "The (optional) list of tasks that are to be executed prior to a task.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("pre")
  public List<@Valid WorkflowTaskModel> getPre() {
    return pre;
  }

  public void setPre(List<@Valid WorkflowTaskModel> pre) {
    this.pre = pre;
  }

  public WorkflowTaskModel timeout(String timeout) {
    this.timeout = timeout;
    return this;
  }

  /**
   * The timeout expression which describes when a task should be deemed as timed-out.
   * @return timeout
  */
  
  @Schema(name = "timeout", description = "The timeout expression which describes when a task should be deemed as timed-out.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("timeout")
  public String getTimeout() {
    return timeout;
  }

  public void setTimeout(String timeout) {
    this.timeout = timeout;
  }

  public WorkflowTaskModel type(String type) {
    this.type = type;
    return this;
  }

  /**
   * Type of the task.
   * @return type
  */
  @NotNull 
  @Schema(name = "type", description = "Type of the task.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkflowTaskModel workflowTask = (WorkflowTaskModel) o;
    return Objects.equals(this.finalize, workflowTask.finalize) &&
        Objects.equals(this.label, workflowTask.label) &&
        Objects.equals(this.name, workflowTask.name) &&
        Objects.equals(this.node, workflowTask.node) &&
        Objects.equals(this.parameters, workflowTask.parameters) &&
        Objects.equals(this.post, workflowTask.post) &&
        Objects.equals(this.pre, workflowTask.pre) &&
        Objects.equals(this.timeout, workflowTask.timeout) &&
        Objects.equals(this.type, workflowTask.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(finalize, label, name, node, parameters, post, pre, timeout, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowTaskModel {\n");
    sb.append("    finalize: ").append(toIndentedString(finalize)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    node: ").append(toIndentedString(node)).append("\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
    sb.append("    post: ").append(toIndentedString(post)).append("\n");
    sb.append("    pre: ").append(toIndentedString(pre)).append("\n");
    sb.append("    timeout: ").append(toIndentedString(timeout)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
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

