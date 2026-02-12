package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.HelpModel;
import com.bytechef.platform.configuration.web.rest.model.TriggerTypeModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A trigger definition defines ways to trigger workflows from the outside services.
 */

@Schema(name = "TriggerDefinitionBasic", description = "A trigger definition defines ways to trigger workflows from the outside services.")
@JsonTypeName("TriggerDefinitionBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-12T08:43:42.870402320+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public class TriggerDefinitionBasicModel {

  private String componentName;

  private Integer componentVersion;

  private @Nullable String description;

  private @Nullable HelpModel help;

  private String name;

  private Boolean outputDefined;

  private Boolean outputFunctionDefined;

  private @Nullable Boolean outputSchemaDefined;

  private @Nullable String title;

  private TriggerTypeModel type;

  public TriggerDefinitionBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TriggerDefinitionBasicModel(String componentName, Integer componentVersion, String name, Boolean outputDefined, Boolean outputFunctionDefined, TriggerTypeModel type) {
    this.componentName = componentName;
    this.componentVersion = componentVersion;
    this.name = name;
    this.outputDefined = outputDefined;
    this.outputFunctionDefined = outputFunctionDefined;
    this.type = type;
  }

  public TriggerDefinitionBasicModel componentName(String componentName) {
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

  public TriggerDefinitionBasicModel componentVersion(Integer componentVersion) {
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

  public TriggerDefinitionBasicModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description.
   * @return description
   */
  
  @Schema(name = "description", description = "The description.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public TriggerDefinitionBasicModel help(@Nullable HelpModel help) {
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

  public TriggerDefinitionBasicModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The action name.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The action name.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public TriggerDefinitionBasicModel outputDefined(Boolean outputDefined) {
    this.outputDefined = outputDefined;
    return this;
  }

  /**
   * Does trigger defines output.
   * @return outputDefined
   */
  @NotNull 
  @Schema(name = "outputDefined", description = "Does trigger defines output.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("outputDefined")
  public Boolean getOutputDefined() {
    return outputDefined;
  }

  public void setOutputDefined(Boolean outputDefined) {
    this.outputDefined = outputDefined;
  }

  public TriggerDefinitionBasicModel outputFunctionDefined(Boolean outputFunctionDefined) {
    this.outputFunctionDefined = outputFunctionDefined;
    return this;
  }

  /**
   * Does trigger defines output function.
   * @return outputFunctionDefined
   */
  @NotNull 
  @Schema(name = "outputFunctionDefined", description = "Does trigger defines output function.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("outputFunctionDefined")
  public Boolean getOutputFunctionDefined() {
    return outputFunctionDefined;
  }

  public void setOutputFunctionDefined(Boolean outputFunctionDefined) {
    this.outputFunctionDefined = outputFunctionDefined;
  }

  public TriggerDefinitionBasicModel outputSchemaDefined(@Nullable Boolean outputSchemaDefined) {
    this.outputSchemaDefined = outputSchemaDefined;
    return this;
  }

  /**
   * Does trigger defines output schema.
   * @return outputSchemaDefined
   */
  
  @Schema(name = "outputSchemaDefined", description = "Does trigger defines output schema.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("outputSchemaDefined")
  public @Nullable Boolean getOutputSchemaDefined() {
    return outputSchemaDefined;
  }

  public void setOutputSchemaDefined(@Nullable Boolean outputSchemaDefined) {
    this.outputSchemaDefined = outputSchemaDefined;
  }

  public TriggerDefinitionBasicModel title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * The title
   * @return title
   */
  
  @Schema(name = "title", description = "The title", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public TriggerDefinitionBasicModel type(TriggerTypeModel type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull @Valid 
  @Schema(name = "type", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public TriggerTypeModel getType() {
    return type;
  }

  public void setType(TriggerTypeModel type) {
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
    TriggerDefinitionBasicModel triggerDefinitionBasic = (TriggerDefinitionBasicModel) o;
    return Objects.equals(this.componentName, triggerDefinitionBasic.componentName) &&
        Objects.equals(this.componentVersion, triggerDefinitionBasic.componentVersion) &&
        Objects.equals(this.description, triggerDefinitionBasic.description) &&
        Objects.equals(this.help, triggerDefinitionBasic.help) &&
        Objects.equals(this.name, triggerDefinitionBasic.name) &&
        Objects.equals(this.outputDefined, triggerDefinitionBasic.outputDefined) &&
        Objects.equals(this.outputFunctionDefined, triggerDefinitionBasic.outputFunctionDefined) &&
        Objects.equals(this.outputSchemaDefined, triggerDefinitionBasic.outputSchemaDefined) &&
        Objects.equals(this.title, triggerDefinitionBasic.title) &&
        Objects.equals(this.type, triggerDefinitionBasic.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, componentVersion, description, help, name, outputDefined, outputFunctionDefined, outputSchemaDefined, title, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TriggerDefinitionBasicModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    help: ").append(toIndentedString(help)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

