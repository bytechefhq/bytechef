package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
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
 * A component contains a set of reusable code(actions) that accomplish specific tasks, triggers and connections if there is a need for a connection to an outside service.
 */

@Schema(name = "ComponentDefinitionBasic", description = "A component contains a set of reusable code(actions) that accomplish specific tasks, triggers and connections if there is a need for a connection to an outside service.")
@JsonTypeName("ComponentDefinitionBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-04-16T15:02:59.821023+02:00[Europe/Zagreb]", comments = "Generator version: 7.4.0")
public class ComponentDefinitionBasicModel {

  private Integer actionsCount;

  private String description;

  private String icon;

  private String name;

  private String title;

  private Integer triggersCount;

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
        Objects.equals(this.description, componentDefinitionBasic.description) &&
        Objects.equals(this.icon, componentDefinitionBasic.icon) &&
        Objects.equals(this.name, componentDefinitionBasic.name) &&
        Objects.equals(this.title, componentDefinitionBasic.title) &&
        Objects.equals(this.triggersCount, componentDefinitionBasic.triggersCount) &&
        Objects.equals(this.version, componentDefinitionBasic.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(actionsCount, description, icon, name, title, triggersCount, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComponentDefinitionBasicModel {\n");
    sb.append("    actionsCount: ").append(toIndentedString(actionsCount)).append("\n");
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
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

