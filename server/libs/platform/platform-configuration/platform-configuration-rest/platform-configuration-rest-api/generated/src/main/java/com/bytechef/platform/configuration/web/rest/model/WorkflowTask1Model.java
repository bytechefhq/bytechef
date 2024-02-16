package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.DataStreamComponentModel;
import com.bytechef.platform.configuration.web.rest.model.WorkflowConnectionModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Represents a definition of a workflow task.
 */

@Schema(name = "WorkflowTask_1", description = "Represents a definition of a workflow task.")
@JsonTypeName("WorkflowTask_1")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-02-26T05:59:06.696196+01:00[Europe/Zagreb]")
public class WorkflowTask1Model {

  @Valid
  private List<@Valid WorkflowConnectionModel> connections;

  private DataStreamComponentModel destination;

  @Valid
  private List<@Valid WorkflowTask1Model> finalize;

  private String label;

  private String name;

  private String node;

  @Valid
  private Map<String, Object> parameters = new HashMap<>();

  @Valid
  private List<@Valid WorkflowTask1Model> post;

  @Valid
  private List<@Valid WorkflowTask1Model> pre;

  private DataStreamComponentModel source;

  private String timeout;

  private String type;

  public WorkflowTask1Model() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WorkflowTask1Model(String name, String type) {
    this.name = name;
    this.type = type;
  }

  public WorkflowTask1Model connections(List<@Valid WorkflowConnectionModel> connections) {
    this.connections = connections;
    return this;
  }

  public WorkflowTask1Model addConnectionsItem(WorkflowConnectionModel connectionsItem) {
    if (this.connections == null) {
      this.connections = new ArrayList<>();
    }
    this.connections.add(connectionsItem);
    return this;
  }

  /**
   * Get connections
   * @return connections
  */
  @Valid 
  @Schema(name = "connections", accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connections")
  public List<@Valid WorkflowConnectionModel> getConnections() {
    return connections;
  }

  public void setConnections(List<@Valid WorkflowConnectionModel> connections) {
    this.connections = connections;
  }

  public WorkflowTask1Model destination(DataStreamComponentModel destination) {
    this.destination = destination;
    return this;
  }

  /**
   * Get destination
   * @return destination
  */
  @Valid 
  @Schema(name = "destination", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("destination")
  public DataStreamComponentModel getDestination() {
    return destination;
  }

  public void setDestination(DataStreamComponentModel destination) {
    this.destination = destination;
  }

  public WorkflowTask1Model finalize(List<@Valid WorkflowTask1Model> finalize) {
    this.finalize = finalize;
    return this;
  }

  public WorkflowTask1Model addFinalizeItem(WorkflowTask1Model finalizeItem) {
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
  public List<@Valid WorkflowTask1Model> getFinalize() {
    return finalize;
  }

  public void setFinalize(List<@Valid WorkflowTask1Model> finalize) {
    this.finalize = finalize;
  }

  public WorkflowTask1Model label(String label) {
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

  public WorkflowTask1Model name(String name) {
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

  public WorkflowTask1Model node(String node) {
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

  public WorkflowTask1Model parameters(Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  public WorkflowTask1Model putParametersItem(String key, Object parametersItem) {
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

  public WorkflowTask1Model post(List<@Valid WorkflowTask1Model> post) {
    this.post = post;
    return this;
  }

  public WorkflowTask1Model addPostItem(WorkflowTask1Model postItem) {
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
  public List<@Valid WorkflowTask1Model> getPost() {
    return post;
  }

  public void setPost(List<@Valid WorkflowTask1Model> post) {
    this.post = post;
  }

  public WorkflowTask1Model pre(List<@Valid WorkflowTask1Model> pre) {
    this.pre = pre;
    return this;
  }

  public WorkflowTask1Model addPreItem(WorkflowTask1Model preItem) {
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
  public List<@Valid WorkflowTask1Model> getPre() {
    return pre;
  }

  public void setPre(List<@Valid WorkflowTask1Model> pre) {
    this.pre = pre;
  }

  public WorkflowTask1Model source(DataStreamComponentModel source) {
    this.source = source;
    return this;
  }

  /**
   * Get source
   * @return source
  */
  @Valid 
  @Schema(name = "source", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("source")
  public DataStreamComponentModel getSource() {
    return source;
  }

  public void setSource(DataStreamComponentModel source) {
    this.source = source;
  }

  public WorkflowTask1Model timeout(String timeout) {
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

  public WorkflowTask1Model type(String type) {
    this.type = type;
    return this;
  }

  /**
   * The type of a task.
   * @return type
  */
  @NotNull 
  @Schema(name = "type", description = "The type of a task.", requiredMode = Schema.RequiredMode.REQUIRED)
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
    WorkflowTask1Model workflowTask1 = (WorkflowTask1Model) o;
    return Objects.equals(this.connections, workflowTask1.connections) &&
        Objects.equals(this.destination, workflowTask1.destination) &&
        Objects.equals(this.finalize, workflowTask1.finalize) &&
        Objects.equals(this.label, workflowTask1.label) &&
        Objects.equals(this.name, workflowTask1.name) &&
        Objects.equals(this.node, workflowTask1.node) &&
        Objects.equals(this.parameters, workflowTask1.parameters) &&
        Objects.equals(this.post, workflowTask1.post) &&
        Objects.equals(this.pre, workflowTask1.pre) &&
        Objects.equals(this.source, workflowTask1.source) &&
        Objects.equals(this.timeout, workflowTask1.timeout) &&
        Objects.equals(this.type, workflowTask1.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connections, destination, finalize, label, name, node, parameters, post, pre, source, timeout, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowTask1Model {\n");
    sb.append("    connections: ").append(toIndentedString(connections)).append("\n");
    sb.append("    destination: ").append(toIndentedString(destination)).append("\n");
    sb.append("    finalize: ").append(toIndentedString(finalize)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    node: ").append(toIndentedString(node)).append("\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
    sb.append("    post: ").append(toIndentedString(post)).append("\n");
    sb.append("    pre: ").append(toIndentedString(pre)).append("\n");
    sb.append("    source: ").append(toIndentedString(source)).append("\n");
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

