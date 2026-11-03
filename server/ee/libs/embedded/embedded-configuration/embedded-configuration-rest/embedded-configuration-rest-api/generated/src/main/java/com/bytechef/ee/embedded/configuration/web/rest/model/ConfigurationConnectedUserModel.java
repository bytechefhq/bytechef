package com.bytechef.ee.embedded.configuration.web.rest.model;

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
 * A connected user with an integration instance on a configuration.
 */

@Schema(name = "ConfigurationConnectedUser", description = "A connected user with an integration instance on a configuration.")
@JsonTypeName("ConfigurationConnectedUser")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-03T02:20:55.684519+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ConfigurationConnectedUserModel {

  private Long connectedUserId;

  private @Nullable String email;

  private String externalId;

  private @Nullable String name;

  public ConfigurationConnectedUserModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ConfigurationConnectedUserModel(Long connectedUserId, String externalId) {
    this.connectedUserId = connectedUserId;
    this.externalId = externalId;
  }

  public ConfigurationConnectedUserModel connectedUserId(Long connectedUserId) {
    this.connectedUserId = connectedUserId;
    return this;
  }

  /**
   * The id of the connected user.
   * @return connectedUserId
   */
  @NotNull 
  @Schema(name = "connectedUserId", description = "The id of the connected user.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connectedUserId")
  public Long getConnectedUserId() {
    return connectedUserId;
  }

  @JsonProperty("connectedUserId")
  public void setConnectedUserId(Long connectedUserId) {
    this.connectedUserId = connectedUserId;
  }

  public ConfigurationConnectedUserModel email(@Nullable String email) {
    this.email = email;
    return this;
  }

  /**
   * The email of the connected user.
   * @return email
   */
  
  @Schema(name = "email", description = "The email of the connected user.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("email")
  public @Nullable String getEmail() {
    return email;
  }

  @JsonProperty("email")
  public void setEmail(@Nullable String email) {
    this.email = email;
  }

  public ConfigurationConnectedUserModel externalId(String externalId) {
    this.externalId = externalId;
    return this;
  }

  /**
   * The external id of the connected user.
   * @return externalId
   */
  @NotNull 
  @Schema(name = "externalId", description = "The external id of the connected user.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("externalId")
  public String getExternalId() {
    return externalId;
  }

  @JsonProperty("externalId")
  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  public ConfigurationConnectedUserModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the connected user.
   * @return name
   */
  
  @Schema(name = "name", description = "The name of the connected user.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConfigurationConnectedUserModel configurationConnectedUser = (ConfigurationConnectedUserModel) o;
    return Objects.equals(this.connectedUserId, configurationConnectedUser.connectedUserId) &&
        Objects.equals(this.email, configurationConnectedUser.email) &&
        Objects.equals(this.externalId, configurationConnectedUser.externalId) &&
        Objects.equals(this.name, configurationConnectedUser.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectedUserId, email, externalId, name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConfigurationConnectedUserModel {\n");
    sb.append("    connectedUserId: ").append(toIndentedString(connectedUserId)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    externalId: ").append(toIndentedString(externalId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

