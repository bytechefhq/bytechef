package com.bytechef.ee.embedded.connected.user.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.connected.user.web.rest.model.CredentialStatusModel;
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
 * ConnectedUserIntegrationInstanceModel
 */

@JsonTypeName("ConnectedUserIntegrationInstance")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-02T08:44:56.368618+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class ConnectedUserIntegrationInstanceModel {

  private @Nullable String componentName;

  private @Nullable Boolean enabled;

  private @Nullable Long id;

  private @Nullable Long integrationId;

  private @Nullable Long integrationInstanceConfigurationId;

  private @Nullable Integer integrationVersion;

  private @Nullable Long connectionId;

  private @Nullable CredentialStatusModel credentialStatus;

  public ConnectedUserIntegrationInstanceModel componentName(@Nullable String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of a component.
   * @return componentName
   */
  
  @Schema(name = "componentName", description = "The name of a component.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentName")
  public @Nullable String getComponentName() {
    return componentName;
  }

  public void setComponentName(@Nullable String componentName) {
    this.componentName = componentName;
  }

  public ConnectedUserIntegrationInstanceModel enabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If an integration's instance is enable dor not.
   * @return enabled
   */
  
  @Schema(name = "enabled", description = "If an integration's instance is enable dor not.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public @Nullable Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
  }

  public ConnectedUserIntegrationInstanceModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an integration instance.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an integration instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public ConnectedUserIntegrationInstanceModel integrationId(@Nullable Long integrationId) {
    this.integrationId = integrationId;
    return this;
  }

  /**
   * The id of an integration.
   * @return integrationId
   */
  
  @Schema(name = "integrationId", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationId")
  public @Nullable Long getIntegrationId() {
    return integrationId;
  }

  public void setIntegrationId(@Nullable Long integrationId) {
    this.integrationId = integrationId;
  }

  public ConnectedUserIntegrationInstanceModel integrationInstanceConfigurationId(@Nullable Long integrationInstanceConfigurationId) {
    this.integrationInstanceConfigurationId = integrationInstanceConfigurationId;
    return this;
  }

  /**
   * The id of an integration instance configuration.
   * @return integrationInstanceConfigurationId
   */
  
  @Schema(name = "integrationInstanceConfigurationId", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an integration instance configuration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationInstanceConfigurationId")
  public @Nullable Long getIntegrationInstanceConfigurationId() {
    return integrationInstanceConfigurationId;
  }

  public void setIntegrationInstanceConfigurationId(@Nullable Long integrationInstanceConfigurationId) {
    this.integrationInstanceConfigurationId = integrationInstanceConfigurationId;
  }

  public ConnectedUserIntegrationInstanceModel integrationVersion(@Nullable Integer integrationVersion) {
    this.integrationVersion = integrationVersion;
    return this;
  }

  /**
   * The version of an integration.
   * @return integrationVersion
   */
  
  @Schema(name = "integrationVersion", accessMode = Schema.AccessMode.READ_ONLY, description = "The version of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationVersion")
  public @Nullable Integer getIntegrationVersion() {
    return integrationVersion;
  }

  public void setIntegrationVersion(@Nullable Integer integrationVersion) {
    this.integrationVersion = integrationVersion;
  }

  public ConnectedUserIntegrationInstanceModel connectionId(@Nullable Long connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  /**
   * The id of a connection.
   * @return connectionId
   */
  
  @Schema(name = "connectionId", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a connection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionId")
  public @Nullable Long getConnectionId() {
    return connectionId;
  }

  public void setConnectionId(@Nullable Long connectionId) {
    this.connectionId = connectionId;
  }

  public ConnectedUserIntegrationInstanceModel credentialStatus(@Nullable CredentialStatusModel credentialStatus) {
    this.credentialStatus = credentialStatus;
    return this;
  }

  /**
   * Get credentialStatus
   * @return credentialStatus
   */
  @Valid 
  @Schema(name = "credentialStatus", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("credentialStatus")
  public @Nullable CredentialStatusModel getCredentialStatus() {
    return credentialStatus;
  }

  public void setCredentialStatus(@Nullable CredentialStatusModel credentialStatus) {
    this.credentialStatus = credentialStatus;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConnectedUserIntegrationInstanceModel connectedUserIntegrationInstance = (ConnectedUserIntegrationInstanceModel) o;
    return Objects.equals(this.componentName, connectedUserIntegrationInstance.componentName) &&
        Objects.equals(this.enabled, connectedUserIntegrationInstance.enabled) &&
        Objects.equals(this.id, connectedUserIntegrationInstance.id) &&
        Objects.equals(this.integrationId, connectedUserIntegrationInstance.integrationId) &&
        Objects.equals(this.integrationInstanceConfigurationId, connectedUserIntegrationInstance.integrationInstanceConfigurationId) &&
        Objects.equals(this.integrationVersion, connectedUserIntegrationInstance.integrationVersion) &&
        Objects.equals(this.connectionId, connectedUserIntegrationInstance.connectionId) &&
        Objects.equals(this.credentialStatus, connectedUserIntegrationInstance.credentialStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, enabled, id, integrationId, integrationInstanceConfigurationId, integrationVersion, connectionId, credentialStatus);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectedUserIntegrationInstanceModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    integrationId: ").append(toIndentedString(integrationId)).append("\n");
    sb.append("    integrationInstanceConfigurationId: ").append(toIndentedString(integrationInstanceConfigurationId)).append("\n");
    sb.append("    integrationVersion: ").append(toIndentedString(integrationVersion)).append("\n");
    sb.append("    connectionId: ").append(toIndentedString(connectionId)).append("\n");
    sb.append("    credentialStatus: ").append(toIndentedString(credentialStatus)).append("\n");
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

