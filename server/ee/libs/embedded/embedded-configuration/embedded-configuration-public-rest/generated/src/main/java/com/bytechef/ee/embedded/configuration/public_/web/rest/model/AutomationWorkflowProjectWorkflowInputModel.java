package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

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
 * A value the connected user supplies before an automation workflow runs.
 */

@Schema(name = "AutomationWorkflowProjectWorkflowInput", description = "A value the connected user supplies before an automation workflow runs.")
@JsonTypeName("AutomationWorkflowProjectWorkflowInput")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-23T21:34:31.743535+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class AutomationWorkflowProjectWorkflowInputModel {

  private @Nullable String name;

  private @Nullable String label;

  private @Nullable String type;

  private @Nullable Boolean required;

  public AutomationWorkflowProjectWorkflowInputModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the input, and the key its value is stored under.
   * @return name
   */
  
  @Schema(name = "name", description = "The name of the input, and the key its value is stored under.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public AutomationWorkflowProjectWorkflowInputModel label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * The label shown to the connected user.
   * @return label
   */
  
  @Schema(name = "label", description = "The label shown to the connected user.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(@Nullable String label) {
    this.label = label;
  }

  public AutomationWorkflowProjectWorkflowInputModel type(@Nullable String type) {
    this.type = type;
    return this;
  }

  /**
   * The input type, e.g. STRING, NUMBER or BOOLEAN.
   * @return type
   */
  
  @Schema(name = "type", description = "The input type, e.g. STRING, NUMBER or BOOLEAN.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("type")
  public @Nullable String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(@Nullable String type) {
    this.type = type;
  }

  public AutomationWorkflowProjectWorkflowInputModel required(@Nullable Boolean required) {
    this.required = required;
    return this;
  }

  /**
   * Whether a value must be supplied.
   * @return required
   */
  
  @Schema(name = "required", description = "Whether a value must be supplied.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("required")
  public @Nullable Boolean getRequired() {
    return required;
  }

  @JsonProperty("required")
  public void setRequired(@Nullable Boolean required) {
    this.required = required;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AutomationWorkflowProjectWorkflowInputModel automationWorkflowProjectWorkflowInput = (AutomationWorkflowProjectWorkflowInputModel) o;
    return Objects.equals(this.name, automationWorkflowProjectWorkflowInput.name) &&
        Objects.equals(this.label, automationWorkflowProjectWorkflowInput.label) &&
        Objects.equals(this.type, automationWorkflowProjectWorkflowInput.type) &&
        Objects.equals(this.required, automationWorkflowProjectWorkflowInput.required);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, label, type, required);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AutomationWorkflowProjectWorkflowInputModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    required: ").append(toIndentedString(required)).append("\n");
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

