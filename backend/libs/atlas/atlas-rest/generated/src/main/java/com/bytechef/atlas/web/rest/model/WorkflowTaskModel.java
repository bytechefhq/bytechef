package com.bytechef.atlas.web.rest.model;

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
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * WorkflowTaskModel
 */

@JsonTypeName("WorkflowTask")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-27T10:08:31.057495+02:00[Europe/Zagreb]")
public class WorkflowTaskModel {

  @JsonProperty("finalize")
  @Valid
  private List<WorkflowTaskModel> finalize = null;

  @JsonProperty("label")
  private String label;

  @JsonProperty("name")
  private String name;

  @JsonProperty("node")
  private String node;

  @JsonProperty("parameters")
  @Valid
  private Map<String, Object> parameters = null;

  @JsonProperty("post")
  @Valid
  private List<WorkflowTaskModel> post = null;

  @JsonProperty("pre")
  @Valid
  private List<WorkflowTaskModel> pre = null;

  @JsonProperty("timeout")
  private String timeout;

  @JsonProperty("type")
  private String type;

  public WorkflowTaskModel finalize(List<WorkflowTaskModel> finalize) {
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
   * Get finalize
   * @return finalize
  */
  @Valid 
  @Schema(name = "finalize", required = false)
  public List<WorkflowTaskModel> getFinalize() {
    return finalize;
  }

  public void setFinalize(List<WorkflowTaskModel> finalize) {
    this.finalize = finalize;
  }

  public WorkflowTaskModel label(String label) {
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

  public WorkflowTaskModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
  */
  
  @Schema(name = "name", required = false)
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
   * Get node
   * @return node
  */
  
  @Schema(name = "node", required = false)
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
   * Get parameters
   * @return parameters
  */
  
  @Schema(name = "parameters", required = false)
  public Map<String, Object> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  public WorkflowTaskModel post(List<WorkflowTaskModel> post) {
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
   * Get post
   * @return post
  */
  @Valid 
  @Schema(name = "post", required = false)
  public List<WorkflowTaskModel> getPost() {
    return post;
  }

  public void setPost(List<WorkflowTaskModel> post) {
    this.post = post;
  }

  public WorkflowTaskModel pre(List<WorkflowTaskModel> pre) {
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
   * Get pre
   * @return pre
  */
  @Valid 
  @Schema(name = "pre", required = false)
  public List<WorkflowTaskModel> getPre() {
    return pre;
  }

  public void setPre(List<WorkflowTaskModel> pre) {
    this.pre = pre;
  }

  public WorkflowTaskModel timeout(String timeout) {
    this.timeout = timeout;
    return this;
  }

  /**
   * Get timeout
   * @return timeout
  */
  
  @Schema(name = "timeout", required = false)
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
   * Get type
   * @return type
  */
  
  @Schema(name = "type", required = false)
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

