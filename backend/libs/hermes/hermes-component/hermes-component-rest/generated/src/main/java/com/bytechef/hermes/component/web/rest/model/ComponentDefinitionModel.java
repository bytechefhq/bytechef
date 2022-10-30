package com.bytechef.hermes.component.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.component.web.rest.model.ComponentActionModel;
import com.bytechef.hermes.component.web.rest.model.ComponentDisplayModel;
import com.bytechef.hermes.component.web.rest.model.ConnectionDefinitionModel;
import com.bytechef.hermes.component.web.rest.model.ResourcesModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * ComponentDefinitionModel
 */

@JsonTypeName("ComponentDefinition")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-27T16:22:37.776273+02:00[Europe/Zagreb]")
public class ComponentDefinitionModel {

  @JsonProperty("connections")
  @Valid
  private List<ConnectionDefinitionModel> connections = null;

  @JsonProperty("display")
  private ComponentDisplayModel display;

  @JsonProperty("name")
  private String name;

  @JsonProperty("actions")
  @Valid
  private List<ComponentActionModel> actions = null;

  @JsonProperty("resources")
  private ResourcesModel resources;

  @JsonProperty("version")
  private Double version;

  public ComponentDefinitionModel connections(List<ConnectionDefinitionModel> connections) {
    this.connections = connections;
    return this;
  }

  public ComponentDefinitionModel addConnectionsItem(ConnectionDefinitionModel connectionsItem) {
    if (this.connections == null) {
      this.connections = new ArrayList<>();
    }
    this.connections.add(connectionsItem);
    return this;
  }

  /**
   * Get connections
   * @return connections
  */
  @Valid 
  @Schema(name = "connections", required = false)
  public List<ConnectionDefinitionModel> getConnections() {
    return connections;
  }

  public void setConnections(List<ConnectionDefinitionModel> connections) {
    this.connections = connections;
  }

  public ComponentDefinitionModel display(ComponentDisplayModel display) {
    this.display = display;
    return this;
  }

  /**
   * Get display
   * @return display
  */
  @Valid 
  @Schema(name = "display", required = false)
  public ComponentDisplayModel getDisplay() {
    return display;
  }

  public void setDisplay(ComponentDisplayModel display) {
    this.display = display;
  }

  public ComponentDefinitionModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
  */
  
  @Schema(name = "name", required = false)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ComponentDefinitionModel actions(List<ComponentActionModel> actions) {
    this.actions = actions;
    return this;
  }

  public ComponentDefinitionModel addActionsItem(ComponentActionModel actionsItem) {
    if (this.actions == null) {
      this.actions = new ArrayList<>();
    }
    this.actions.add(actionsItem);
    return this;
  }

  /**
   * Get actions
   * @return actions
  */
  @Valid 
  @Schema(name = "actions", required = false)
  public List<ComponentActionModel> getActions() {
    return actions;
  }

  public void setActions(List<ComponentActionModel> actions) {
    this.actions = actions;
  }

  public ComponentDefinitionModel resources(ResourcesModel resources) {
    this.resources = resources;
    return this;
  }

  /**
   * Get resources
   * @return resources
  */
  @Valid 
  @Schema(name = "resources", required = false)
  public ResourcesModel getResources() {
    return resources;
  }

  public void setResources(ResourcesModel resources) {
    this.resources = resources;
  }

  public ComponentDefinitionModel version(Double version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * @return version
  */
  
  @Schema(name = "version", required = false)
  public Double getVersion() {
    return version;
  }

  public void setVersion(Double version) {
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
    ComponentDefinitionModel componentDefinition = (ComponentDefinitionModel) o;
    return Objects.equals(this.connections, componentDefinition.connections) &&
        Objects.equals(this.display, componentDefinition.display) &&
        Objects.equals(this.name, componentDefinition.name) &&
        Objects.equals(this.actions, componentDefinition.actions) &&
        Objects.equals(this.resources, componentDefinition.resources) &&
        Objects.equals(this.version, componentDefinition.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connections, display, name, actions, resources, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComponentDefinitionModel {\n");
    sb.append("    connections: ").append(toIndentedString(connections)).append("\n");
    sb.append("    display: ").append(toIndentedString(display)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    actions: ").append(toIndentedString(actions)).append("\n");
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

