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
 * An action the calling app may execute through a bound connection.
 */

@Schema(name = "AppAction", description = "An action the calling app may execute through a bound connection.")
@JsonTypeName("AppAction")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:31.849354+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class AppActionModel {

  private String bindingKey;

  private String componentName;

  private String actionName;

  private JsonNullable<String> description = JsonNullable.<String>undefined();

  private Map<String, Object> inputSchema = new HashMap<>();

  public AppActionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AppActionModel(String bindingKey, String componentName, String actionName, Map<String, Object> inputSchema) {
    this.bindingKey = bindingKey;
    this.componentName = componentName;
    this.actionName = actionName;
    this.inputSchema = inputSchema;
  }

  public AppActionModel bindingKey(String bindingKey) {
    this.bindingKey = bindingKey;
    return this;
  }

  /**
   * Get bindingKey
   * @return bindingKey
   */
  @NotNull 
  @Schema(name = "bindingKey", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("bindingKey")
  public String getBindingKey() {
    return bindingKey;
  }

  @JsonProperty("bindingKey")
  public void setBindingKey(String bindingKey) {
    this.bindingKey = bindingKey;
  }

  public AppActionModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * Get componentName
   * @return componentName
   */
  @NotNull 
  @Schema(name = "componentName", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  @JsonProperty("componentName")
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public AppActionModel actionName(String actionName) {
    this.actionName = actionName;
    return this;
  }

  /**
   * Get actionName
   * @return actionName
   */
  @NotNull 
  @Schema(name = "actionName", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("actionName")
  public String getActionName() {
    return actionName;
  }

  @JsonProperty("actionName")
  public void setActionName(String actionName) {
    this.actionName = actionName;
  }

  public AppActionModel description(String description) {
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

  public AppActionModel inputSchema(Map<String, Object> inputSchema) {
    this.inputSchema = inputSchema;
    return this;
  }

  public AppActionModel putInputSchemaItem(String key, Object inputSchemaItem) {
    if (this.inputSchema == null) {
      this.inputSchema = new HashMap<>();
    }
    this.inputSchema.put(key, inputSchemaItem);
    return this;
  }

  /**
   * JSON Schema of the action input, generated from the component's full property definition. Properties whose shape depends on the connection appear as open objects.
   * @return inputSchema
   */
  @NotNull 
  @Schema(name = "inputSchema", description = "JSON Schema of the action input, generated from the component's full property definition. Properties whose shape depends on the connection appear as open objects.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("inputSchema")
  public Map<String, Object> getInputSchema() {
    return inputSchema;
  }

  @JsonProperty("inputSchema")
  public void setInputSchema(Map<String, Object> inputSchema) {
    this.inputSchema = inputSchema;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppActionModel appAction = (AppActionModel) o;
    return Objects.equals(this.bindingKey, appAction.bindingKey) &&
        Objects.equals(this.componentName, appAction.componentName) &&
        Objects.equals(this.actionName, appAction.actionName) &&
        equalsNullable(this.description, appAction.description) &&
        Objects.equals(this.inputSchema, appAction.inputSchema);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(bindingKey, componentName, actionName, hashCodeNullable(description), inputSchema);
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
    sb.append("class AppActionModel {\n");
    sb.append("    bindingKey: ").append(toIndentedString(bindingKey)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    actionName: ").append(toIndentedString(actionName)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    inputSchema: ").append(toIndentedString(inputSchema)).append("\n");
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

