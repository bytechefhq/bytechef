package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.PropertyModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A cluster element definition.
 */

@Schema(name = "ClusterElementDefinition", description = "A cluster element definition.")
@JsonTypeName("ClusterElementDefinition")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-03-14T15:53:17.053861+01:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
public class ClusterElementDefinitionModel {

  private String componentName;

  private Integer componentVersion;

  private String elementType;

  private Boolean outputDefined;

  @Valid
  private List<@Valid PropertyModel> properties = new ArrayList<>();

  public ClusterElementDefinitionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ClusterElementDefinitionModel(String componentName, Integer componentVersion, String elementType, Boolean outputDefined) {
    this.componentName = componentName;
    this.componentVersion = componentVersion;
    this.elementType = elementType;
    this.outputDefined = outputDefined;
  }

  public ClusterElementDefinitionModel componentName(String componentName) {
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

  public ClusterElementDefinitionModel componentVersion(Integer componentVersion) {
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

  public ClusterElementDefinitionModel elementType(String elementType) {
    this.elementType = elementType;
    return this;
  }

  /**
   * The cluster element type.
   * @return elementType
   */
  @NotNull 
  @Schema(name = "elementType", description = "The cluster element type.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("elementType")
  public String getElementType() {
    return elementType;
  }

  public void setElementType(String elementType) {
    this.elementType = elementType;
  }

  public ClusterElementDefinitionModel outputDefined(Boolean outputDefined) {
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

  public ClusterElementDefinitionModel properties(List<@Valid PropertyModel> properties) {
    this.properties = properties;
    return this;
  }

  public ClusterElementDefinitionModel addPropertiesItem(PropertyModel propertiesItem) {
    if (this.properties == null) {
      this.properties = new ArrayList<>();
    }
    this.properties.add(propertiesItem);
    return this;
  }

  /**
   * The list of action properties.
   * @return properties
   */
  @Valid 
  @Schema(name = "properties", description = "The list of action properties.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("properties")
  public List<@Valid PropertyModel> getProperties() {
    return properties;
  }

  public void setProperties(List<@Valid PropertyModel> properties) {
    this.properties = properties;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterElementDefinitionModel clusterElementDefinition = (ClusterElementDefinitionModel) o;
    return Objects.equals(this.componentName, clusterElementDefinition.componentName) &&
        Objects.equals(this.componentVersion, clusterElementDefinition.componentVersion) &&
        Objects.equals(this.elementType, clusterElementDefinition.elementType) &&
        Objects.equals(this.outputDefined, clusterElementDefinition.outputDefined) &&
        Objects.equals(this.properties, clusterElementDefinition.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, componentVersion, elementType, outputDefined, properties);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClusterElementDefinitionModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    elementType: ").append(toIndentedString(elementType)).append("\n");
    sb.append("    outputDefined: ").append(toIndentedString(outputDefined)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
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

