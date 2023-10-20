package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.DisplayModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ResourcesModel;
import com.bytechef.hermes.definition.registry.web.rest.model.TriggerTypeModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-15T19:47:32.550589+02:00[Europe/Zagreb]")
public class TriggerDefinitionModel {

  private DisplayModel display;

  private Object exampleOutput;

  private String name;

  @Valid
  private List<@Valid PropertyModel> outputSchema;

  @Valid
  private List<@Valid PropertyModel> properties;

  private ResourcesModel resources;

  private TriggerTypeModel type;

  /**
   * Default constructor
   * @deprecated Use {@link TriggerDefinitionModel#TriggerDefinitionModel(DisplayModel, String, TriggerTypeModel)}
   */
  @Deprecated
  public TriggerDefinitionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TriggerDefinitionModel(DisplayModel display, String name, TriggerTypeModel type) {
    this.display = display;
    this.name = name;
    this.type = type;
  }

  public TriggerDefinitionModel display(DisplayModel display) {
    this.display = display;
    return this;
  }

  /**
   * Get display
   * @return display
  */
  @NotNull @Valid 
  @Schema(name = "display", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("display")
  public DisplayModel getDisplay() {
    return display;
  }

  public void setDisplay(DisplayModel display) {
    this.display = display;
  }

  public TriggerDefinitionModel exampleOutput(Object exampleOutput) {
    this.exampleOutput = exampleOutput;
    return this;
  }

  /**
   * The example value of the action's output.
   * @return exampleOutput
  */
  
  @Schema(name = "exampleOutput", description = "The example value of the action's output.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("exampleOutput")
  public Object getExampleOutput() {
    return exampleOutput;
  }

  public void setExampleOutput(Object exampleOutput) {
    this.exampleOutput = exampleOutput;
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

  public TriggerDefinitionModel outputSchema(List<@Valid PropertyModel> outputSchema) {
    this.outputSchema = outputSchema;
    return this;
  }

  public TriggerDefinitionModel addOutputSchemaItem(PropertyModel outputSchemaItem) {
    if (this.outputSchema == null) {
      this.outputSchema = new ArrayList<>();
    }
    this.outputSchema.add(outputSchemaItem);
    return this;
  }

  /**
   * The output schema of an execution result.
   * @return outputSchema
  */
  @Valid 
  @Schema(name = "outputSchema", description = "The output schema of an execution result.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("outputSchema")
  public List<@Valid PropertyModel> getOutputSchema() {
    return outputSchema;
  }

  public void setOutputSchema(List<@Valid PropertyModel> outputSchema) {
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

  public TriggerDefinitionModel resources(ResourcesModel resources) {
    this.resources = resources;
    return this;
  }

  /**
   * Get resources
   * @return resources
  */
  @Valid 
  @Schema(name = "resources", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("resources")
  public ResourcesModel getResources() {
    return resources;
  }

  public void setResources(ResourcesModel resources) {
    this.resources = resources;
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
    return Objects.equals(this.display, triggerDefinition.display) &&
        Objects.equals(this.exampleOutput, triggerDefinition.exampleOutput) &&
        Objects.equals(this.name, triggerDefinition.name) &&
        Objects.equals(this.outputSchema, triggerDefinition.outputSchema) &&
        Objects.equals(this.properties, triggerDefinition.properties) &&
        Objects.equals(this.resources, triggerDefinition.resources) &&
        Objects.equals(this.type, triggerDefinition.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(display, exampleOutput, name, outputSchema, properties, resources, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TriggerDefinitionModel {\n");
    sb.append("    display: ").append(toIndentedString(display)).append("\n");
    sb.append("    exampleOutput: ").append(toIndentedString(exampleOutput)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    outputSchema: ").append(toIndentedString(outputSchema)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
    sb.append("    resources: ").append(toIndentedString(resources)).append("\n");
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

