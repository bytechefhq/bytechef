package com.bytechef.hermes.component.web.rest.model;

import java.net.URI;
import java.util.Objects;
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
 * ComponentDisplayModel
 */

@JsonTypeName("ComponentDisplay")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-27T16:22:37.776273+02:00[Europe/Zagreb]")
public class ComponentDisplayModel {

  @JsonProperty("category")
  private String category;

  @JsonProperty("subtitle")
  private String subtitle;

  @JsonProperty("tags")
  @Valid
  private List<String> tags = null;

  @JsonProperty("description")
  private String description;

  @JsonProperty("icon")
  private String icon;

  @JsonProperty("label")
  private String label;

  public ComponentDisplayModel category(String category) {
    this.category = category;
    return this;
  }

  /**
   * Get category
   * @return category
  */
  
  @Schema(name = "category", required = false)
  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public ComponentDisplayModel subtitle(String subtitle) {
    this.subtitle = subtitle;
    return this;
  }

  /**
   * Get subtitle
   * @return subtitle
  */
  
  @Schema(name = "subtitle", required = false)
  public String getSubtitle() {
    return subtitle;
  }

  public void setSubtitle(String subtitle) {
    this.subtitle = subtitle;
  }

  public ComponentDisplayModel tags(List<String> tags) {
    this.tags = tags;
    return this;
  }

  public ComponentDisplayModel addTagsItem(String tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Get tags
   * @return tags
  */
  
  @Schema(name = "tags", required = false)
  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public ComponentDisplayModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
  */
  
  @Schema(name = "description", required = false)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ComponentDisplayModel icon(String icon) {
    this.icon = icon;
    return this;
  }

  /**
   * Get icon
   * @return icon
  */
  
  @Schema(name = "icon", required = false)
  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public ComponentDisplayModel label(String label) {
    this.label = label;
    return this;
  }

  /**
   * Get label
   * @return label
  */
  
  @Schema(name = "label", required = false)
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComponentDisplayModel componentDisplay = (ComponentDisplayModel) o;
    return Objects.equals(this.category, componentDisplay.category) &&
        Objects.equals(this.subtitle, componentDisplay.subtitle) &&
        Objects.equals(this.tags, componentDisplay.tags) &&
        Objects.equals(this.description, componentDisplay.description) &&
        Objects.equals(this.icon, componentDisplay.icon) &&
        Objects.equals(this.label, componentDisplay.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(category, subtitle, tags, description, icon, label);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComponentDisplayModel {\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    subtitle: ").append(toIndentedString(subtitle)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
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

