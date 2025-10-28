package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.ComponentConnectionModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;
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

@Schema(name = "WorkflowTask", description = "Represents a definition of a workflow task.")
@JsonTypeName("WorkflowTask")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-21T12:06:41.161145+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public class WorkflowTaskModel {

  private Boolean clusterRoot = false;

  @Valid
  private Map<String, Object> clusterElements = new HashMap<>();

  @Valid
  private List<@Valid ComponentConnectionModel> connections = new ArrayList<>();

  private @Nullable String description;

  @Valid
  private List<@Valid WorkflowTaskModel> finalize = new ArrayList<>();

  private @Nullable String label;

  @Valid
  private Map<String, Object> metadata = new HashMap<>();

  private String name;

  private @Nullable String node;

  @Valid
  private Map<String, Object> parameters = new HashMap<>();

  @Valid
  private List<@Valid WorkflowTaskModel> post = new ArrayList<>();

  @Valid
  private List<@Valid WorkflowTaskModel> pre = new ArrayList<>();

  private @Nullable String timeout;

  private String type;

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

  public WorkflowTaskModel clusterRoot(Boolean clusterRoot) {
    this.clusterRoot = clusterRoot;
    return this;
  }

  /**
   * Get clusterRoot
   * @return clusterRoot
   */
  
  @Schema(name = "clusterRoot", accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("clusterRoot")
  public Boolean getClusterRoot() {
    return clusterRoot;
  }

  public void setClusterRoot(Boolean clusterRoot) {
    this.clusterRoot = clusterRoot;
  }

  public WorkflowTaskModel clusterElements(Map<String, Object> clusterElements) {
    this.clusterElements = clusterElements;
    return this;
  }

  public WorkflowTaskModel putClusterElementsItem(String key, Object clusterElementsItem) {
    if (this.clusterElements == null) {
      this.clusterElements = new HashMap<>();
    }
    this.clusterElements.put(key, clusterElementsItem);
    return this;
  }

  /**
   * Get clusterElements
   * @return clusterElements
   */
  
  @Schema(name = "clusterElements", accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("clusterElements")
  public Map<String, Object> getClusterElements() {
    return clusterElements;
  }

  public void setClusterElements(Map<String, Object> clusterElements) {
    this.clusterElements = clusterElements;
  }

  public WorkflowTaskModel connections(List<@Valid ComponentConnectionModel> connections) {
    this.connections = connections;
    return this;
  }

  public WorkflowTaskModel addConnectionsItem(ComponentConnectionModel connectionsItem) {
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
  public List<@Valid ComponentConnectionModel> getConnections() {
    return connections;
  }

  public void setConnections(List<@Valid ComponentConnectionModel> connections) {
    this.connections = connections;
  }

  public WorkflowTaskModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of the task.
   * @return description
   */
  
  @Schema(name = "description", description = "The description of the task.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
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

  public WorkflowTaskModel label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * The human-readable description of the task.
   * @return label
   */
  
  @Schema(name = "label", description = "The human-readable description of the task.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  public void setLabel(@Nullable String label) {
    this.label = label;
  }

  public WorkflowTaskModel metadata(Map<String, Object> metadata) {
    this.metadata = metadata;
    return this;
  }

  public WorkflowTaskModel putMetadataItem(String key, Object metadataItem) {
    if (this.metadata == null) {
      this.metadata = new HashMap<>();
    }
    this.metadata.put(key, metadataItem);
    return this;
  }

  /**
   * Key-value map of metadata.
   * @return metadata
   */
  
  @Schema(name = "metadata", description = "Key-value map of metadata.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("metadata")
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
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

  public WorkflowTaskModel node(@Nullable String node) {
    this.node = node;
    return this;
  }

  /**
   * Defines the name of the type of the node that the task execution will be routed to. For instance, if the node value is \"encoder\", then the task will be routed to the \"encoder\" queue which is presumably subscribed to by worker nodes of \"encoder\" type.
   * @return node
   */
  
  @Schema(name = "node", description = "Defines the name of the type of the node that the task execution will be routed to. For instance, if the node value is \"encoder\", then the task will be routed to the \"encoder\" queue which is presumably subscribed to by worker nodes of \"encoder\" type.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("node")
  public @Nullable String getNode() {
    return node;
  }

  public void setNode(@Nullable String node) {
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

  public WorkflowTaskModel timeout(@Nullable String timeout) {
    this.timeout = timeout;
    return this;
  }

  /**
   * The timeout expression which describes when a task should be deemed as timed-out.
   * @return timeout
   */
  
  @Schema(name = "timeout", description = "The timeout expression which describes when a task should be deemed as timed-out.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("timeout")
  public @Nullable String getTimeout() {
    return timeout;
  }

  public void setTimeout(@Nullable String timeout) {
    this.timeout = timeout;
  }

  public WorkflowTaskModel type(String type) {
    this.type = type;
    return this;
  }

  /**
   * The type of the task.
   * @return type
   */
  @NotNull 
  @Schema(name = "type", description = "The type of the task.", requiredMode = Schema.RequiredMode.REQUIRED)
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
    return Objects.equals(this.clusterRoot, workflowTask.clusterRoot) &&
        Objects.equals(this.clusterElements, workflowTask.clusterElements) &&
        Objects.equals(this.connections, workflowTask.connections) &&
        Objects.equals(this.description, workflowTask.description) &&
        Objects.equals(this.finalize, workflowTask.finalize) &&
        Objects.equals(this.label, workflowTask.label) &&
        Objects.equals(this.metadata, workflowTask.metadata) &&
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
    return Objects.hash(clusterRoot, clusterElements, connections, description, finalize, label, metadata, name, node, parameters, post, pre, timeout, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowTaskModel {\n");
    sb.append("    clusterRoot: ").append(toIndentedString(clusterRoot)).append("\n");
    sb.append("    clusterElements: ").append(toIndentedString(clusterElements)).append("\n");
    sb.append("    connections: ").append(toIndentedString(connections)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    finalize: ").append(toIndentedString(finalize)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
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

