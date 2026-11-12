package com.bytechef.ee.embedded.execution.public_.web.rest.model;

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
 * A component trigger a connected user can subscribe to.
 */

@Schema(name = "Trigger", description = "A component trigger a connected user can subscribe to.")
@JsonTypeName("Trigger")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-12T00:36:32.396363+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class TriggerModel {

  private @Nullable String description;

  private Boolean hasDynamicOutput;

  private String inputSchema;

  private String name;

  private @Nullable String outputSchema;

  private String title;

  private String type;

  public TriggerModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TriggerModel(Boolean hasDynamicOutput, String inputSchema, String name, String title, String type) {
    this.hasDynamicOutput = hasDynamicOutput;
    this.inputSchema = inputSchema;
    this.name = name;
    this.title = title;
    this.type = type;
  }

  public TriggerModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of what the trigger does.
   * @return description
   */
  
  @Schema(name = "description", description = "The description of what the trigger does.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public TriggerModel hasDynamicOutput(Boolean hasDynamicOutput) {
    this.hasDynamicOutput = hasDynamicOutput;
    return this;
  }

  /**
   * Indicates that the structure of the output depends on the input.
   * @return hasDynamicOutput
   */
  @NotNull 
  @Schema(name = "hasDynamicOutput", description = "Indicates that the structure of the output depends on the input.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("hasDynamicOutput")
  public Boolean getHasDynamicOutput() {
    return hasDynamicOutput;
  }

  @JsonProperty("hasDynamicOutput")
  public void setHasDynamicOutput(Boolean hasDynamicOutput) {
    this.hasDynamicOutput = hasDynamicOutput;
  }

  public TriggerModel inputSchema(String inputSchema) {
    this.inputSchema = inputSchema;
    return this;
  }

  /**
   * JSON Schema for the trigger input parameters.
   * @return inputSchema
   */
  @NotNull 
  @Schema(name = "inputSchema", description = "JSON Schema for the trigger input parameters.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("inputSchema")
  public String getInputSchema() {
    return inputSchema;
  }

  @JsonProperty("inputSchema")
  public void setInputSchema(String inputSchema) {
    this.inputSchema = inputSchema;
  }

  public TriggerModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The programmatic name of the trigger, used when creating a trigger subscription.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The programmatic name of the trigger, used when creating a trigger subscription.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public TriggerModel outputSchema(@Nullable String outputSchema) {
    this.outputSchema = outputSchema;
    return this;
  }

  /**
   * JSON Schema for the trigger output, when the output structure is static and an object.
   * @return outputSchema
   */
  
  @Schema(name = "outputSchema", description = "JSON Schema for the trigger output, when the output structure is static and an object.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("outputSchema")
  public @Nullable String getOutputSchema() {
    return outputSchema;
  }

  @JsonProperty("outputSchema")
  public void setOutputSchema(@Nullable String outputSchema) {
    this.outputSchema = outputSchema;
  }

  public TriggerModel title(String title) {
    this.title = title;
    return this;
  }

  /**
   * The full title of the trigger.
   * @return title
   */
  @NotNull 
  @Schema(name = "title", description = "The full title of the trigger.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  public TriggerModel type(String type) {
    this.type = type;
    return this;
  }

  /**
   * The type of the trigger: STATIC_WEBHOOK, DYNAMIC_WEBHOOK, POLLING, HYBRID, LISTENER or CALLABLE.
   * @return type
   */
  @NotNull 
  @Schema(name = "type", description = "The type of the trigger: STATIC_WEBHOOK, DYNAMIC_WEBHOOK, POLLING, HYBRID, LISTENER or CALLABLE.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  @JsonProperty("type")
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
    TriggerModel trigger = (TriggerModel) o;
    return Objects.equals(this.description, trigger.description) &&
        Objects.equals(this.hasDynamicOutput, trigger.hasDynamicOutput) &&
        Objects.equals(this.inputSchema, trigger.inputSchema) &&
        Objects.equals(this.name, trigger.name) &&
        Objects.equals(this.outputSchema, trigger.outputSchema) &&
        Objects.equals(this.title, trigger.title) &&
        Objects.equals(this.type, trigger.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, hasDynamicOutput, inputSchema, name, outputSchema, title, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TriggerModel {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    hasDynamicOutput: ").append(toIndentedString(hasDynamicOutput)).append("\n");
    sb.append("    inputSchema: ").append(toIndentedString(inputSchema)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    outputSchema: ").append(toIndentedString(outputSchema)).append("\n");
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
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

