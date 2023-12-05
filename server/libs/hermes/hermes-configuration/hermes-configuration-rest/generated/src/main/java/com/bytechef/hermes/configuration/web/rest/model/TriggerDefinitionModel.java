package com.bytechef.hermes.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.configuration.web.rest.model.HelpModel;
import com.bytechef.hermes.configuration.web.rest.model.PropertyModel;
import com.bytechef.hermes.configuration.web.rest.model.TriggerTypeModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * A trigger definition defines ways to trigger workflows from the outside services.
 */

@Schema(name = "TriggerDefinition", description = "A trigger definition defines ways to trigger workflows from the outside services.")
@JsonTypeName("TriggerDefinition")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-12-04T07:58:17.397618+01:00[Europe/Zagreb]")
public class TriggerDefinitionModel {

  private String componentName;

  private Integer componentVersion;

  private String description;

  private Object sampleOutput;

  private HelpModel help;

  private String name;

  private PropertyModel outputSchema;

  @Valid
  private List<@Valid PropertyModel> properties;

  private String title;

  private TriggerTypeModel type;

  public TriggerDefinitionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TriggerDefinitionModel(String name, TriggerTypeModel type) {
    this.name = name;
    this.type = type;
  }

  public TriggerDefinitionModel componentName(String componentName) {
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

  public TriggerDefinitionModel componentVersion(Integer componentVersion) {
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

  public TriggerDefinitionModel description(String description) {
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

  public TriggerDefinitionModel sampleOutput(Object sampleOutput) {
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

  public TriggerDefinitionModel help(HelpModel help) {
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

  public TriggerDefinitionModel name(String name) {
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

  public TriggerDefinitionModel outputSchema(PropertyModel outputSchema) {
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

  public TriggerDefinitionModel properties(List<@Valid PropertyModel> properties) {
    this.properties = properties;
    return this;
  }

  public TriggerDefinitionModel addPropertiesItem(PropertyModel propertiesItem) {
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

  public TriggerDefinitionModel title(String title) {
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

  public TriggerDefinitionModel type(TriggerTypeModel type) {
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
    TriggerDefinitionModel triggerDefinition = (TriggerDefinitionModel) o;
    return Objects.equals(this.componentName, triggerDefinition.componentName) &&
        Objects.equals(this.componentVersion, triggerDefinition.componentVersion) &&
        Objects.equals(this.description, triggerDefinition.description) &&
        Objects.equals(this.sampleOutput, triggerDefinition.sampleOutput) &&
        Objects.equals(this.help, triggerDefinition.help) &&
        Objects.equals(this.name, triggerDefinition.name) &&
        Objects.equals(this.outputSchema, triggerDefinition.outputSchema) &&
        Objects.equals(this.properties, triggerDefinition.properties) &&
        Objects.equals(this.title, triggerDefinition.title) &&
        Objects.equals(this.type, triggerDefinition.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, componentVersion, description, sampleOutput, help, name, outputSchema, properties, title, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TriggerDefinitionModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    sampleOutput: ").append(toIndentedString(sampleOutput)).append("\n");
    sb.append("    help: ").append(toIndentedString(help)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    outputSchema: ").append(toIndentedString(outputSchema)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
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

