package com.bytechef.hermes.configuration.web.rest.model;

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
 * GetActionDefinitionsTaskTypesParameterInnerModel
 */

@JsonTypeName("getActionDefinitions_taskTypes_parameter_inner")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-06-22T07:40:21.691849+02:00[Europe/Zagreb]")
public class GetActionDefinitionsTaskTypesParameterInnerModel {

  private String componentName;

  private Integer componentVersion;

  private String actionName;

  public GetActionDefinitionsTaskTypesParameterInnerModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * Get componentName
   * @return componentName
  */
  
  @Schema(name = "componentName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public GetActionDefinitionsTaskTypesParameterInnerModel componentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * Get componentVersion
   * @return componentVersion
  */
  
  @Schema(name = "componentVersion", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentVersion")
  public Integer getComponentVersion() {
    return componentVersion;
  }

  public void setComponentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  public GetActionDefinitionsTaskTypesParameterInnerModel actionName(String actionName) {
    this.actionName = actionName;
    return this;
  }

  /**
   * Get actionName
   * @return actionName
  */
  
  @Schema(name = "actionName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("actionName")
  public String getActionName() {
    return actionName;
  }

  public void setActionName(String actionName) {
    this.actionName = actionName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetActionDefinitionsTaskTypesParameterInnerModel getActionDefinitionsTaskTypesParameterInner = (GetActionDefinitionsTaskTypesParameterInnerModel) o;
    return Objects.equals(this.componentName, getActionDefinitionsTaskTypesParameterInner.componentName) &&
        Objects.equals(this.componentVersion, getActionDefinitionsTaskTypesParameterInner.componentVersion) &&
        Objects.equals(this.actionName, getActionDefinitionsTaskTypesParameterInner.actionName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, componentVersion, actionName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetActionDefinitionsTaskTypesParameterInnerModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    actionName: ").append(toIndentedString(actionName)).append("\n");
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

