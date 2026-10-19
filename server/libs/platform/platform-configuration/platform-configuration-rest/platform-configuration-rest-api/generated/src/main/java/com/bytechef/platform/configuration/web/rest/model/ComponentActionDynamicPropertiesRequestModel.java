package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
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
 * ComponentActionDynamicPropertiesRequestModel
 */

@JsonTypeName("ComponentActionDynamicPropertiesRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-23T16:46:07.669852+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ComponentActionDynamicPropertiesRequestModel {

  private String actionName;

  private String componentName;

  private Integer componentVersion;

  private @Nullable String connectionKey;

  private List<String> lookupDependsOnPaths = new ArrayList<>();

  private Map<String, Object> parameters = new HashMap<>();

  public ComponentActionDynamicPropertiesRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ComponentActionDynamicPropertiesRequestModel(String actionName, String componentName, Integer componentVersion) {
    this.actionName = actionName;
    this.componentName = componentName;
    this.componentVersion = componentVersion;
  }

  public ComponentActionDynamicPropertiesRequestModel actionName(String actionName) {
    this.actionName = actionName;
    return this;
  }

  /**
   * The name of a component action.
   * @return actionName
   */
  @NotNull 
  @Schema(name = "actionName", description = "The name of a component action.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("actionName")
  public String getActionName() {
    return actionName;
  }

  @JsonProperty("actionName")
  public void setActionName(String actionName) {
    this.actionName = actionName;
  }

  public ComponentActionDynamicPropertiesRequestModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of a component.
   * @return componentName
   */
  @NotNull 
  @Schema(name = "componentName", description = "The name of a component.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  @JsonProperty("componentName")
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public ComponentActionDynamicPropertiesRequestModel componentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * The version of a component.
   * @return componentVersion
   */
  @NotNull 
  @Schema(name = "componentVersion", description = "The version of a component.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentVersion")
  public Integer getComponentVersion() {
    return componentVersion;
  }

  @JsonProperty("componentVersion")
  public void setComponentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  public ComponentActionDynamicPropertiesRequestModel connectionKey(@Nullable String connectionKey) {
    this.connectionKey = connectionKey;
    return this;
  }

  /**
   * The key of a component connection declared on the workflow node.
   * @return connectionKey
   */
  
  @Schema(name = "connectionKey", description = "The key of a component connection declared on the workflow node.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionKey")
  public @Nullable String getConnectionKey() {
    return connectionKey;
  }

  @JsonProperty("connectionKey")
  public void setConnectionKey(@Nullable String connectionKey) {
    this.connectionKey = connectionKey;
  }

  public ComponentActionDynamicPropertiesRequestModel lookupDependsOnPaths(List<String> lookupDependsOnPaths) {
    this.lookupDependsOnPaths = lookupDependsOnPaths;
    return this;
  }

  public ComponentActionDynamicPropertiesRequestModel addLookupDependsOnPathsItem(String lookupDependsOnPathsItem) {
    if (this.lookupDependsOnPaths == null) {
      this.lookupDependsOnPaths = new ArrayList<>();
    }
    this.lookupDependsOnPaths.add(lookupDependsOnPathsItem);
    return this;
  }

  /**
   * The list of dependency paths.
   * @return lookupDependsOnPaths
   */
  
  @Schema(name = "lookupDependsOnPaths", description = "The list of dependency paths.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lookupDependsOnPaths")
  public List<String> getLookupDependsOnPaths() {
    return lookupDependsOnPaths;
  }

  @JsonProperty("lookupDependsOnPaths")
  public void setLookupDependsOnPaths(List<String> lookupDependsOnPaths) {
    this.lookupDependsOnPaths = lookupDependsOnPaths;
  }

  public ComponentActionDynamicPropertiesRequestModel parameters(Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  public ComponentActionDynamicPropertiesRequestModel putParametersItem(String key, Object parametersItem) {
    if (this.parameters == null) {
      this.parameters = new HashMap<>();
    }
    this.parameters.put(key, parametersItem);
    return this;
  }

  /**
   * The known parameter values of the action call.
   * @return parameters
   */
  
  @Schema(name = "parameters", description = "The known parameter values of the action call.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("parameters")
  public Map<String, Object> getParameters() {
    return parameters;
  }

  @JsonProperty("parameters")
  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComponentActionDynamicPropertiesRequestModel componentActionDynamicPropertiesRequest = (ComponentActionDynamicPropertiesRequestModel) o;
    return Objects.equals(this.actionName, componentActionDynamicPropertiesRequest.actionName) &&
        Objects.equals(this.componentName, componentActionDynamicPropertiesRequest.componentName) &&
        Objects.equals(this.componentVersion, componentActionDynamicPropertiesRequest.componentVersion) &&
        Objects.equals(this.connectionKey, componentActionDynamicPropertiesRequest.connectionKey) &&
        Objects.equals(this.lookupDependsOnPaths, componentActionDynamicPropertiesRequest.lookupDependsOnPaths) &&
        Objects.equals(this.parameters, componentActionDynamicPropertiesRequest.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(actionName, componentName, componentVersion, connectionKey, lookupDependsOnPaths, parameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComponentActionDynamicPropertiesRequestModel {\n");
    sb.append("    actionName: ").append(toIndentedString(actionName)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    connectionKey: ").append(toIndentedString(connectionKey)).append("\n");
    sb.append("    lookupDependsOnPaths: ").append(toIndentedString(lookupDependsOnPaths)).append("\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

