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
 * A category of component.
 */

@Schema(name = "ComponentCategory", description = "A category of component.")
@JsonTypeName("ComponentCategory")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-10-28T06:11:36.791210+01:00[Europe/Zagreb]", comments = "Generator version: 7.9.0")
public class ComponentCategoryModel {

  private String key;

  private String label;

  public ComponentCategoryModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ComponentCategoryModel(String key) {
    this.key = key;
  }

  public ComponentCategoryModel key(String key) {
    this.key = key;
    return this;
  }

  /**
   * The key of a category.
   * @return key
   */
  @NotNull 
  @Schema(name = "key", description = "The key of a category.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("key")
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public ComponentCategoryModel label(String label) {
    this.label = label;
    return this;
  }

  /**
   * The label of a category.
   * @return label
   */
  
  @Schema(name = "label", description = "The label of a category.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
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
    ComponentCategoryModel componentCategory = (ComponentCategoryModel) o;
    return Objects.equals(this.key, componentCategory.key) &&
        Objects.equals(this.label, componentCategory.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, label);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComponentCategoryModel {\n");
    sb.append("    key: ").append(toIndentedString(key)).append("\n");
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

