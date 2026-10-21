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
 * A connection of a workspace. It never carries parameters or credentials.
 */

@Schema(name = "AppWorkspaceConnection", description = "A connection of a workspace. It never carries parameters or credentials.")
@JsonTypeName("AppWorkspaceConnection")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:30.252991+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class AppWorkspaceConnectionModel {

  private @Nullable Long id;

  private @Nullable String name;

  private @Nullable String componentName;

  private @Nullable Integer connectionVersion;

  private @Nullable String authorizationType;

  private @Nullable String credentialStatus;

  private @Nullable Boolean aiProvider;

  private @Nullable Boolean usable;

  public AppWorkspaceConnectionModel id(@Nullable Long id) {
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

  public AppWorkspaceConnectionModel name(@Nullable String name) {
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

  public AppWorkspaceConnectionModel componentName(@Nullable String componentName) {
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

  public AppWorkspaceConnectionModel connectionVersion(@Nullable Integer connectionVersion) {
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

  public AppWorkspaceConnectionModel authorizationType(@Nullable String authorizationType) {
    this.authorizationType = authorizationType;
    return this;
  }

  /**
   * Get authorizationType
   * @return authorizationType
   */
  
  @Schema(name = "authorizationType", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("authorizationType")
  public @Nullable String getAuthorizationType() {
    return authorizationType;
  }

  @JsonProperty("authorizationType")
  public void setAuthorizationType(@Nullable String authorizationType) {
    this.authorizationType = authorizationType;
  }

  public AppWorkspaceConnectionModel credentialStatus(@Nullable String credentialStatus) {
    this.credentialStatus = credentialStatus;
    return this;
  }

  /**
   * Get credentialStatus
   * @return credentialStatus
   */
  
  @Schema(name = "credentialStatus", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("credentialStatus")
  public @Nullable String getCredentialStatus() {
    return credentialStatus;
  }

  @JsonProperty("credentialStatus")
  public void setCredentialStatus(@Nullable String credentialStatus) {
    this.credentialStatus = credentialStatus;
  }

  public AppWorkspaceConnectionModel aiProvider(@Nullable Boolean aiProvider) {
    this.aiProvider = aiProvider;
    return this;
  }

  /**
   * Whether this is a virtual AI-provider connection, which never fills a deployment connection slot.
   * @return aiProvider
   */
  
  @Schema(name = "aiProvider", description = "Whether this is a virtual AI-provider connection, which never fills a deployment connection slot.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("aiProvider")
  public @Nullable Boolean getAiProvider() {
    return aiProvider;
  }

  @JsonProperty("aiProvider")
  public void setAiProvider(@Nullable Boolean aiProvider) {
    this.aiProvider = aiProvider;
  }

  public AppWorkspaceConnectionModel usable(@Nullable Boolean usable) {
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
    AppWorkspaceConnectionModel appWorkspaceConnection = (AppWorkspaceConnectionModel) o;
    return Objects.equals(this.id, appWorkspaceConnection.id) &&
        Objects.equals(this.name, appWorkspaceConnection.name) &&
        Objects.equals(this.componentName, appWorkspaceConnection.componentName) &&
        Objects.equals(this.connectionVersion, appWorkspaceConnection.connectionVersion) &&
        Objects.equals(this.authorizationType, appWorkspaceConnection.authorizationType) &&
        Objects.equals(this.credentialStatus, appWorkspaceConnection.credentialStatus) &&
        Objects.equals(this.aiProvider, appWorkspaceConnection.aiProvider) &&
        Objects.equals(this.usable, appWorkspaceConnection.usable);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, componentName, connectionVersion, authorizationType, credentialStatus, aiProvider, usable);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppWorkspaceConnectionModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    connectionVersion: ").append(toIndentedString(connectionVersion)).append("\n");
    sb.append("    authorizationType: ").append(toIndentedString(authorizationType)).append("\n");
    sb.append("    credentialStatus: ").append(toIndentedString(credentialStatus)).append("\n");
    sb.append("    aiProvider: ").append(toIndentedString(aiProvider)).append("\n");
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

