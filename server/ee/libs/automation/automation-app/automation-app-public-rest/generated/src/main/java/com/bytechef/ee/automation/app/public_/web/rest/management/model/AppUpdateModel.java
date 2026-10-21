package com.bytechef.ee.automation.app.public_.web.rest.management.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppHostingTargetModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppStatusModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppVisibilityModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:30.252991+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class AppUpdateModel {

  private @Nullable String name;

  private @Nullable String description;

  private @Nullable AppStatusModel status;

  private @Nullable AppHostingTargetModel hostingTarget;

  private @Nullable String url;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastDeployedDate;

  private @Nullable AppVisibilityModel visibility;

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

  public AppUpdateModel status(@Nullable AppStatusModel status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @Valid 
  @Schema(name = "status", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("status")
  public @Nullable AppStatusModel getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable AppStatusModel status) {
    this.status = status;
  }

  public AppUpdateModel hostingTarget(@Nullable AppHostingTargetModel hostingTarget) {
    this.hostingTarget = hostingTarget;
    return this;
  }

  /**
   * Get hostingTarget
   * @return hostingTarget
   */
  @Valid 
  @Schema(name = "hostingTarget", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("hostingTarget")
  public @Nullable AppHostingTargetModel getHostingTarget() {
    return hostingTarget;
  }

  @JsonProperty("hostingTarget")
  public void setHostingTarget(@Nullable AppHostingTargetModel hostingTarget) {
    this.hostingTarget = hostingTarget;
  }

  public AppUpdateModel url(@Nullable String url) {
    this.url = url;
    return this;
  }

  /**
   * The new url. An empty string clears it.
   * @return url
   */
  @Size(max = 2048) 
  @Schema(name = "url", description = "The new url. An empty string clears it.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("url")
  public @Nullable String getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(@Nullable String url) {
    this.url = url;
  }

  public AppUpdateModel lastDeployedDate(@Nullable OffsetDateTime lastDeployedDate) {
    this.lastDeployedDate = lastDeployedDate;
    return this;
  }

  /**
   * Get lastDeployedDate
   * @return lastDeployedDate
   */
  @Valid 
  @Schema(name = "lastDeployedDate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastDeployedDate")
  public @Nullable OffsetDateTime getLastDeployedDate() {
    return lastDeployedDate;
  }

  @JsonProperty("lastDeployedDate")
  public void setLastDeployedDate(@Nullable OffsetDateTime lastDeployedDate) {
    this.lastDeployedDate = lastDeployedDate;
  }

  public AppUpdateModel visibility(@Nullable AppVisibilityModel visibility) {
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
    AppUpdateModel appUpdate = (AppUpdateModel) o;
    return Objects.equals(this.name, appUpdate.name) &&
        Objects.equals(this.description, appUpdate.description) &&
        Objects.equals(this.status, appUpdate.status) &&
        Objects.equals(this.hostingTarget, appUpdate.hostingTarget) &&
        Objects.equals(this.url, appUpdate.url) &&
        Objects.equals(this.lastDeployedDate, appUpdate.lastDeployedDate) &&
        Objects.equals(this.visibility, appUpdate.visibility);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, status, hostingTarget, url, lastDeployedDate, visibility);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppUpdateModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    hostingTarget: ").append(toIndentedString(hostingTarget)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    lastDeployedDate: ").append(toIndentedString(lastDeployedDate)).append("\n");
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

