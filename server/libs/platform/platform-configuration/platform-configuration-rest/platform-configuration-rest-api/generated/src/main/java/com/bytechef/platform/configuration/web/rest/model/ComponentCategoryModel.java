package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.lang.Nullable;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-15T09:52:48.574632+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class ComponentCategoryModel {

  private String name;

  private @Nullable String label;

  public ComponentCategoryModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ComponentCategoryModel(String name) {
    this.name = name;
  }

  public ComponentCategoryModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of a category.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The name of a category.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ComponentCategoryModel label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * The label of a category.
   * @return label
   */
  
  @Schema(name = "label", description = "The label of a category.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  public void setLabel(@Nullable String label) {
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
    return Objects.equals(this.name, componentCategory.name) &&
        Objects.equals(this.label, componentCategory.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, label);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComponentCategoryModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

