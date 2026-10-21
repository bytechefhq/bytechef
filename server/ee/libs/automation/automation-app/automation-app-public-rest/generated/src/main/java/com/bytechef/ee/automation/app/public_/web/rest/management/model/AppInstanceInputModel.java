package com.bytechef.ee.automation.app.public_.web.rest.management.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppHostingTargetModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppUserAuthModeModel;
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
 * The values to create an app instance with. An absent userAuthMode means REQUIRED when the authorization server is on and NONE when it is off; with it off only NONE is accepted.
 */

@Schema(name = "AppInstanceInput", description = "The values to create an app instance with. An absent userAuthMode means REQUIRED when the authorization server is on and NONE when it is off; with it off only NONE is accepted.")
@JsonTypeName("AppInstanceInput")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-17T08:35:51.568732+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class AppInstanceInputModel {

  private Long workspaceId;

  private String name;

  private AppHostingTargetModel hostingTarget;

  private @Nullable AppUserAuthModeModel userAuthMode;

  private @Nullable AppVisibilityModel visibility;

  public AppInstanceInputModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AppInstanceInputModel(Long workspaceId, String name, AppHostingTargetModel hostingTarget) {
    this.workspaceId = workspaceId;
    this.name = name;
    this.hostingTarget = hostingTarget;
  }

  public AppInstanceInputModel workspaceId(Long workspaceId) {
    this.workspaceId = workspaceId;
    return this;
  }

  /**
   * The workspace the instance lives in; it may differ from the app's workspace.
   * @return workspaceId
   */
  @NotNull 
  @Schema(name = "workspaceId", description = "The workspace the instance lives in; it may differ from the app's workspace.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workspaceId")
  public Long getWorkspaceId() {
    return workspaceId;
  }

  @JsonProperty("workspaceId")
  public void setWorkspaceId(Long workspaceId) {
    this.workspaceId = workspaceId;
  }

  public AppInstanceInputModel name(String name) {
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

  public AppInstanceInputModel hostingTarget(AppHostingTargetModel hostingTarget) {
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

  public AppInstanceInputModel userAuthMode(@Nullable AppUserAuthModeModel userAuthMode) {
    this.userAuthMode = userAuthMode;
    return this;
  }

  /**
   * Get userAuthMode
   * @return userAuthMode
   */
  @Valid 
  @Schema(name = "userAuthMode", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("userAuthMode")
  public @Nullable AppUserAuthModeModel getUserAuthMode() {
    return userAuthMode;
  }

  @JsonProperty("userAuthMode")
  public void setUserAuthMode(@Nullable AppUserAuthModeModel userAuthMode) {
    this.userAuthMode = userAuthMode;
  }

  public AppInstanceInputModel visibility(@Nullable AppVisibilityModel visibility) {
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
    AppInstanceInputModel appInstanceInput = (AppInstanceInputModel) o;
    return Objects.equals(this.workspaceId, appInstanceInput.workspaceId) &&
        Objects.equals(this.name, appInstanceInput.name) &&
        Objects.equals(this.hostingTarget, appInstanceInput.hostingTarget) &&
        Objects.equals(this.userAuthMode, appInstanceInput.userAuthMode) &&
        Objects.equals(this.visibility, appInstanceInput.visibility);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workspaceId, name, hostingTarget, userAuthMode, visibility);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppInstanceInputModel {\n");
    sb.append("    workspaceId: ").append(toIndentedString(workspaceId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    hostingTarget: ").append(toIndentedString(hostingTarget)).append("\n");
    sb.append("    userAuthMode: ").append(toIndentedString(userAuthMode)).append("\n");
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

