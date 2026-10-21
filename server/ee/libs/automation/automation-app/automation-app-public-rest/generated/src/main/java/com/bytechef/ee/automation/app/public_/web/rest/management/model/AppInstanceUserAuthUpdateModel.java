package com.bytechef.ee.automation.app.public_.web.rest.management.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppUserAuthModeModel;
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
 * The new sign-in mode of an app instance.
 */

@Schema(name = "AppInstanceUserAuthUpdate", description = "The new sign-in mode of an app instance.")
@JsonTypeName("AppInstanceUserAuthUpdate")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-16T15:27:36.535245+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class AppInstanceUserAuthUpdateModel {

  private AppUserAuthModeModel userAuthMode;

  public AppInstanceUserAuthUpdateModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AppInstanceUserAuthUpdateModel(AppUserAuthModeModel userAuthMode) {
    this.userAuthMode = userAuthMode;
  }

  public AppInstanceUserAuthUpdateModel userAuthMode(AppUserAuthModeModel userAuthMode) {
    this.userAuthMode = userAuthMode;
    return this;
  }

  /**
   * Get userAuthMode
   * @return userAuthMode
   */
  @NotNull @Valid 
  @Schema(name = "userAuthMode", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("userAuthMode")
  public AppUserAuthModeModel getUserAuthMode() {
    return userAuthMode;
  }

  @JsonProperty("userAuthMode")
  public void setUserAuthMode(AppUserAuthModeModel userAuthMode) {
    this.userAuthMode = userAuthMode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppInstanceUserAuthUpdateModel appInstanceUserAuthUpdate = (AppInstanceUserAuthUpdateModel) o;
    return Objects.equals(this.userAuthMode, appInstanceUserAuthUpdate.userAuthMode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userAuthMode);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppInstanceUserAuthUpdateModel {\n");
    sb.append("    userAuthMode: ").append(toIndentedString(userAuthMode)).append("\n");
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

