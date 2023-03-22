package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.DisplayModel;
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
 * An action is a portion of reusable code that accomplish a specific task. When building a workflow, each action is represented as a task inside the workflow. The task &#39;type&#39; property is defined as [component name]/v[component version]/[action name]. Action properties are used to set properties of the task inside the workflow.
 */

@Schema(name = "ActionDefinitionBasic", description = "An action is a portion of reusable code that accomplish a specific task. When building a workflow, each action is represented as a task inside the workflow. The task 'type' property is defined as [component name]/v[component version]/[action name]. Action properties are used to set properties of the task inside the workflow.")
@JsonTypeName("ActionDefinitionBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-22T18:48:16.972666+01:00[Europe/Zagreb]")
public class ActionDefinitionBasicModel {

  @JsonProperty("name")
  private String name;

  @JsonProperty("display")
  private DisplayModel display;

  public ActionDefinitionBasicModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The action name.
   * @return name
  */
  @NotNull 
  @Schema(name = "name", description = "The action name.", requiredMode = Schema.RequiredMode.REQUIRED)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ActionDefinitionBasicModel display(DisplayModel display) {
    this.display = display;
    return this;
  }

  /**
   * Get display
   * @return display
  */
  @NotNull @Valid 
  @Schema(name = "display", requiredMode = Schema.RequiredMode.REQUIRED)
  public DisplayModel getDisplay() {
    return display;
  }

  public void setDisplay(DisplayModel display) {
    this.display = display;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ActionDefinitionBasicModel actionDefinitionBasic = (ActionDefinitionBasicModel) o;
    return Objects.equals(this.name, actionDefinitionBasic.name) &&
        Objects.equals(this.display, actionDefinitionBasic.display);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, display);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ActionDefinitionBasicModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    display: ").append(toIndentedString(display)).append("\n");
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

