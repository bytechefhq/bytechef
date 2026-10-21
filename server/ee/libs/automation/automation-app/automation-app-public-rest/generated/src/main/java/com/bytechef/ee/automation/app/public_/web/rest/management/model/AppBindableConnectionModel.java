package com.bytechef.ee.automation.app.public_.web.rest.management.model;

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
 * A connection that can be bound to an app.
 */

@Schema(name = "AppBindableConnection", description = "A connection that can be bound to an app.")
@JsonTypeName("AppBindableConnection")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:30.252991+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class AppBindableConnectionModel {

  private @Nullable Long id;

  private @Nullable String name;

  private @Nullable String componentName;

  private @Nullable Integer connectionVersion;

  private @Nullable Integer componentVersion;

  private @Nullable Boolean usable;

  public AppBindableConnectionModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  
  @Schema(name = "id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public AppBindableConnectionModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  
  @Schema(name = "name", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public AppBindableConnectionModel componentName(@Nullable String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * Get componentName
   * @return componentName
   */
  
  @Schema(name = "componentName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentName")
  public @Nullable String getComponentName() {
    return componentName;
  }

  @JsonProperty("componentName")
  public void setComponentName(@Nullable String componentName) {
    this.componentName = componentName;
  }

  public AppBindableConnectionModel connectionVersion(@Nullable Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
    return this;
  }

  /**
   * Get connectionVersion
   * @return connectionVersion
   */
  
  @Schema(name = "connectionVersion", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionVersion")
  public @Nullable Integer getConnectionVersion() {
    return connectionVersion;
  }

  @JsonProperty("connectionVersion")
  public void setConnectionVersion(@Nullable Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
  }

  public AppBindableConnectionModel componentVersion(@Nullable Integer componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * Get componentVersion
   * @return componentVersion
   */
  
  @Schema(name = "componentVersion", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentVersion")
  public @Nullable Integer getComponentVersion() {
    return componentVersion;
  }

  @JsonProperty("componentVersion")
  public void setComponentVersion(@Nullable Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  public AppBindableConnectionModel usable(@Nullable Boolean usable) {
    this.usable = usable;
    return this;
  }

  /**
   * Whether the connection's credentials are valid and the caller may use it.
   * @return usable
   */
  
  @Schema(name = "usable", description = "Whether the connection's credentials are valid and the caller may use it.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("usable")
  public @Nullable Boolean getUsable() {
    return usable;
  }

  @JsonProperty("usable")
  public void setUsable(@Nullable Boolean usable) {
    this.usable = usable;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppBindableConnectionModel appBindableConnection = (AppBindableConnectionModel) o;
    return Objects.equals(this.id, appBindableConnection.id) &&
        Objects.equals(this.name, appBindableConnection.name) &&
        Objects.equals(this.componentName, appBindableConnection.componentName) &&
        Objects.equals(this.connectionVersion, appBindableConnection.connectionVersion) &&
        Objects.equals(this.componentVersion, appBindableConnection.componentVersion) &&
        Objects.equals(this.usable, appBindableConnection.usable);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, componentName, connectionVersion, componentVersion, usable);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppBindableConnectionModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    connectionVersion: ").append(toIndentedString(connectionVersion)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    usable: ").append(toIndentedString(usable)).append("\n");
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

