package com.bytechef.ee.automation.app.public_.web.rest.management.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppHostingTargetModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppVisibilityModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The values to create an app with.
 */

@Schema(name = "AppInput", description = "The values to create an app with.")
@JsonTypeName("AppInput")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:30.252991+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class AppInputModel {

  private String name;

  private @Nullable String description;

  private AppHostingTargetModel hostingTarget;

  private @Nullable AppVisibilityModel visibility;

  public AppInputModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AppInputModel(String name, AppHostingTargetModel hostingTarget) {
    this.name = name;
    this.hostingTarget = hostingTarget;
  }

  public AppInputModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull @Size(max = 256) 
  @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public AppInputModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
   */
  
  @Schema(name = "description", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public AppInputModel hostingTarget(AppHostingTargetModel hostingTarget) {
    this.hostingTarget = hostingTarget;
    return this;
  }

  /**
   * Get hostingTarget
   * @return hostingTarget
   */
  @NotNull @Valid 
  @Schema(name = "hostingTarget", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("hostingTarget")
  public AppHostingTargetModel getHostingTarget() {
    return hostingTarget;
  }

  @JsonProperty("hostingTarget")
  public void setHostingTarget(AppHostingTargetModel hostingTarget) {
    this.hostingTarget = hostingTarget;
  }

  public AppInputModel visibility(@Nullable AppVisibilityModel visibility) {
    this.visibility = visibility;
    return this;
  }

  /**
   * Get visibility
   * @return visibility
   */
  @Valid 
  @Schema(name = "visibility", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("visibility")
  public @Nullable AppVisibilityModel getVisibility() {
    return visibility;
  }

  @JsonProperty("visibility")
  public void setVisibility(@Nullable AppVisibilityModel visibility) {
    this.visibility = visibility;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppInputModel appInput = (AppInputModel) o;
    return Objects.equals(this.name, appInput.name) &&
        Objects.equals(this.description, appInput.description) &&
        Objects.equals(this.hostingTarget, appInput.hostingTarget) &&
        Objects.equals(this.visibility, appInput.visibility);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, hostingTarget, visibility);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppInputModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    hostingTarget: ").append(toIndentedString(hostingTarget)).append("\n");
    sb.append("    visibility: ").append(toIndentedString(visibility)).append("\n");
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

