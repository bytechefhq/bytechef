package com.bytechef.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.embedded.configuration.public_.web.rest.model.CredentialStatusModel;
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
 * Contains all required information to open a connection to a service defined by componentName parameter.
 */

@Schema(name = "Connection", description = "Contains all required information to open a connection to a service defined by componentName parameter.")
@JsonTypeName("Connection")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-01-26T16:38:58.839699+01:00[Europe/Zagreb]", comments = "Generator version: 7.11.0")
public class ConnectionModel {

  private @Nullable Boolean active;

  private @Nullable Integer connectionVersion;

  private @Nullable CredentialStatusModel credentialStatus;

  private @Nullable Long id;

  public ConnectionModel active(Boolean active) {
    this.active = active;
    return this;
  }

  /**
   * If a connection is used in any of active workflows.
   * @return active
   */
  
  @Schema(name = "active", accessMode = Schema.AccessMode.READ_ONLY, description = "If a connection is used in any of active workflows.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("active")
  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public ConnectionModel connectionVersion(Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
    return this;
  }

  /**
   * The version of a component that uses this connection.
   * @return connectionVersion
   */
  
  @Schema(name = "connectionVersion", accessMode = Schema.AccessMode.READ_ONLY, description = "The version of a component that uses this connection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionVersion")
  public Integer getConnectionVersion() {
    return connectionVersion;
  }

  public void setConnectionVersion(Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
  }

  public ConnectionModel credentialStatus(CredentialStatusModel credentialStatus) {
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
  public CredentialStatusModel getCredentialStatus() {
    return credentialStatus;
  }

  public void setCredentialStatus(CredentialStatusModel credentialStatus) {
    this.credentialStatus = credentialStatus;
  }

  public ConnectionModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a connection.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a connection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConnectionModel connection = (ConnectionModel) o;
    return Objects.equals(this.active, connection.active) &&
        Objects.equals(this.connectionVersion, connection.connectionVersion) &&
        Objects.equals(this.credentialStatus, connection.credentialStatus) &&
        Objects.equals(this.id, connection.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(active, connectionVersion, credentialStatus, id);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectionModel {\n");
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
    sb.append("    connectionVersion: ").append(toIndentedString(connectionVersion)).append("\n");
    sb.append("    credentialStatus: ").append(toIndentedString(credentialStatus)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
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

