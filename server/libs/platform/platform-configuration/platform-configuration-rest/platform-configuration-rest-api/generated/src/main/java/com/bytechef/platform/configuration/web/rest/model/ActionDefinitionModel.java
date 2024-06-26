package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.HelpModel;
import com.bytechef.platform.configuration.web.rest.model.PropertyModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * An action is a portion of reusable code that accomplish a specific task. When building a workflow, each action is represented as a task inside the workflow. The task &#39;type&#39; property is defined as [component name]/v[component version]/[action name]. Action properties are used to set properties of the task inside the workflow.
 */

@Schema(name = "ActionDefinition", description = "An action is a portion of reusable code that accomplish a specific task. When building a workflow, each action is represented as a task inside the workflow. The task 'type' property is defined as [component name]/v[component version]/[action name]. Action properties are used to set properties of the task inside the workflow.")
@JsonTypeName("ActionDefinition")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-26T11:09:40.503215+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public class ActionDefinitionModel {

  private String componentName;

  private Integer componentVersion;

  private String description;

  private Boolean workflowNodeDescriptionDefined;

  private HelpModel help;

  private String name;

  private Boolean outputDefined;

  private Boolean outputFunctionDefined;

  @Valid
  private List<@Valid PropertyModel> properties = new ArrayList<>();

  private String title;

  public ActionDefinitionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ActionDefinitionModel(String name, Boolean outputDefined, Boolean outputFunctionDefined) {
    this.name = name;
    this.outputDefined = outputDefined;
    this.outputFunctionDefined = outputFunctionDefined;
  }

  public ActionDefinitionModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The component name.
   * @return componentName
  */
  
  @Schema(name = "componentName", description = "The component name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public ActionDefinitionModel componentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * The component version.
   * @return componentVersion
  */
  
  @Schema(name = "componentVersion", description = "The component version.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentVersion")
  public Integer getComponentVersion() {
    return componentVersion;
  }

  public void setComponentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  public ActionDefinitionModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The description.
   * @return description
  */
  
  @Schema(name = "description", description = "The description.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ActionDefinitionModel workflowNodeDescriptionDefined(Boolean workflowNodeDescriptionDefined) {
    this.workflowNodeDescriptionDefined = workflowNodeDescriptionDefined;
    return this;
  }

  /**
   * Does action define dynamic node description.
   * @return workflowNodeDescriptionDefined
  */
  
  @Schema(name = "workflowNodeDescriptionDefined", description = "Does action define dynamic node description.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowNodeDescriptionDefined")
  public Boolean getWorkflowNodeDescriptionDefined() {
    return workflowNodeDescriptionDefined;
  }

  public void setWorkflowNodeDescriptionDefined(Boolean workflowNodeDescriptionDefined) {
    this.workflowNodeDescriptionDefined = workflowNodeDescriptionDefined;
  }

  public ActionDefinitionModel help(HelpModel help) {
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
  public HelpModel getHelp() {
    return help;
  }

  public void setHelp(HelpModel help) {
    this.help = help;
  }

  public ActionDefinitionModel name(String name) {
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

  public ActionDefinitionModel outputDefined(Boolean outputDefined) {
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

  public ActionDefinitionModel outputFunctionDefined(Boolean outputFunctionDefined) {
    this.outputFunctionDefined = outputFunctionDefined;
    return this;
  }

  /**
   * Does action define dynamic output schema.
   * @return outputFunctionDefined
  */
  @NotNull 
  @Schema(name = "outputFunctionDefined", description = "Does action define dynamic output schema.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("outputFunctionDefined")
  public Boolean getOutputFunctionDefined() {
    return outputFunctionDefined;
  }

  public void setOutputFunctionDefined(Boolean outputFunctionDefined) {
    this.outputFunctionDefined = outputFunctionDefined;
  }

  public ActionDefinitionModel properties(List<@Valid PropertyModel> properties) {
    this.properties = properties;
    return this;
  }

  public ActionDefinitionModel addPropertiesItem(PropertyModel propertiesItem) {
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

  public ActionDefinitionModel title(String title) {
    this.title = title;
    return this;
  }

  /**
   * The title
   * @return title
  */
  
  @Schema(name = "title", description = "The title", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ActionDefinitionModel actionDefinition = (ActionDefinitionModel) o;
    return Objects.equals(this.componentName, actionDefinition.componentName) &&
        Objects.equals(this.componentVersion, actionDefinition.componentVersion) &&
        Objects.equals(this.description, actionDefinition.description) &&
        Objects.equals(this.workflowNodeDescriptionDefined, actionDefinition.workflowNodeDescriptionDefined) &&
        Objects.equals(this.help, actionDefinition.help) &&
        Objects.equals(this.name, actionDefinition.name) &&
        Objects.equals(this.outputDefined, actionDefinition.outputDefined) &&
        Objects.equals(this.outputFunctionDefined, actionDefinition.outputFunctionDefined) &&
        Objects.equals(this.properties, actionDefinition.properties) &&
        Objects.equals(this.title, actionDefinition.title);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, componentVersion, description, workflowNodeDescriptionDefined, help, name, outputDefined, outputFunctionDefined, properties, title);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ActionDefinitionModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    workflowNodeDescriptionDefined: ").append(toIndentedString(workflowNodeDescriptionDefined)).append("\n");
    sb.append("    help: ").append(toIndentedString(help)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    outputDefined: ").append(toIndentedString(outputDefined)).append("\n");
    sb.append("    outputFunctionDefined: ").append(toIndentedString(outputFunctionDefined)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
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

