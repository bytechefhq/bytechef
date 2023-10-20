package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
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
 * A display information.
 */

@Schema(name = "Display", description = "A display information.")
@JsonTypeName("Display")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-12T13:09:55.588650+01:00[Europe/Zagreb]")
public class DisplayModel {

  @JsonProperty("category")
  private String category;

  @JsonProperty("description")
  private String description;

  @JsonProperty("icon")
  private String icon;

  @JsonProperty("label")
  private String label;

  @JsonProperty("subtitle")
  private String subtitle;

  @JsonProperty("tags")
  @Valid
  private List<String> tags = null;

  public DisplayModel category(String category) {
    this.category = category;
    return this;
  }

  /**
   * The category.
   * @return category
  */
  
  @Schema(name = "category", description = "The category.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public DisplayModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The description.
   * @return description
  */
  
  @Schema(name = "description", description = "The description.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public DisplayModel icon(String icon) {
    this.icon = icon;
    return this;
  }

  /**
   * The icon.
   * @return icon
  */
  
  @Schema(name = "icon", description = "The icon.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public DisplayModel label(String label) {
    this.label = label;
    return this;
  }

  /**
   * The label.
   * @return label
  */
  
  @Schema(name = "label", description = "The label.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public DisplayModel subtitle(String subtitle) {
    this.subtitle = subtitle;
    return this;
  }

  /**
   * The additional explanation.
   * @return subtitle
  */
  
  @Schema(name = "subtitle", description = "The additional explanation.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getSubtitle() {
    return subtitle;
  }

  public void setSubtitle(String subtitle) {
    this.subtitle = subtitle;
  }

  public DisplayModel tags(List<String> tags) {
    this.tags = tags;
    return this;
  }

  public DisplayModel addTagsItem(String tagsItem) {
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
  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DisplayModel display = (DisplayModel) o;
    return Objects.equals(this.category, display.category) &&
        Objects.equals(this.description, display.description) &&
        Objects.equals(this.icon, display.icon) &&
        Objects.equals(this.label, display.label) &&
        Objects.equals(this.subtitle, display.subtitle) &&
        Objects.equals(this.tags, display.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(category, description, icon, label, subtitle, tags);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DisplayModel {\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    subtitle: ").append(toIndentedString(subtitle)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
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

