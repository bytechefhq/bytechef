package com.bytechef.ee.automation.app.public_.web.rest.management.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The deployment connection and settings of an app instance. Both absent clears them.
 */

@Schema(name = "AppInstanceDeploymentSettingsUpdate", description = "The deployment connection and settings of an app instance. Both absent clears them.")
@JsonTypeName("AppInstanceDeploymentSettingsUpdate")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-16T22:08:52.163262+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class AppInstanceDeploymentSettingsUpdateModel {

  private @Nullable Long connectionId;

  private Map<String, String> settings = new HashMap<>();

  public AppInstanceDeploymentSettingsUpdateModel connectionId(@Nullable Long connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  /**
   * The connection the deployment provider calls the hosting service with.
   * @return connectionId
   */
  
  @Schema(name = "connectionId", description = "The connection the deployment provider calls the hosting service with.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionId")
  public @Nullable Long getConnectionId() {
    return connectionId;
  }

  @JsonProperty("connectionId")
  public void setConnectionId(@Nullable Long connectionId) {
    this.connectionId = connectionId;
  }

  public AppInstanceDeploymentSettingsUpdateModel settings(Map<String, String> settings) {
    this.settings = settings;
    return this;
  }

  public AppInstanceDeploymentSettingsUpdateModel putSettingsItem(String key, String settingsItem) {
    if (this.settings == null) {
      this.settings = new HashMap<>();
    }
    this.settings.put(key, settingsItem);
    return this;
  }

  /**
   * The provider's settings. They are visible to everyone who can view the instance and must not hold secrets.
   * @return settings
   */
  
  @Schema(name = "settings", description = "The provider's settings. They are visible to everyone who can view the instance and must not hold secrets.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("settings")
  public Map<String, String> getSettings() {
    return settings;
  }

  @JsonProperty("settings")
  public void setSettings(Map<String, String> settings) {
    this.settings = settings;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppInstanceDeploymentSettingsUpdateModel appInstanceDeploymentSettingsUpdate = (AppInstanceDeploymentSettingsUpdateModel) o;
    return Objects.equals(this.connectionId, appInstanceDeploymentSettingsUpdate.connectionId) &&
        Objects.equals(this.settings, appInstanceDeploymentSettingsUpdate.settings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionId, settings);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppInstanceDeploymentSettingsUpdateModel {\n");
    sb.append("    connectionId: ").append(toIndentedString(connectionId)).append("\n");
    sb.append("    settings: ").append(toIndentedString(settings)).append("\n");
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

