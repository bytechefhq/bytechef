package com.bytechef.ee.automation.app.public_.web.rest.management.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppCategoryModel;
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
 * The values to change on an app. Every field is optional; an absent field keeps its stored value.
 */

@Schema(name = "AppUpdate", description = "The values to change on an app. Every field is optional; an absent field keeps its stored value.")
@JsonTypeName("AppUpdate")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-17T00:27:49.077723+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class AppUpdateModel {

  private @Nullable String name;

  private @Nullable String description;

  private @Nullable AppCategoryModel category;

  private @Nullable Boolean portalDiscoverable;

  public AppUpdateModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @Size(max = 256) 
  @Schema(name = "name", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public AppUpdateModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The new description. An empty string clears it.
   * @return description
   */
  
  @Schema(name = "description", description = "The new description. An empty string clears it.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public AppUpdateModel category(@Nullable AppCategoryModel category) {
    this.category = category;
    return this;
  }

  /**
   * Get category
   * @return category
   */
  @Valid 
  @Schema(name = "category", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("category")
  public @Nullable AppCategoryModel getCategory() {
    return category;
  }

  @JsonProperty("category")
  public void setCategory(@Nullable AppCategoryModel category) {
    this.category = category;
  }

  public AppUpdateModel portalDiscoverable(@Nullable Boolean portalDiscoverable) {
    this.portalDiscoverable = portalDiscoverable;
    return this;
  }

  /**
   * Whether the app is listed in the App Portal while it is ACTIVE. It may be set in any status.
   * @return portalDiscoverable
   */
  
  @Schema(name = "portalDiscoverable", description = "Whether the app is listed in the App Portal while it is ACTIVE. It may be set in any status.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("portalDiscoverable")
  public @Nullable Boolean getPortalDiscoverable() {
    return portalDiscoverable;
  }

  @JsonProperty("portalDiscoverable")
  public void setPortalDiscoverable(@Nullable Boolean portalDiscoverable) {
    this.portalDiscoverable = portalDiscoverable;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppUpdateModel appUpdate = (AppUpdateModel) o;
    return Objects.equals(this.name, appUpdate.name) &&
        Objects.equals(this.description, appUpdate.description) &&
        Objects.equals(this.category, appUpdate.category) &&
        Objects.equals(this.portalDiscoverable, appUpdate.portalDiscoverable);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, category, portalDiscoverable);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppUpdateModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    portalDiscoverable: ").append(toIndentedString(portalDiscoverable)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

