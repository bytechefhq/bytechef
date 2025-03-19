package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A cluster element basic definition.
 */

@Schema(name = "ClusterElementDefinitionBasic", description = "A cluster element basic definition.")
@JsonTypeName("ClusterElementDefinitionBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-03-20T07:39:40.498527+01:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
public class ClusterElementDefinitionBasicModel {

  private String componentName;

  private Integer componentVersion;

  private String name;

  private String type;

  private Boolean outputDefined;

  public ClusterElementDefinitionBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ClusterElementDefinitionBasicModel(String componentName, Integer componentVersion, String name, String type, Boolean outputDefined) {
    this.componentName = componentName;
    this.componentVersion = componentVersion;
    this.name = name;
    this.type = type;
    this.outputDefined = outputDefined;
  }

  public ClusterElementDefinitionBasicModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The component name.
   * @return componentName
   */
  @NotNull 
  @Schema(name = "componentName", description = "The component name.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public ClusterElementDefinitionBasicModel componentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * The component version.
   * @return componentVersion
   */
  @NotNull 
  @Schema(name = "componentVersion", description = "The component version.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentVersion")
  public Integer getComponentVersion() {
    return componentVersion;
  }

  public void setComponentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  public ClusterElementDefinitionBasicModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The cluster element name.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The cluster element name.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ClusterElementDefinitionBasicModel type(String type) {
    this.type = type;
    return this;
  }

  /**
   * The cluster element type.
   * @return type
   */
  @NotNull 
  @Schema(name = "type", description = "The cluster element type.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public ClusterElementDefinitionBasicModel outputDefined(Boolean outputDefined) {
    this.outputDefined = outputDefined;
    return this;
  }

  /**
   * Does action define output schema.
   * @return outputDefined
   */
  @NotNull 
  @Schema(name = "outputDefined", description = "Does action define output schema.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("outputDefined")
  public Boolean getOutputDefined() {
    return outputDefined;
  }

  public void setOutputDefined(Boolean outputDefined) {
    this.outputDefined = outputDefined;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterElementDefinitionBasicModel clusterElementDefinitionBasic = (ClusterElementDefinitionBasicModel) o;
    return Objects.equals(this.componentName, clusterElementDefinitionBasic.componentName) &&
        Objects.equals(this.componentVersion, clusterElementDefinitionBasic.componentVersion) &&
        Objects.equals(this.name, clusterElementDefinitionBasic.name) &&
        Objects.equals(this.type, clusterElementDefinitionBasic.type) &&
        Objects.equals(this.outputDefined, clusterElementDefinitionBasic.outputDefined);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, componentVersion, name, type, outputDefined);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClusterElementDefinitionBasicModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    outputDefined: ").append(toIndentedString(outputDefined)).append("\n");
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

