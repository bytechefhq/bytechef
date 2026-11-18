package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * ComponentOperationPropertyRequestModel
 */

@JsonTypeName("ComponentOperationPropertyRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-23T16:47:06.280758+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ComponentOperationPropertyRequestModel {

  private @Nullable String clusterElementType;

  private @Nullable String clusterElementWorkflowNodeName;

  private String componentName;

  private Integer componentVersion;

  private @Nullable String connectionKey;

  private List<String> lookupDependsOnPaths = new ArrayList<>();

  private String operationName;

  /**
   * The type of a component operation.
   */
  public enum OperationTypeEnum {
    ACTION("ACTION"),
    
    TRIGGER("TRIGGER");

    private final String value;

    OperationTypeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static OperationTypeEnum fromValue(String value) {
      for (OperationTypeEnum b : OperationTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private OperationTypeEnum operationType;

  private Map<String, Object> parameters = new HashMap<>();

  public ComponentOperationPropertyRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ComponentOperationPropertyRequestModel(String componentName, Integer componentVersion, String operationName, OperationTypeEnum operationType) {
    this.componentName = componentName;
    this.componentVersion = componentVersion;
    this.operationName = operationName;
    this.operationType = operationType;
  }

  public ComponentOperationPropertyRequestModel clusterElementType(@Nullable String clusterElementType) {
    this.clusterElementType = clusterElementType;
    return this;
  }

  /**
   * The type of a cluster element of the workflow node that declares the connection.
   * @return clusterElementType
   */
  
  @Schema(name = "clusterElementType", description = "The type of a cluster element of the workflow node that declares the connection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("clusterElementType")
  public @Nullable String getClusterElementType() {
    return clusterElementType;
  }

  @JsonProperty("clusterElementType")
  public void setClusterElementType(@Nullable String clusterElementType) {
    this.clusterElementType = clusterElementType;
  }

  public ComponentOperationPropertyRequestModel clusterElementWorkflowNodeName(@Nullable String clusterElementWorkflowNodeName) {
    this.clusterElementWorkflowNodeName = clusterElementWorkflowNodeName;
    return this;
  }

  /**
   * The name of a cluster element of the workflow node that declares the connection.
   * @return clusterElementWorkflowNodeName
   */
  
  @Schema(name = "clusterElementWorkflowNodeName", description = "The name of a cluster element of the workflow node that declares the connection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("clusterElementWorkflowNodeName")
  public @Nullable String getClusterElementWorkflowNodeName() {
    return clusterElementWorkflowNodeName;
  }

  @JsonProperty("clusterElementWorkflowNodeName")
  public void setClusterElementWorkflowNodeName(@Nullable String clusterElementWorkflowNodeName) {
    this.clusterElementWorkflowNodeName = clusterElementWorkflowNodeName;
  }

  public ComponentOperationPropertyRequestModel componentName(String componentName) {
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

  public ComponentOperationPropertyRequestModel componentVersion(Integer componentVersion) {
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

  public ComponentOperationPropertyRequestModel connectionKey(@Nullable String connectionKey) {
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

  public ComponentOperationPropertyRequestModel lookupDependsOnPaths(List<String> lookupDependsOnPaths) {
    this.lookupDependsOnPaths = lookupDependsOnPaths;
    return this;
  }

  public ComponentOperationPropertyRequestModel addLookupDependsOnPathsItem(String lookupDependsOnPathsItem) {
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

  public ComponentOperationPropertyRequestModel operationName(String operationName) {
    this.operationName = operationName;
    return this;
  }

  /**
   * The name of a component action or trigger.
   * @return operationName
   */
  @NotNull 
  @Schema(name = "operationName", description = "The name of a component action or trigger.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("operationName")
  public String getOperationName() {
    return operationName;
  }

  @JsonProperty("operationName")
  public void setOperationName(String operationName) {
    this.operationName = operationName;
  }

  public ComponentOperationPropertyRequestModel operationType(OperationTypeEnum operationType) {
    this.operationType = operationType;
    return this;
  }

  /**
   * The type of a component operation.
   * @return operationType
   */
  @NotNull 
  @Schema(name = "operationType", description = "The type of a component operation.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("operationType")
  public OperationTypeEnum getOperationType() {
    return operationType;
  }

  @JsonProperty("operationType")
  public void setOperationType(OperationTypeEnum operationType) {
    this.operationType = operationType;
  }

  public ComponentOperationPropertyRequestModel parameters(Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  public ComponentOperationPropertyRequestModel putParametersItem(String key, Object parametersItem) {
    if (this.parameters == null) {
      this.parameters = new HashMap<>();
    }
    this.parameters.put(key, parametersItem);
    return this;
  }

  /**
   * The known parameter values of the operation.
   * @return parameters
   */
  
  @Schema(name = "parameters", description = "The known parameter values of the operation.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    ComponentOperationPropertyRequestModel componentOperationPropertyRequest = (ComponentOperationPropertyRequestModel) o;
    return Objects.equals(this.clusterElementType, componentOperationPropertyRequest.clusterElementType) &&
        Objects.equals(this.clusterElementWorkflowNodeName, componentOperationPropertyRequest.clusterElementWorkflowNodeName) &&
        Objects.equals(this.componentName, componentOperationPropertyRequest.componentName) &&
        Objects.equals(this.componentVersion, componentOperationPropertyRequest.componentVersion) &&
        Objects.equals(this.connectionKey, componentOperationPropertyRequest.connectionKey) &&
        Objects.equals(this.lookupDependsOnPaths, componentOperationPropertyRequest.lookupDependsOnPaths) &&
        Objects.equals(this.operationName, componentOperationPropertyRequest.operationName) &&
        Objects.equals(this.operationType, componentOperationPropertyRequest.operationType) &&
        Objects.equals(this.parameters, componentOperationPropertyRequest.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clusterElementType, clusterElementWorkflowNodeName, componentName, componentVersion, connectionKey, lookupDependsOnPaths, operationName, operationType, parameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComponentOperationPropertyRequestModel {\n");
    sb.append("    clusterElementType: ").append(toIndentedString(clusterElementType)).append("\n");
    sb.append("    clusterElementWorkflowNodeName: ").append(toIndentedString(clusterElementWorkflowNodeName)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    connectionKey: ").append(toIndentedString(connectionKey)).append("\n");
    sb.append("    lookupDependsOnPaths: ").append(toIndentedString(lookupDependsOnPaths)).append("\n");
    sb.append("    operationName: ").append(toIndentedString(operationName)).append("\n");
    sb.append("    operationType: ").append(toIndentedString(operationType)).append("\n");
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

