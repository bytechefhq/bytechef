package com.bytechef.hermes.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.configuration.web.rest.model.ResourcesModel;
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

@Schema(name = "ComponentDefinitionBasic", description = "A component contains a set of reusable code(actions) that accomplish specific tasks, triggers and connections if there is a need for a connection to an outside service.")
@JsonTypeName("ComponentDefinitionBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-12-11T17:41:37.501155+01:00[Europe/Zagreb]")
public class ComponentDefinitionBasicModel {

  private Integer actionsCount;

  private String category;

  private String description;

  private String icon;

  private String name;

  private ResourcesModel resources;

  @Valid
  private List<String> tags;

  private String title;

  private Integer triggersCount;

  public ComponentDefinitionBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ComponentDefinitionBasicModel(String name) {
    this.name = name;
  }

  public ComponentDefinitionBasicModel actionsCount(Integer actionsCount) {
    this.actionsCount = actionsCount;
    return this;
  }

  /**
   * Get actionsCount
   * @return actionsCount
  */
  
  @Schema(name = "actionsCount", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("actionsCount")
  public Integer getActionsCount() {
    return actionsCount;
  }

  public void setActionsCount(Integer actionsCount) {
    this.actionsCount = actionsCount;
  }

  public ComponentDefinitionBasicModel category(String category) {
    this.category = category;
    return this;
  }

  /**
   * The category.
   * @return category
  */
  
  @Schema(name = "category", description = "The category.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("category")
  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public ComponentDefinitionBasicModel description(String description) {
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

  public ComponentDefinitionBasicModel icon(String icon) {
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

  public ComponentDefinitionBasicModel resources(ResourcesModel resources) {
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

  public ComponentDefinitionBasicModel tags(List<String> tags) {
    this.tags = tags;
    return this;
  }

  public ComponentDefinitionBasicModel addTagsItem(String tagsItem) {
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

  public ComponentDefinitionBasicModel title(String title) {
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

  public ComponentDefinitionBasicModel triggersCount(Integer triggersCount) {
    this.triggersCount = triggersCount;
    return this;
  }

  /**
   * Get triggersCount
   * @return triggersCount
  */
  
  @Schema(name = "triggersCount", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("triggersCount")
  public Integer getTriggersCount() {
    return triggersCount;
  }

  public void setTriggersCount(Integer triggersCount) {
    this.triggersCount = triggersCount;
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
        Objects.equals(this.category, componentDefinitionBasic.category) &&
        Objects.equals(this.description, componentDefinitionBasic.description) &&
        Objects.equals(this.icon, componentDefinitionBasic.icon) &&
        Objects.equals(this.name, componentDefinitionBasic.name) &&
        Objects.equals(this.resources, componentDefinitionBasic.resources) &&
        Objects.equals(this.tags, componentDefinitionBasic.tags) &&
        Objects.equals(this.title, componentDefinitionBasic.title) &&
        Objects.equals(this.triggersCount, componentDefinitionBasic.triggersCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(actionsCount, category, description, icon, name, resources, tags, title, triggersCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComponentDefinitionBasicModel {\n");
    sb.append("    actionsCount: ").append(toIndentedString(actionsCount)).append("\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    resources: ").append(toIndentedString(resources)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    triggersCount: ").append(toIndentedString(triggersCount)).append("\n");
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

