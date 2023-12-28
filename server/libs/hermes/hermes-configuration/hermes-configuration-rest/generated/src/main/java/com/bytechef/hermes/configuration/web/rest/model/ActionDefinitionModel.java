package com.bytechef.hermes.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.configuration.web.rest.model.HelpModel;
import com.bytechef.hermes.configuration.web.rest.model.PropertyModel;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-12-28T18:24:23.377490+01:00[Europe/Zagreb]")
public class ActionDefinitionModel {

  private String componentName;

  private Integer componentVersion;

  private String description;

  private Boolean editorDescriptionDataSource;

  private HelpModel help;

  private String name;

  private PropertyModel outputSchema;

  private Boolean outputSchemaDataSource;

  @Valid
  private List<@Valid PropertyModel> properties;

  private Object sampleOutput;

  private Boolean sampleOutputDataSource;

  private String title;

  public ActionDefinitionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ActionDefinitionModel(String name) {
    this.name = name;
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

  public ActionDefinitionModel editorDescriptionDataSource(Boolean editorDescriptionDataSource) {
    this.editorDescriptionDataSource = editorDescriptionDataSource;
    return this;
  }

  /**
   * Does action has defined dynamic editor description.
   * @return editorDescriptionDataSource
  */
  
  @Schema(name = "editorDescriptionDataSource", description = "Does action has defined dynamic editor description.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("editorDescriptionDataSource")
  public Boolean getEditorDescriptionDataSource() {
    return editorDescriptionDataSource;
  }

  public void setEditorDescriptionDataSource(Boolean editorDescriptionDataSource) {
    this.editorDescriptionDataSource = editorDescriptionDataSource;
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

  public ActionDefinitionModel outputSchema(PropertyModel outputSchema) {
    this.outputSchema = outputSchema;
    return this;
  }

  /**
   * Get outputSchema
   * @return outputSchema
  */
  @Valid 
  @Schema(name = "outputSchema", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("outputSchema")
  public PropertyModel getOutputSchema() {
    return outputSchema;
  }

  public void setOutputSchema(PropertyModel outputSchema) {
    this.outputSchema = outputSchema;
  }

  public ActionDefinitionModel outputSchemaDataSource(Boolean outputSchemaDataSource) {
    this.outputSchemaDataSource = outputSchemaDataSource;
    return this;
  }

  /**
   * Does action has defined dynamic output schema.
   * @return outputSchemaDataSource
  */
  
  @Schema(name = "outputSchemaDataSource", description = "Does action has defined dynamic output schema.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("outputSchemaDataSource")
  public Boolean getOutputSchemaDataSource() {
    return outputSchemaDataSource;
  }

  public void setOutputSchemaDataSource(Boolean outputSchemaDataSource) {
    this.outputSchemaDataSource = outputSchemaDataSource;
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

  public ActionDefinitionModel sampleOutput(Object sampleOutput) {
    this.sampleOutput = sampleOutput;
    return this;
  }

  /**
   * The sample value of the action's output.
   * @return sampleOutput
  */
  
  @Schema(name = "sampleOutput", description = "The sample value of the action's output.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("sampleOutput")
  public Object getSampleOutput() {
    return sampleOutput;
  }

  public void setSampleOutput(Object sampleOutput) {
    this.sampleOutput = sampleOutput;
  }

  public ActionDefinitionModel sampleOutputDataSource(Boolean sampleOutputDataSource) {
    this.sampleOutputDataSource = sampleOutputDataSource;
    return this;
  }

  /**
   * Does action has defined dynamic sample output.
   * @return sampleOutputDataSource
  */
  
  @Schema(name = "sampleOutputDataSource", description = "Does action has defined dynamic sample output.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("sampleOutputDataSource")
  public Boolean getSampleOutputDataSource() {
    return sampleOutputDataSource;
  }

  public void setSampleOutputDataSource(Boolean sampleOutputDataSource) {
    this.sampleOutputDataSource = sampleOutputDataSource;
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
        Objects.equals(this.editorDescriptionDataSource, actionDefinition.editorDescriptionDataSource) &&
        Objects.equals(this.help, actionDefinition.help) &&
        Objects.equals(this.name, actionDefinition.name) &&
        Objects.equals(this.outputSchema, actionDefinition.outputSchema) &&
        Objects.equals(this.outputSchemaDataSource, actionDefinition.outputSchemaDataSource) &&
        Objects.equals(this.properties, actionDefinition.properties) &&
        Objects.equals(this.sampleOutput, actionDefinition.sampleOutput) &&
        Objects.equals(this.sampleOutputDataSource, actionDefinition.sampleOutputDataSource) &&
        Objects.equals(this.title, actionDefinition.title);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, componentVersion, description, editorDescriptionDataSource, help, name, outputSchema, outputSchemaDataSource, properties, sampleOutput, sampleOutputDataSource, title);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ActionDefinitionModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    editorDescriptionDataSource: ").append(toIndentedString(editorDescriptionDataSource)).append("\n");
    sb.append("    help: ").append(toIndentedString(help)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    outputSchema: ").append(toIndentedString(outputSchema)).append("\n");
    sb.append("    outputSchemaDataSource: ").append(toIndentedString(outputSchemaDataSource)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
    sb.append("    sampleOutput: ").append(toIndentedString(sampleOutput)).append("\n");
    sb.append("    sampleOutputDataSource: ").append(toIndentedString(sampleOutputDataSource)).append("\n");
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

