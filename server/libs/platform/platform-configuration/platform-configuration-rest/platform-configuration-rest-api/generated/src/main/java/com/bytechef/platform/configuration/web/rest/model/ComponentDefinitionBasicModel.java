package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.ComponentCategoryModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
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

@Schema(name = "ComponentDefinitionBasic", description = "A component contains a set of reusable code(actions) that accomplish specific tasks, triggers and connections if there is a need for a connection to an outside service.")
@JsonTypeName("ComponentDefinitionBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-07T09:52:01.007100+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public class ComponentDefinitionBasicModel {

  private @Nullable Integer actionsCount;

  @Valid
  private Map<String, Integer> clusterElementsCount = new HashMap<>();

  @Valid
  private List<@Valid ComponentCategoryModel> componentCategories = new ArrayList<>();

  private @Nullable String description;

  private @Nullable String icon;

  private String name;

  private @Nullable String title;

  private @Nullable Integer triggersCount;

  private Integer version;

  public ComponentDefinitionBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ComponentDefinitionBasicModel(String name, Integer version) {
    this.name = name;
    this.version = version;
  }

  public ComponentDefinitionBasicModel actionsCount(@Nullable Integer actionsCount) {
    this.actionsCount = actionsCount;
    return this;
  }

  /**
   * The number of actions a component has
   * @return actionsCount
   */
  
  @Schema(name = "actionsCount", description = "The number of actions a component has", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("actionsCount")
  public @Nullable Integer getActionsCount() {
    return actionsCount;
  }

  public void setActionsCount(@Nullable Integer actionsCount) {
    this.actionsCount = actionsCount;
  }

  public ComponentDefinitionBasicModel clusterElementsCount(Map<String, Integer> clusterElementsCount) {
    this.clusterElementsCount = clusterElementsCount;
    return this;
  }

  public ComponentDefinitionBasicModel putClusterElementsCountItem(String key, Integer clusterElementsCountItem) {
    if (this.clusterElementsCount == null) {
      this.clusterElementsCount = new HashMap<>();
    }
    this.clusterElementsCount.put(key, clusterElementsCountItem);
    return this;
  }

  /**
   * The number of cluster elements a component has
   * @return clusterElementsCount
   */
  
  @Schema(name = "clusterElementsCount", description = "The number of cluster elements a component has", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("clusterElementsCount")
  public Map<String, Integer> getClusterElementsCount() {
    return clusterElementsCount;
  }

  public void setClusterElementsCount(Map<String, Integer> clusterElementsCount) {
    this.clusterElementsCount = clusterElementsCount;
  }

  public ComponentDefinitionBasicModel componentCategories(List<@Valid ComponentCategoryModel> componentCategories) {
    this.componentCategories = componentCategories;
    return this;
  }

  public ComponentDefinitionBasicModel addComponentCategoriesItem(ComponentCategoryModel componentCategoriesItem) {
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

  public ComponentDefinitionBasicModel description(@Nullable String description) {
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

  public ComponentDefinitionBasicModel icon(@Nullable String icon) {
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

  public ComponentDefinitionBasicModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of a component.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The name of a component.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ComponentDefinitionBasicModel title(@Nullable String title) {
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

  public ComponentDefinitionBasicModel triggersCount(@Nullable Integer triggersCount) {
    this.triggersCount = triggersCount;
    return this;
  }

  /**
   * The number of triggers a component has
   * @return triggersCount
   */
  
  @Schema(name = "triggersCount", description = "The number of triggers a component has", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("triggersCount")
  public @Nullable Integer getTriggersCount() {
    return triggersCount;
  }

  public void setTriggersCount(@Nullable Integer triggersCount) {
    this.triggersCount = triggersCount;
  }

  public ComponentDefinitionBasicModel version(Integer version) {
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
    ComponentDefinitionBasicModel componentDefinitionBasic = (ComponentDefinitionBasicModel) o;
    return Objects.equals(this.actionsCount, componentDefinitionBasic.actionsCount) &&
        Objects.equals(this.clusterElementsCount, componentDefinitionBasic.clusterElementsCount) &&
        Objects.equals(this.componentCategories, componentDefinitionBasic.componentCategories) &&
        Objects.equals(this.description, componentDefinitionBasic.description) &&
        Objects.equals(this.icon, componentDefinitionBasic.icon) &&
        Objects.equals(this.name, componentDefinitionBasic.name) &&
        Objects.equals(this.title, componentDefinitionBasic.title) &&
        Objects.equals(this.triggersCount, componentDefinitionBasic.triggersCount) &&
        Objects.equals(this.version, componentDefinitionBasic.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(actionsCount, clusterElementsCount, componentCategories, description, icon, name, title, triggersCount, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComponentDefinitionBasicModel {\n");
    sb.append("    actionsCount: ").append(toIndentedString(actionsCount)).append("\n");
    sb.append("    clusterElementsCount: ").append(toIndentedString(clusterElementsCount)).append("\n");
    sb.append("    componentCategories: ").append(toIndentedString(componentCategories)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    triggersCount: ").append(toIndentedString(triggersCount)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

