package com.bytechef.helios.project.web.rest.model;

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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-04T12:16:52.208086+02:00[Europe/Zagreb]")
public class WorkflowConnectionModel {

  private String componentName;

  private Integer connectionVersion;

  /**
   * Default constructor
   * @deprecated Use {@link WorkflowConnectionModel#WorkflowConnectionModel(String, Integer)}
   */
  @Deprecated
  public WorkflowConnectionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WorkflowConnectionModel(String componentName, Integer connectionVersion) {
    this.componentName = componentName;
    this.connectionVersion = connectionVersion;
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

  public WorkflowConnectionModel connectionVersion(Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
    return this;
  }

  /**
   * Get connectionVersion
   * @return connectionVersion
  */
  @NotNull 
  @Schema(name = "connectionVersion", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connectionVersion")
  public Integer getConnectionVersion() {
    return connectionVersion;
  }

  public void setConnectionVersion(Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
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
        Objects.equals(this.connectionVersion, workflowConnection.connectionVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, connectionVersion);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowConnectionModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    connectionVersion: ").append(toIndentedString(connectionVersion)).append("\n");
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

