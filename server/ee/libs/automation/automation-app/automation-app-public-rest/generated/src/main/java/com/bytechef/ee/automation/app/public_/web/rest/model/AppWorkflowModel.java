package com.bytechef.ee.automation.app.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.lang.Nullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A workflow bound to the calling app.
 */

@Schema(name = "AppWorkflow", description = "A workflow bound to the calling app.")
@JsonTypeName("AppWorkflow")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:31.849354+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class AppWorkflowModel {

  private String workflowKey;

  private String name;

  private JsonNullable<String> description = JsonNullable.<String>undefined();

  private JsonNullable<Map<String, Object>> inputSchema = JsonNullable.<Map<String, Object>>undefined();

  private JsonNullable<Map<String, Object>> outputSchema = JsonNullable.<Map<String, Object>>undefined();

  private Boolean available;

  public AppWorkflowModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AppWorkflowModel(String workflowKey, String name, Boolean available) {
    this.workflowKey = workflowKey;
    this.name = name;
    this.available = available;
  }

  public AppWorkflowModel workflowKey(String workflowKey) {
    this.workflowKey = workflowKey;
    return this;
  }

  /**
   * Get workflowKey
   * @return workflowKey
   */
  @NotNull 
  @Schema(name = "workflowKey", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workflowKey")
  public String getWorkflowKey() {
    return workflowKey;
  }

  @JsonProperty("workflowKey")
  public void setWorkflowKey(String workflowKey) {
    this.workflowKey = workflowKey;
  }

  public AppWorkflowModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public AppWorkflowModel description(String description) {
    this.description = JsonNullable.of(description);
    return this;
  }

  /**
   * Get description
   * @return description
   */
  
  @Schema(name = "description", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("description")
  public JsonNullable<String> getDescription() {
    return description;
  }

  public void setDescription(JsonNullable<String> description) {
    this.description = description;
  }

  public AppWorkflowModel inputSchema(Map<String, Object> inputSchema) {
    this.inputSchema = JsonNullable.of(inputSchema);
    return this;
  }

  public AppWorkflowModel putInputSchemaItem(String key, Object inputSchemaItem) {
    if (this.inputSchema == null || !this.inputSchema.isPresent() || this.inputSchema.get() == null) {
      this.inputSchema = JsonNullable.of(new HashMap<>());
    }
    this.inputSchema.get().put(key, inputSchemaItem);
    return this;
  }

  /**
   * JSON Schema of the run input, as declared by the workflow's Call workflow trigger.
   * @return inputSchema
   */
  
  @Schema(name = "inputSchema", description = "JSON Schema of the run input, as declared by the workflow's Call workflow trigger.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("inputSchema")
  public JsonNullable<Map<String, Object>> getInputSchema() {
    return inputSchema;
  }

  public void setInputSchema(JsonNullable<Map<String, Object>> inputSchema) {
    this.inputSchema = inputSchema;
  }

  public AppWorkflowModel outputSchema(Map<String, Object> outputSchema) {
    this.outputSchema = JsonNullable.of(outputSchema);
    return this;
  }

  public AppWorkflowModel putOutputSchemaItem(String key, Object outputSchemaItem) {
    if (this.outputSchema == null || !this.outputSchema.isPresent() || this.outputSchema.get() == null) {
      this.outputSchema = JsonNullable.of(new HashMap<>());
    }
    this.outputSchema.get().put(key, outputSchemaItem);
    return this;
  }

  /**
   * JSON Schema of the result, as declared by the workflow's Respond to workflow call step.
   * @return outputSchema
   */
  
  @Schema(name = "outputSchema", description = "JSON Schema of the result, as declared by the workflow's Respond to workflow call step.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("outputSchema")
  public JsonNullable<Map<String, Object>> getOutputSchema() {
    return outputSchema;
  }

  public void setOutputSchema(JsonNullable<Map<String, Object>> outputSchema) {
    this.outputSchema = outputSchema;
  }

  public AppWorkflowModel available(Boolean available) {
    this.available = available;
    return this;
  }

  /**
   * False when the deployment or workflow is gone or disabled; runs then fail with 409.
   * @return available
   */
  @NotNull 
  @Schema(name = "available", description = "False when the deployment or workflow is gone or disabled; runs then fail with 409.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("available")
  public Boolean getAvailable() {
    return available;
  }

  @JsonProperty("available")
  public void setAvailable(Boolean available) {
    this.available = available;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppWorkflowModel appWorkflow = (AppWorkflowModel) o;
    return Objects.equals(this.workflowKey, appWorkflow.workflowKey) &&
        Objects.equals(this.name, appWorkflow.name) &&
        equalsNullable(this.description, appWorkflow.description) &&
        equalsNullable(this.inputSchema, appWorkflow.inputSchema) &&
        equalsNullable(this.outputSchema, appWorkflow.outputSchema) &&
        Objects.equals(this.available, appWorkflow.available);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(workflowKey, name, hashCodeNullable(description), hashCodeNullable(inputSchema), hashCodeNullable(outputSchema), available);
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppWorkflowModel {\n");
    sb.append("    workflowKey: ").append(toIndentedString(workflowKey)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    inputSchema: ").append(toIndentedString(inputSchema)).append("\n");
    sb.append("    outputSchema: ").append(toIndentedString(outputSchema)).append("\n");
    sb.append("    available: ").append(toIndentedString(available)).append("\n");
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

