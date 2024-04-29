package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * UpdateWorkflowNodeParameterRequestModel
 */

@JsonTypeName("updateWorkflowNodeParameter_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-04-29T06:21:57.549276+02:00[Europe/Zagreb]", comments = "Generator version: 7.4.0")
public class UpdateWorkflowNodeParameterRequestModel {

  private Integer arrayIndex;

  private String name;

  private String path;

  private Object value;

  private String workflowNodeName;

  public UpdateWorkflowNodeParameterRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UpdateWorkflowNodeParameterRequestModel(String path, String workflowNodeName) {
    this.path = path;
    this.workflowNodeName = workflowNodeName;
  }

  public UpdateWorkflowNodeParameterRequestModel arrayIndex(Integer arrayIndex) {
    this.arrayIndex = arrayIndex;
    return this;
  }

  /**
   * The array index.
   * @return arrayIndex
  */
  
  @Schema(name = "arrayIndex", description = "The array index.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("arrayIndex")
  public Integer getArrayIndex() {
    return arrayIndex;
  }

  public void setArrayIndex(Integer arrayIndex) {
    this.arrayIndex = arrayIndex;
  }

  public UpdateWorkflowNodeParameterRequestModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The parameter name.
   * @return name
  */
  
  @Schema(name = "name", description = "The parameter name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public UpdateWorkflowNodeParameterRequestModel path(String path) {
    this.path = path;
    return this;
  }

  /**
   * The workflow node parameter path.
   * @return path
  */
  @NotNull 
  @Schema(name = "path", description = "The workflow node parameter path.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("path")
  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public UpdateWorkflowNodeParameterRequestModel value(Object value) {
    this.value = value;
    return this;
  }

  /**
   * The value.
   * @return value
  */
  
  @Schema(name = "value", description = "The value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("value")
  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public UpdateWorkflowNodeParameterRequestModel workflowNodeName(String workflowNodeName) {
    this.workflowNodeName = workflowNodeName;
    return this;
  }

  /**
   * The workflow node name.
   * @return workflowNodeName
  */
  @NotNull 
  @Schema(name = "workflowNodeName", description = "The workflow node name.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workflowNodeName")
  public String getWorkflowNodeName() {
    return workflowNodeName;
  }

  public void setWorkflowNodeName(String workflowNodeName) {
    this.workflowNodeName = workflowNodeName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateWorkflowNodeParameterRequestModel updateWorkflowNodeParameterRequest = (UpdateWorkflowNodeParameterRequestModel) o;
    return Objects.equals(this.arrayIndex, updateWorkflowNodeParameterRequest.arrayIndex) &&
        Objects.equals(this.name, updateWorkflowNodeParameterRequest.name) &&
        Objects.equals(this.path, updateWorkflowNodeParameterRequest.path) &&
        Objects.equals(this.value, updateWorkflowNodeParameterRequest.value) &&
        Objects.equals(this.workflowNodeName, updateWorkflowNodeParameterRequest.workflowNodeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(arrayIndex, name, path, value, workflowNodeName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateWorkflowNodeParameterRequestModel {\n");
    sb.append("    arrayIndex: ").append(toIndentedString(arrayIndex)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    workflowNodeName: ").append(toIndentedString(workflowNodeName)).append("\n");
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

