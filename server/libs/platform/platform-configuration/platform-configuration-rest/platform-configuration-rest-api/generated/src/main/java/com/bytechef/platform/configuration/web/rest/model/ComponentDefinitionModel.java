package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.ActionDefinitionBasicModel;
import com.bytechef.platform.configuration.web.rest.model.ComponentCategoryModel;
import com.bytechef.platform.configuration.web.rest.model.ConnectionDefinitionBasicModel;
import com.bytechef.platform.configuration.web.rest.model.ResourcesModel;
import com.bytechef.platform.configuration.web.rest.model.TriggerDefinitionBasicModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
 * A component contains a set of reusable code(actions) that accomplish specific tasks, triggers and connections if there is a need for a connection to an outside service.
 */

@Schema(name = "ComponentDefinition", description = "A component contains a set of reusable code(actions) that accomplish specific tasks, triggers and connections if there is a need for a connection to an outside service.")
@JsonTypeName("ComponentDefinition")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-26T23:39:11.255305+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public class ComponentDefinitionModel {

  @Valid
  private List<@Valid ActionDefinitionBasicModel> actions = new ArrayList<>();

  @Valid
  private List<ComponentCategoryModel> categories = new ArrayList<>();

  private ConnectionDefinitionBasicModel connection;

  private Boolean connectionRequired;

  private String description;

  private String icon;

  private String name;

  private ResourcesModel resources;

  @Valid
  private List<String> tags = new ArrayList<>();

  private String title;

  @Valid
  private List<@Valid TriggerDefinitionBasicModel> triggers = new ArrayList<>();

  private Integer version;

  public ComponentDefinitionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ComponentDefinitionModel(Boolean connectionRequired, String name, Integer version) {
    this.connectionRequired = connectionRequired;
    this.name = name;
    this.version = version;
  }

  public ComponentDefinitionModel actions(List<@Valid ActionDefinitionBasicModel> actions) {
    this.actions = actions;
    return this;
  }

  public ComponentDefinitionModel addActionsItem(ActionDefinitionBasicModel actionsItem) {
    if (this.actions == null) {
      this.actions = new ArrayList<>();
    }
    this.actions.add(actionsItem);
    return this;
  }

  /**
   * The list of all available actions the component can perform.
   * @return actions
  */
  @Valid 
  @Schema(name = "actions", description = "The list of all available actions the component can perform.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("actions")
  public List<@Valid ActionDefinitionBasicModel> getActions() {
    return actions;
  }

  public void setActions(List<@Valid ActionDefinitionBasicModel> actions) {
    this.actions = actions;
  }

  public ComponentDefinitionModel categories(List<ComponentCategoryModel> categories) {
    this.categories = categories;
    return this;
  }

  public ComponentDefinitionModel addCategoriesItem(ComponentCategoryModel categoriesItem) {
    if (this.categories == null) {
      this.categories = new ArrayList<>();
    }
    this.categories.add(categoriesItem);
    return this;
  }

  /**
   * The list of categories the component belongs to.
   * @return categories
  */
  @Valid 
  @Schema(name = "categories", description = "The list of categories the component belongs to.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("categories")
  public List<ComponentCategoryModel> getCategories() {
    return categories;
  }

  public void setCategories(List<ComponentCategoryModel> categories) {
    this.categories = categories;
  }

  public ComponentDefinitionModel connection(ConnectionDefinitionBasicModel connection) {
    this.connection = connection;
    return this;
  }

  /**
   * Get connection
   * @return connection
  */
  @Valid 
  @Schema(name = "connection", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connection")
  public ConnectionDefinitionBasicModel getConnection() {
    return connection;
  }

  public void setConnection(ConnectionDefinitionBasicModel connection) {
    this.connection = connection;
  }

  public ComponentDefinitionModel connectionRequired(Boolean connectionRequired) {
    this.connectionRequired = connectionRequired;
    return this;
  }

  /**
   * If connection is required or not if it is defined.
   * @return connectionRequired
  */
  @NotNull 
  @Schema(name = "connectionRequired", description = "If connection is required or not if it is defined.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connectionRequired")
  public Boolean getConnectionRequired() {
    return connectionRequired;
  }

  public void setConnectionRequired(Boolean connectionRequired) {
    this.connectionRequired = connectionRequired;
  }

  public ComponentDefinitionModel description(String description) {
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

  public ComponentDefinitionModel icon(String icon) {
    this.icon = icon;
    return this;
  }

  /**
   * The icon.
   * @return icon
  */
  
  @Schema(name = "icon", description = "The icon.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("icon")
  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public ComponentDefinitionModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name.
   * @return name
  */
  @NotNull 
  @Schema(name = "name", description = "The name.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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
  @Schema(name = "resources", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("resources")
  public ResourcesModel getResources() {
    return resources;
  }

  public void setResources(ResourcesModel resources) {
    this.resources = resources;
  }

  public ComponentDefinitionModel tags(List<String> tags) {
    this.tags = tags;
    return this;
  }

  public ComponentDefinitionModel addTagsItem(String tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Tags for categorization.
   * @return tags
  */
  
  @Schema(name = "tags", description = "Tags for categorization.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("tags")
  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public ComponentDefinitionModel title(String title) {
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

  public ComponentDefinitionModel triggers(List<@Valid TriggerDefinitionBasicModel> triggers) {
    this.triggers = triggers;
    return this;
  }

  public ComponentDefinitionModel addTriggersItem(TriggerDefinitionBasicModel triggersItem) {
    if (this.triggers == null) {
      this.triggers = new ArrayList<>();
    }
    this.triggers.add(triggersItem);
    return this;
  }

  /**
   * The list of all available triggers the component can perform.
   * @return triggers
  */
  @Valid 
  @Schema(name = "triggers", description = "The list of all available triggers the component can perform.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("triggers")
  public List<@Valid TriggerDefinitionBasicModel> getTriggers() {
    return triggers;
  }

  public void setTriggers(List<@Valid TriggerDefinitionBasicModel> triggers) {
    this.triggers = triggers;
  }

  public ComponentDefinitionModel version(Integer version) {
    this.version = version;
    return this;
  }

  /**
   * The version of a component.
   * @return version
  */
  @NotNull 
  @Schema(name = "version", description = "The version of a component.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("version")
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
    ComponentDefinitionModel componentDefinition = (ComponentDefinitionModel) o;
    return Objects.equals(this.actions, componentDefinition.actions) &&
        Objects.equals(this.categories, componentDefinition.categories) &&
        Objects.equals(this.connection, componentDefinition.connection) &&
        Objects.equals(this.connectionRequired, componentDefinition.connectionRequired) &&
        Objects.equals(this.description, componentDefinition.description) &&
        Objects.equals(this.icon, componentDefinition.icon) &&
        Objects.equals(this.name, componentDefinition.name) &&
        Objects.equals(this.resources, componentDefinition.resources) &&
        Objects.equals(this.tags, componentDefinition.tags) &&
        Objects.equals(this.title, componentDefinition.title) &&
        Objects.equals(this.triggers, componentDefinition.triggers) &&
        Objects.equals(this.version, componentDefinition.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(actions, categories, connection, connectionRequired, description, icon, name, resources, tags, title, triggers, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComponentDefinitionModel {\n");
    sb.append("    actions: ").append(toIndentedString(actions)).append("\n");
    sb.append("    categories: ").append(toIndentedString(categories)).append("\n");
    sb.append("    connection: ").append(toIndentedString(connection)).append("\n");
    sb.append("    connectionRequired: ").append(toIndentedString(connectionRequired)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    resources: ").append(toIndentedString(resources)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    triggers: ").append(toIndentedString(triggers)).append("\n");
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

