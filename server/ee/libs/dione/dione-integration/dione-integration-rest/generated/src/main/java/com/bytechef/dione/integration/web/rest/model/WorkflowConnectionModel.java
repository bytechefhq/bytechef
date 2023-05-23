package com.bytechef.dione.integration.web.rest.model;

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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-23T16:33:59.288008+02:00[Europe/Zagreb]")
public class WorkflowConnectionModel {

  private String componentName;

  private Integer componentVersion;

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
  public WorkflowConnectionModel(String componentName, Integer componentVersion) {
    this.componentName = componentName;
    this.componentVersion = componentVersion;
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
        Objects.equals(this.componentVersion, workflowConnection.componentVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, componentVersion);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowConnectionModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
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

