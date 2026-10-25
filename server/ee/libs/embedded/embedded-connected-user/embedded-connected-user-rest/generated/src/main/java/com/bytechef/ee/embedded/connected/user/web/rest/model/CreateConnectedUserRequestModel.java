package com.bytechef.ee.embedded.connected.user.web.rest.model;

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
 * CreateConnectedUserRequestModel
 */

@JsonTypeName("CreateConnectedUserRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-23T23:44:29.402755+02:00[Europe/Zagreb]", comments = "Generator version: 7.25.0")
public class CreateConnectedUserRequestModel {

  private @Nullable String email;

  private Long environmentId;

  private String externalId;

  private Map<String, String> metadata = new HashMap<>();

  private @Nullable String name;

  public CreateConnectedUserRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateConnectedUserRequestModel(Long environmentId, String externalId) {
    this.environmentId = environmentId;
    this.externalId = externalId;
  }

  public CreateConnectedUserRequestModel email(@Nullable String email) {
    this.email = email;
    return this;
  }

  /**
   * The email address.
   * @return email
   */
  
  @Schema(name = "email", description = "The email address.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("email")
  public @Nullable String getEmail() {
    return email;
  }

  @JsonProperty("email")
  public void setEmail(@Nullable String email) {
    this.email = email;
  }

  public CreateConnectedUserRequestModel environmentId(Long environmentId) {
    this.environmentId = environmentId;
    return this;
  }

  /**
   * The id of an environment.
   * @return environmentId
   */
  @NotNull 
  @Schema(name = "environmentId", description = "The id of an environment.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("environmentId")
  public Long getEnvironmentId() {
    return environmentId;
  }

  @JsonProperty("environmentId")
  public void setEnvironmentId(Long environmentId) {
    this.environmentId = environmentId;
  }

  public CreateConnectedUserRequestModel externalId(String externalId) {
    this.externalId = externalId;
    return this;
  }

  /**
   * The connected user external id.
   * @return externalId
   */
  @NotNull 
  @Schema(name = "externalId", description = "The connected user external id.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("externalId")
  public String getExternalId() {
    return externalId;
  }

  @JsonProperty("externalId")
  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  public CreateConnectedUserRequestModel metadata(Map<String, String> metadata) {
    this.metadata = metadata;
    return this;
  }

  public CreateConnectedUserRequestModel putMetadataItem(String key, String metadataItem) {
    if (this.metadata == null) {
      this.metadata = new HashMap<>();
    }
    this.metadata.put(key, metadataItem);
    return this;
  }

  /**
   * The metadata.
   * @return metadata
   */
  
  @Schema(name = "metadata", description = "The metadata.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("metadata")
  public Map<String, String> getMetadata() {
    return metadata;
  }

  @JsonProperty("metadata")
  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  public CreateConnectedUserRequestModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of a connected user.
   * @return name
   */
  
  @Schema(name = "name", description = "The name of a connected user.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    CreateConnectedUserRequestModel createConnectedUserRequest = (CreateConnectedUserRequestModel) o;
    return Objects.equals(this.email, createConnectedUserRequest.email) &&
        Objects.equals(this.environmentId, createConnectedUserRequest.environmentId) &&
        Objects.equals(this.externalId, createConnectedUserRequest.externalId) &&
        Objects.equals(this.metadata, createConnectedUserRequest.metadata) &&
        Objects.equals(this.name, createConnectedUserRequest.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(email, environmentId, externalId, metadata, name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateConnectedUserRequestModel {\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    environmentId: ").append(toIndentedString(environmentId)).append("\n");
    sb.append("    externalId: ").append(toIndentedString(externalId)).append("\n");
    sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
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

