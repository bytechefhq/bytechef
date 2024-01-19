package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.PropertyModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ComponentOutputSchemaModel
 */

@JsonTypeName("ComponentOutputSchema")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-01-19T11:58:57.058637+01:00[Europe/Zagreb]")
public class ComponentOutputSchemaModel {

  private PropertyModel definition;

  private Object sampleOutput;

  public ComponentOutputSchemaModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ComponentOutputSchemaModel(PropertyModel definition, Object sampleOutput) {
    this.definition = definition;
    this.sampleOutput = sampleOutput;
  }

  public ComponentOutputSchemaModel definition(PropertyModel definition) {
    this.definition = definition;
    return this;
  }

  /**
   * Get definition
   * @return definition
  */
  @NotNull @Valid 
  @Schema(name = "definition", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("definition")
  public PropertyModel getDefinition() {
    return definition;
  }

  public void setDefinition(PropertyModel definition) {
    this.definition = definition;
  }

  public ComponentOutputSchemaModel sampleOutput(Object sampleOutput) {
    this.sampleOutput = sampleOutput;
    return this;
  }

  /**
   * The sample value of the action's/trigger's output.
   * @return sampleOutput
  */
  @NotNull 
  @Schema(name = "sampleOutput", description = "The sample value of the action's/trigger's output.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("sampleOutput")
  public Object getSampleOutput() {
    return sampleOutput;
  }

  public void setSampleOutput(Object sampleOutput) {
    this.sampleOutput = sampleOutput;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComponentOutputSchemaModel componentOutputSchema = (ComponentOutputSchemaModel) o;
    return Objects.equals(this.definition, componentOutputSchema.definition) &&
        Objects.equals(this.sampleOutput, componentOutputSchema.sampleOutput);
  }

  @Override
  public int hashCode() {
    return Objects.hash(definition, sampleOutput);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComponentOutputSchemaModel {\n");
    sb.append("    definition: ").append(toIndentedString(definition)).append("\n");
    sb.append("    sampleOutput: ").append(toIndentedString(sampleOutput)).append("\n");
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

