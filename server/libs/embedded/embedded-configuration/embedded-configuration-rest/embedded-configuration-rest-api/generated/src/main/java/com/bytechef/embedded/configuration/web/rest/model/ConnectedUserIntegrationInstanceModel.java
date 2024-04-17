package com.bytechef.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-04-21T11:42:41.592087+02:00[Europe/Zagreb]", comments = "Generator version: 7.4.0")
public class ConnectedUserIntegrationInstanceModel {

  private String componentName;

  private Boolean enabled;

  private Long id;

  private Long integrationId;

  private Integer integrationVersion;

  private Long connectionId;

  private com.bytechef.platform.connection.web.rest.model.CredentialStatusModel credentialStatus;

  public ConnectedUserIntegrationInstanceModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of a component.
   * @return componentName
  */
  
  @Schema(name = "componentName", description = "The name of a component.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public ConnectedUserIntegrationInstanceModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If an integration's instance is enable dor not.
   * @return enabled
  */
  
  @Schema(name = "enabled", description = "If an integration's instance is enable dor not.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public ConnectedUserIntegrationInstanceModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an integration instance.
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an integration instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ConnectedUserIntegrationInstanceModel integrationId(Long integrationId) {
    this.integrationId = integrationId;
    return this;
  }

  /**
   * The id of an integration.
   * @return integrationId
  */
  
  @Schema(name = "integrationId", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationId")
  public Long getIntegrationId() {
    return integrationId;
  }

  public void setIntegrationId(Long integrationId) {
    this.integrationId = integrationId;
  }

  public ConnectedUserIntegrationInstanceModel integrationVersion(Integer integrationVersion) {
    this.integrationVersion = integrationVersion;
    return this;
  }

  /**
   * The version of an integration.
   * @return integrationVersion
  */
  
  @Schema(name = "integrationVersion", accessMode = Schema.AccessMode.READ_ONLY, description = "The version of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationVersion")
  public Integer getIntegrationVersion() {
    return integrationVersion;
  }

  public void setIntegrationVersion(Integer integrationVersion) {
    this.integrationVersion = integrationVersion;
  }

  public ConnectedUserIntegrationInstanceModel connectionId(Long connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  /**
   * The id of a connection.
   * @return connectionId
  */
  
  @Schema(name = "connectionId", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a connection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionId")
  public Long getConnectionId() {
    return connectionId;
  }

  public void setConnectionId(Long connectionId) {
    this.connectionId = connectionId;
  }

  public ConnectedUserIntegrationInstanceModel credentialStatus(com.bytechef.platform.connection.web.rest.model.CredentialStatusModel credentialStatus) {
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
  public com.bytechef.platform.connection.web.rest.model.CredentialStatusModel getCredentialStatus() {
    return credentialStatus;
  }

  public void setCredentialStatus(com.bytechef.platform.connection.web.rest.model.CredentialStatusModel credentialStatus) {
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
        Objects.equals(this.integrationVersion, connectedUserIntegrationInstance.integrationVersion) &&
        Objects.equals(this.connectionId, connectedUserIntegrationInstance.connectionId) &&
        Objects.equals(this.credentialStatus, connectedUserIntegrationInstance.credentialStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, enabled, id, integrationId, integrationVersion, connectionId, credentialStatus);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectedUserIntegrationInstanceModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    integrationId: ").append(toIndentedString(integrationId)).append("\n");
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

