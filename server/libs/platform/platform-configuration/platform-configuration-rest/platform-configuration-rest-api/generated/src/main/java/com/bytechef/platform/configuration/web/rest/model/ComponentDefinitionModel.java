package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.ActionDefinitionBasicModel;
import com.bytechef.platform.configuration.web.rest.model.ClusterElementDefinitionBasicModel;
import com.bytechef.platform.configuration.web.rest.model.ClusterElementTypeModel;
import com.bytechef.platform.configuration.web.rest.model.ComponentCategoryModel;
import com.bytechef.platform.configuration.web.rest.model.ConnectionDefinitionBasicModel;
import com.bytechef.platform.configuration.web.rest.model.ResourcesModel;
import com.bytechef.platform.configuration.web.rest.model.TriggerDefinitionBasicModel;
import com.bytechef.platform.configuration.web.rest.model.UnifiedApiCategoryModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-23T12:36:59.494207+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class ComponentDefinitionModel {

  @Valid
  private Map<String, List<String>> actionClusterElementTypes = new HashMap<>();

  @Valid
  private List<@Valid ActionDefinitionBasicModel> actions = new ArrayList<>();

  private Boolean clusterElement;

  @Valid
  private Map<String, List<String>> clusterElementClusterElementTypes = new HashMap<>();

  @Valid
  private List<@Valid ClusterElementDefinitionBasicModel> clusterElements = new ArrayList<>();

  @Valid
  private List<@Valid ClusterElementTypeModel> clusterElementTypes = new ArrayList<>();

  private Boolean clusterRoot;

  @Valid
  private List<@Valid ComponentCategoryModel> componentCategories = new ArrayList<>();

  private @Nullable ConnectionDefinitionBasicModel connection;

  private Boolean connectionRequired;

  private @Nullable String description;

  private @Nullable String icon;

  private String name;

  private @Nullable ResourcesModel resources;

  @Valid
  private List<String> tags = new ArrayList<>();

  private @Nullable String title;

  @Valid
  private List<@Valid TriggerDefinitionBasicModel> triggers = new ArrayList<>();

  private @Nullable UnifiedApiCategoryModel unifiedApiCategory;

  private Integer version;

  public ComponentDefinitionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ComponentDefinitionModel(Boolean clusterElement, Boolean clusterRoot, Boolean connectionRequired, String name, Integer version) {
    this.clusterElement = clusterElement;
    this.clusterRoot = clusterRoot;
    this.connectionRequired = connectionRequired;
    this.name = name;
    this.version = version;
  }

  public ComponentDefinitionModel actionClusterElementTypes(Map<String, List<String>> actionClusterElementTypes) {
    this.actionClusterElementTypes = actionClusterElementTypes;
    return this;
  }

  public ComponentDefinitionModel putActionClusterElementTypesItem(String key, List<String> actionClusterElementTypesItem) {
    if (this.actionClusterElementTypes == null) {
      this.actionClusterElementTypes = new HashMap<>();
    }
    this.actionClusterElementTypes.put(key, actionClusterElementTypesItem);
    return this;
  }

  /**
   * The list of cluster element types per action.
   * @return actionClusterElementTypes
   */
  @Valid 
  @Schema(name = "actionClusterElementTypes", description = "The list of cluster element types per action.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("actionClusterElementTypes")
  public Map<String, List<String>> getActionClusterElementTypes() {
    return actionClusterElementTypes;
  }

  public void setActionClusterElementTypes(Map<String, List<String>> actionClusterElementTypes) {
    this.actionClusterElementTypes = actionClusterElementTypes;
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

  public ComponentDefinitionModel clusterElement(Boolean clusterElement) {
    this.clusterElement = clusterElement;
    return this;
  }

  /**
   * Is the component cluster element.
   * @return clusterElement
   */
  @NotNull 
  @Schema(name = "clusterElement", description = "Is the component cluster element.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("clusterElement")
  public Boolean getClusterElement() {
    return clusterElement;
  }

  public void setClusterElement(Boolean clusterElement) {
    this.clusterElement = clusterElement;
  }

  public ComponentDefinitionModel clusterElementClusterElementTypes(Map<String, List<String>> clusterElementClusterElementTypes) {
    this.clusterElementClusterElementTypes = clusterElementClusterElementTypes;
    return this;
  }

  public ComponentDefinitionModel putClusterElementClusterElementTypesItem(String key, List<String> clusterElementClusterElementTypesItem) {
    if (this.clusterElementClusterElementTypes == null) {
      this.clusterElementClusterElementTypes = new HashMap<>();
    }
    this.clusterElementClusterElementTypes.put(key, clusterElementClusterElementTypesItem);
    return this;
  }

  /**
   * The list of cluster element types per root cluster element.
   * @return clusterElementClusterElementTypes
   */
  @Valid 
  @Schema(name = "clusterElementClusterElementTypes", description = "The list of cluster element types per root cluster element.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("clusterElementClusterElementTypes")
  public Map<String, List<String>> getClusterElementClusterElementTypes() {
    return clusterElementClusterElementTypes;
  }

  public void setClusterElementClusterElementTypes(Map<String, List<String>> clusterElementClusterElementTypes) {
    this.clusterElementClusterElementTypes = clusterElementClusterElementTypes;
  }

  public ComponentDefinitionModel clusterElements(List<@Valid ClusterElementDefinitionBasicModel> clusterElements) {
    this.clusterElements = clusterElements;
    return this;
  }

  public ComponentDefinitionModel addClusterElementsItem(ClusterElementDefinitionBasicModel clusterElementsItem) {
    if (this.clusterElements == null) {
      this.clusterElements = new ArrayList<>();
    }
    this.clusterElements.add(clusterElementsItem);
    return this;
  }

  /**
   * The list of all available cluster elements the component can perform.
   * @return clusterElements
   */
  @Valid 
  @Schema(name = "clusterElements", description = "The list of all available cluster elements the component can perform.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("clusterElements")
  public List<@Valid ClusterElementDefinitionBasicModel> getClusterElements() {
    return clusterElements;
  }

  public void setClusterElements(List<@Valid ClusterElementDefinitionBasicModel> clusterElements) {
    this.clusterElements = clusterElements;
  }

  public ComponentDefinitionModel clusterElementTypes(List<@Valid ClusterElementTypeModel> clusterElementTypes) {
    this.clusterElementTypes = clusterElementTypes;
    return this;
  }

  public ComponentDefinitionModel addClusterElementTypesItem(ClusterElementTypeModel clusterElementTypesItem) {
    if (this.clusterElementTypes == null) {
      this.clusterElementTypes = new ArrayList<>();
    }
    this.clusterElementTypes.add(clusterElementTypesItem);
    return this;
  }

  /**
   * The list of cluster element types.
   * @return clusterElementTypes
   */
  @Valid 
  @Schema(name = "clusterElementTypes", description = "The list of cluster element types.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("clusterElementTypes")
  public List<@Valid ClusterElementTypeModel> getClusterElementTypes() {
    return clusterElementTypes;
  }

  public void setClusterElementTypes(List<@Valid ClusterElementTypeModel> clusterElementTypes) {
    this.clusterElementTypes = clusterElementTypes;
  }

  public ComponentDefinitionModel clusterRoot(Boolean clusterRoot) {
    this.clusterRoot = clusterRoot;
    return this;
  }

  /**
   * Is the component cluster root.
   * @return clusterRoot
   */
  @NotNull 
  @Schema(name = "clusterRoot", description = "Is the component cluster root.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("clusterRoot")
  public Boolean getClusterRoot() {
    return clusterRoot;
  }

  public void setClusterRoot(Boolean clusterRoot) {
    this.clusterRoot = clusterRoot;
  }

  public ComponentDefinitionModel componentCategories(List<@Valid ComponentCategoryModel> componentCategories) {
    this.componentCategories = componentCategories;
    return this;
  }

  public ComponentDefinitionModel addComponentCategoriesItem(ComponentCategoryModel componentCategoriesItem) {
    if (this.componentCategories == null) {
      this.componentCategories = new ArrayList<>();
    }
    this.componentCategories.add(componentCategoriesItem);
    return this;
  }

  /**
   * The list of categories the component belongs to.
   * @return componentCategories
   */
  @Valid 
  @Schema(name = "componentCategories", description = "The list of categories the component belongs to.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentCategories")
  public List<@Valid ComponentCategoryModel> getComponentCategories() {
    return componentCategories;
  }

  public void setComponentCategories(List<@Valid ComponentCategoryModel> componentCategories) {
    this.componentCategories = componentCategories;
  }

  public ComponentDefinitionModel connection(@Nullable ConnectionDefinitionBasicModel connection) {
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
  public @Nullable ConnectionDefinitionBasicModel getConnection() {
    return connection;
  }

  public void setConnection(@Nullable ConnectionDefinitionBasicModel connection) {
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

  public ComponentDefinitionModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description.
   * @return description
   */
  
  @Schema(name = "description", description = "The description.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public ComponentDefinitionModel icon(@Nullable String icon) {
    this.icon = icon;
    return this;
  }

  /**
   * The icon.
   * @return icon
   */
  
  @Schema(name = "icon", description = "The icon.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("icon")
  public @Nullable String getIcon() {
    return icon;
  }

  public void setIcon(@Nullable String icon) {
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

  public ComponentDefinitionModel resources(@Nullable ResourcesModel resources) {
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
  public @Nullable ResourcesModel getResources() {
    return resources;
  }

  public void setResources(@Nullable ResourcesModel resources) {
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

  public ComponentDefinitionModel title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * The title
   * @return title
   */
  
  @Schema(name = "title", description = "The title", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  public void setTitle(@Nullable String title) {
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

  public ComponentDefinitionModel unifiedApiCategory(@Nullable UnifiedApiCategoryModel unifiedApiCategory) {
    this.unifiedApiCategory = unifiedApiCategory;
    return this;
  }

  /**
   * Get unifiedApiCategory
   * @return unifiedApiCategory
   */
  @Valid 
  @Schema(name = "unifiedApiCategory", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("unifiedApiCategory")
  public @Nullable UnifiedApiCategoryModel getUnifiedApiCategory() {
    return unifiedApiCategory;
  }

  public void setUnifiedApiCategory(@Nullable UnifiedApiCategoryModel unifiedApiCategory) {
    this.unifiedApiCategory = unifiedApiCategory;
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
    return Objects.equals(this.actionClusterElementTypes, componentDefinition.actionClusterElementTypes) &&
        Objects.equals(this.actions, componentDefinition.actions) &&
        Objects.equals(this.clusterElement, componentDefinition.clusterElement) &&
        Objects.equals(this.clusterElementClusterElementTypes, componentDefinition.clusterElementClusterElementTypes) &&
        Objects.equals(this.clusterElements, componentDefinition.clusterElements) &&
        Objects.equals(this.clusterElementTypes, componentDefinition.clusterElementTypes) &&
        Objects.equals(this.clusterRoot, componentDefinition.clusterRoot) &&
        Objects.equals(this.componentCategories, componentDefinition.componentCategories) &&
        Objects.equals(this.connection, componentDefinition.connection) &&
        Objects.equals(this.connectionRequired, componentDefinition.connectionRequired) &&
        Objects.equals(this.description, componentDefinition.description) &&
        Objects.equals(this.icon, componentDefinition.icon) &&
        Objects.equals(this.name, componentDefinition.name) &&
        Objects.equals(this.resources, componentDefinition.resources) &&
        Objects.equals(this.tags, componentDefinition.tags) &&
        Objects.equals(this.title, componentDefinition.title) &&
        Objects.equals(this.triggers, componentDefinition.triggers) &&
        Objects.equals(this.unifiedApiCategory, componentDefinition.unifiedApiCategory) &&
        Objects.equals(this.version, componentDefinition.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(actionClusterElementTypes, actions, clusterElement, clusterElementClusterElementTypes, clusterElements, clusterElementTypes, clusterRoot, componentCategories, connection, connectionRequired, description, icon, name, resources, tags, title, triggers, unifiedApiCategory, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComponentDefinitionModel {\n");
    sb.append("    actionClusterElementTypes: ").append(toIndentedString(actionClusterElementTypes)).append("\n");
    sb.append("    actions: ").append(toIndentedString(actions)).append("\n");
    sb.append("    clusterElement: ").append(toIndentedString(clusterElement)).append("\n");
    sb.append("    clusterElementClusterElementTypes: ").append(toIndentedString(clusterElementClusterElementTypes)).append("\n");
    sb.append("    clusterElements: ").append(toIndentedString(clusterElements)).append("\n");
    sb.append("    clusterElementTypes: ").append(toIndentedString(clusterElementTypes)).append("\n");
    sb.append("    clusterRoot: ").append(toIndentedString(clusterRoot)).append("\n");
    sb.append("    componentCategories: ").append(toIndentedString(componentCategories)).append("\n");
    sb.append("    connection: ").append(toIndentedString(connection)).append("\n");
    sb.append("    connectionRequired: ").append(toIndentedString(connectionRequired)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    resources: ").append(toIndentedString(resources)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    triggers: ").append(toIndentedString(triggers)).append("\n");
    sb.append("    unifiedApiCategory: ").append(toIndentedString(unifiedApiCategory)).append("\n");
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

