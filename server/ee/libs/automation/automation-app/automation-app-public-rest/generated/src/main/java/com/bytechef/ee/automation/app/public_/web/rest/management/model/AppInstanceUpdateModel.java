package com.bytechef.ee.automation.app.public_.web.rest.management.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppHostingTargetModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.lang.Nullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The values to change on an app instance. Every field is optional; an absent field keeps its stored value.
 */

@Schema(name = "AppInstanceUpdate", description = "The values to change on an app instance. Every field is optional; an absent field keeps its stored value.")
@JsonTypeName("AppInstanceUpdate")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-17T03:16:20.884871+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class AppInstanceUpdateModel {

  private @Nullable String name;

  private @Nullable AppHostingTargetModel hostingTarget;

  private @Nullable String url;

  private JsonNullable<Map<String, Object>> _configuration = JsonNullable.<Map<String, Object>>undefined();

  public AppInstanceUpdateModel name(@Nullable String name) {
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

  public AppInstanceUpdateModel hostingTarget(@Nullable AppHostingTargetModel hostingTarget) {
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

  public AppInstanceUpdateModel url(@Nullable String url) {
    this.url = url;
    return this;
  }

  /**
   * The new url: an absolute http or https URL without user info, and https outside DEVELOPMENT while sign-in is on. Any other value answers 400 APP_INSTANCE_URL_INVALID. An empty string clears it.
   * @return url
   */
  @Size(max = 2048) 
  @Schema(name = "url", description = "The new url: an absolute http or https URL without user info, and https outside DEVELOPMENT while sign-in is on. Any other value answers 400 APP_INSTANCE_URL_INVALID. An empty string clears it.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("url")
  public @Nullable String getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(@Nullable String url) {
    this.url = url;
  }

  public AppInstanceUpdateModel _configuration(Map<String, Object> _configuration) {
    this._configuration = JsonNullable.of(_configuration);
    return this;
  }

  public AppInstanceUpdateModel putConfigurationItem(String key, Object _configurationItem) {
    if (this._configuration == null || !this._configuration.isPresent() || this._configuration.get() == null) {
      this._configuration = JsonNullable.of(new HashMap<>());
    }
    this._configuration.get().put(key, _configurationItem);
    return this;
  }

  /**
   * A JSON object of at most 64 KB that replaces the stored configuration. An empty object clears it; absent or null keeps it. It is applied with the other fields in one transaction. It is visible to everyone who can view the instance and must not hold secrets.
   * @return _configuration
   */
  
  @Schema(name = "configuration", description = "A JSON object of at most 64 KB that replaces the stored configuration. An empty object clears it; absent or null keeps it. It is applied with the other fields in one transaction. It is visible to everyone who can view the instance and must not hold secrets.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("configuration")
  public JsonNullable<Map<String, Object>> getConfiguration() {
    return _configuration;
  }

  public void setConfiguration(JsonNullable<Map<String, Object>> _configuration) {
    this._configuration = _configuration;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppInstanceUpdateModel appInstanceUpdate = (AppInstanceUpdateModel) o;
    return Objects.equals(this.name, appInstanceUpdate.name) &&
        Objects.equals(this.hostingTarget, appInstanceUpdate.hostingTarget) &&
        Objects.equals(this.url, appInstanceUpdate.url) &&
        equalsNullable(this._configuration, appInstanceUpdate._configuration);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, hostingTarget, url, hashCodeNullable(_configuration));
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppInstanceUpdateModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    hostingTarget: ").append(toIndentedString(hostingTarget)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    _configuration: ").append(toIndentedString(_configuration)).append("\n");
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

