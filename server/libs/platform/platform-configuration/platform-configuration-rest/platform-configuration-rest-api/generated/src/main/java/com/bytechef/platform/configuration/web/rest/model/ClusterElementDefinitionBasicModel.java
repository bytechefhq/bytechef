package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.HelpModel;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-19T05:46:41.627972+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public class ClusterElementDefinitionBasicModel {

  private String componentName;

  private Integer componentVersion;

  private @Nullable String description;

  private @Nullable HelpModel help;

  private String name;

  private @Nullable String icon;

  private Boolean outputDefined;

  private @Nullable Boolean outputFunctionDefined;

  private @Nullable Boolean outputSchemaDefined;

  private @Nullable String title;

  private String type;

  public ClusterElementDefinitionBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ClusterElementDefinitionBasicModel(String componentName, Integer componentVersion, String name, Boolean outputDefined, String type) {
    this.componentName = componentName;
    this.componentVersion = componentVersion;
    this.name = name;
    this.outputDefined = outputDefined;
    this.type = type;
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

  public ClusterElementDefinitionBasicModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The cluster element description.
   * @return description
   */
  
  @Schema(name = "description", description = "The cluster element description.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public ClusterElementDefinitionBasicModel help(@Nullable HelpModel help) {
    this.help = help;
    return this;
  }

  /**
   * Get help
   * @return help
   */
  @Valid 
  @Schema(name = "help", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("help")
  public @Nullable HelpModel getHelp() {
    return help;
  }

  public void setHelp(@Nullable HelpModel help) {
    this.help = help;
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

  public ClusterElementDefinitionBasicModel icon(@Nullable String icon) {
    this.icon = icon;
    return this;
  }

  /**
   * The component icon.
   * @return icon
   */
  
  @Schema(name = "icon", description = "The component icon.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("icon")
  public @Nullable String getIcon() {
    return icon;
  }

  public void setIcon(@Nullable String icon) {
    this.icon = icon;
  }

  public ClusterElementDefinitionBasicModel outputDefined(Boolean outputDefined) {
    this.outputDefined = outputDefined;
    return this;
  }

  /**
   * Does action defines output.
   * @return outputDefined
   */
  @NotNull 
  @Schema(name = "outputDefined", description = "Does action defines output.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("outputDefined")
  public Boolean getOutputDefined() {
    return outputDefined;
  }

  public void setOutputDefined(Boolean outputDefined) {
    this.outputDefined = outputDefined;
  }

  public ClusterElementDefinitionBasicModel outputFunctionDefined(@Nullable Boolean outputFunctionDefined) {
    this.outputFunctionDefined = outputFunctionDefined;
    return this;
  }

  /**
   * Does action defines output function.
   * @return outputFunctionDefined
   */
  
  @Schema(name = "outputFunctionDefined", description = "Does action defines output function.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("outputFunctionDefined")
  public @Nullable Boolean getOutputFunctionDefined() {
    return outputFunctionDefined;
  }

  public void setOutputFunctionDefined(@Nullable Boolean outputFunctionDefined) {
    this.outputFunctionDefined = outputFunctionDefined;
  }

  public ClusterElementDefinitionBasicModel outputSchemaDefined(@Nullable Boolean outputSchemaDefined) {
    this.outputSchemaDefined = outputSchemaDefined;
    return this;
  }

  /**
   * Does action defines output schema.
   * @return outputSchemaDefined
   */
  
  @Schema(name = "outputSchemaDefined", description = "Does action defines output schema.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("outputSchemaDefined")
  public @Nullable Boolean getOutputSchemaDefined() {
    return outputSchemaDefined;
  }

  public void setOutputSchemaDefined(@Nullable Boolean outputSchemaDefined) {
    this.outputSchemaDefined = outputSchemaDefined;
  }

  public ClusterElementDefinitionBasicModel title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * The cluster element title.
   * @return title
   */
  
  @Schema(name = "title", description = "The cluster element title.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  public void setTitle(@Nullable String title) {
    this.title = title;
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
        Objects.equals(this.description, clusterElementDefinitionBasic.description) &&
        Objects.equals(this.help, clusterElementDefinitionBasic.help) &&
        Objects.equals(this.name, clusterElementDefinitionBasic.name) &&
        Objects.equals(this.icon, clusterElementDefinitionBasic.icon) &&
        Objects.equals(this.outputDefined, clusterElementDefinitionBasic.outputDefined) &&
        Objects.equals(this.outputFunctionDefined, clusterElementDefinitionBasic.outputFunctionDefined) &&
        Objects.equals(this.outputSchemaDefined, clusterElementDefinitionBasic.outputSchemaDefined) &&
        Objects.equals(this.title, clusterElementDefinitionBasic.title) &&
        Objects.equals(this.type, clusterElementDefinitionBasic.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, componentVersion, description, help, name, icon, outputDefined, outputFunctionDefined, outputSchemaDefined, title, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClusterElementDefinitionBasicModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    help: ").append(toIndentedString(help)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    outputDefined: ").append(toIndentedString(outputDefined)).append("\n");
    sb.append("    outputFunctionDefined: ").append(toIndentedString(outputFunctionDefined)).append("\n");
    sb.append("    outputSchemaDefined: ").append(toIndentedString(outputSchemaDefined)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

