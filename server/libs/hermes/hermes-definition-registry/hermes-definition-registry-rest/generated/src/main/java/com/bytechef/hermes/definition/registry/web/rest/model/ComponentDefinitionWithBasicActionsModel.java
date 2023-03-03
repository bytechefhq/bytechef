package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.ActionDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ConnectionDefinitionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.DisplayModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ResourcesModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
 * A component contains a set of reusable code(actions) that accomplish specific tasks, triggers(TODO) and connections if there is a need for a connection to an outside service.
 */

@Schema(name = "ComponentDefinitionWithBasicActions", description = "A component contains a set of reusable code(actions) that accomplish specific tasks, triggers(TODO) and connections if there is a need for a connection to an outside service.")
@JsonTypeName("ComponentDefinitionWithBasicActions")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-02T18:38:21.432374+01:00[Europe/Zagreb]")
public class ComponentDefinitionWithBasicActionsModel {

  @JsonProperty("actions")
  @Valid
  private List<ActionDefinitionBasicModel> actions = new ArrayList<>();

  @JsonProperty("connection")
  private ConnectionDefinitionModel connection;

  @JsonProperty("display")
  private DisplayModel display;

  @JsonProperty("name")
  private String name;

  @JsonProperty("resources")
  private ResourcesModel resources;

  @JsonProperty("version")
  private Integer version;

  public ComponentDefinitionWithBasicActionsModel actions(List<ActionDefinitionBasicModel> actions) {
    this.actions = actions;
    return this;
  }

  public ComponentDefinitionWithBasicActionsModel addActionsItem(ActionDefinitionBasicModel actionsItem) {
    this.actions.add(actionsItem);
    return this;
  }

  /**
   * The list of all available actions the component can perform.
   * @return actions
  */
  @NotNull @Valid 
  @Schema(name = "actions", description = "The list of all available actions the component can perform.", requiredMode = Schema.RequiredMode.REQUIRED)
  public List<ActionDefinitionBasicModel> getActions() {
    return actions;
  }

  public void setActions(List<ActionDefinitionBasicModel> actions) {
    this.actions = actions;
  }

  public ComponentDefinitionWithBasicActionsModel connection(ConnectionDefinitionModel connection) {
    this.connection = connection;
    return this;
  }

  /**
   * Get connection
   * @return connection
  */
  @Valid 
  @Schema(name = "connection", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public ConnectionDefinitionModel getConnection() {
    return connection;
  }

  public void setConnection(ConnectionDefinitionModel connection) {
    this.connection = connection;
  }

  public ComponentDefinitionWithBasicActionsModel display(DisplayModel display) {
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

  public ComponentDefinitionWithBasicActionsModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The component name.
   * @return name
  */
  @NotNull 
  @Schema(name = "name", description = "The component name.", requiredMode = Schema.RequiredMode.REQUIRED)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ComponentDefinitionWithBasicActionsModel resources(ResourcesModel resources) {
    this.resources = resources;
    return this;
  }

  /**
   * Get resources
   * @return resources
  */
  @NotNull @Valid 
  @Schema(name = "resources", requiredMode = Schema.RequiredMode.REQUIRED)
  public ResourcesModel getResources() {
    return resources;
  }

  public void setResources(ResourcesModel resources) {
    this.resources = resources;
  }

  public ComponentDefinitionWithBasicActionsModel version(Integer version) {
    this.version = version;
    return this;
  }

  /**
   * The version of a component.
   * @return version
  */
  @NotNull 
  @Schema(name = "version", description = "The version of a component.", requiredMode = Schema.RequiredMode.REQUIRED)
  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComponentDefinitionWithBasicActionsModel componentDefinitionWithBasicActions = (ComponentDefinitionWithBasicActionsModel) o;
    return Objects.equals(this.actions, componentDefinitionWithBasicActions.actions) &&
        Objects.equals(this.connection, componentDefinitionWithBasicActions.connection) &&
        Objects.equals(this.display, componentDefinitionWithBasicActions.display) &&
        Objects.equals(this.name, componentDefinitionWithBasicActions.name) &&
        Objects.equals(this.resources, componentDefinitionWithBasicActions.resources) &&
        Objects.equals(this.version, componentDefinitionWithBasicActions.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(actions, connection, display, name, resources, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComponentDefinitionWithBasicActionsModel {\n");
    sb.append("    actions: ").append(toIndentedString(actions)).append("\n");
    sb.append("    connection: ").append(toIndentedString(connection)).append("\n");
    sb.append("    display: ").append(toIndentedString(display)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    resources: ").append(toIndentedString(resources)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
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

