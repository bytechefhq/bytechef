package com.bytechef.helios.execution.web.rest.model;

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
 * WorkflowConnectionModel
 */

@JsonTypeName("WorkflowConnection")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-12-11T16:20:09.622934+01:00[Europe/Zagreb]")
public class WorkflowConnectionModel {

  private String componentName;

  private Integer componentVersion;

  private Long id;

  private String key;

  private String operationName;

  private Boolean required;

  public WorkflowConnectionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WorkflowConnectionModel(String componentName, Integer componentVersion, String key, String operationName, Boolean required) {
    this.componentName = componentName;
    this.componentVersion = componentVersion;
    this.key = key;
    this.operationName = operationName;
    this.required = required;
  }

  public WorkflowConnectionModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * Get componentName
   * @return componentName
  */
  @NotNull 
  @Schema(name = "componentName", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public WorkflowConnectionModel componentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * Get componentVersion
   * @return componentVersion
  */
  @NotNull 
  @Schema(name = "componentVersion", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentVersion")
  public Integer getComponentVersion() {
    return componentVersion;
  }

  public void setComponentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  public WorkflowConnectionModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
  */
  
  @Schema(name = "id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public WorkflowConnectionModel key(String key) {
    this.key = key;
    return this;
  }

  /**
   * Get key
   * @return key
  */
  @NotNull 
  @Schema(name = "key", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("key")
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public WorkflowConnectionModel operationName(String operationName) {
    this.operationName = operationName;
    return this;
  }

  /**
   * Get operationName
   * @return operationName
  */
  @NotNull 
  @Schema(name = "operationName", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("operationName")
  public String getOperationName() {
    return operationName;
  }

  public void setOperationName(String operationName) {
    this.operationName = operationName;
  }

  public WorkflowConnectionModel required(Boolean required) {
    this.required = required;
    return this;
  }

  /**
   * Get required
   * @return required
  */
  @NotNull 
  @Schema(name = "required", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("required")
  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkflowConnectionModel workflowConnection = (WorkflowConnectionModel) o;
    return Objects.equals(this.componentName, workflowConnection.componentName) &&
        Objects.equals(this.componentVersion, workflowConnection.componentVersion) &&
        Objects.equals(this.id, workflowConnection.id) &&
        Objects.equals(this.key, workflowConnection.key) &&
        Objects.equals(this.operationName, workflowConnection.operationName) &&
        Objects.equals(this.required, workflowConnection.required);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, componentVersion, id, key, operationName, required);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowConnectionModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    key: ").append(toIndentedString(key)).append("\n");
    sb.append("    operationName: ").append(toIndentedString(operationName)).append("\n");
    sb.append("    required: ").append(toIndentedString(required)).append("\n");
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

