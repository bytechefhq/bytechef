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
 * UpdateConnectedUserRequestModel
 */

@JsonTypeName("UpdateConnectedUserRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-23T23:44:29.402755+02:00[Europe/Zagreb]", comments = "Generator version: 7.25.0")
public class UpdateConnectedUserRequestModel {

  private @Nullable String email;

  private Map<String, String> metadata = new HashMap<>();

  private @Nullable String name;

  private Integer version;

  public UpdateConnectedUserRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UpdateConnectedUserRequestModel(Integer version) {
    this.version = version;
  }

  public UpdateConnectedUserRequestModel email(@Nullable String email) {
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

  public UpdateConnectedUserRequestModel metadata(Map<String, String> metadata) {
    this.metadata = metadata;
    return this;
  }

  public UpdateConnectedUserRequestModel putMetadataItem(String key, String metadataItem) {
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

  public UpdateConnectedUserRequestModel name(@Nullable String name) {
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

  public UpdateConnectedUserRequestModel version(Integer version) {
    this.version = version;
    return this;
  }

  /**
   * The version of the connected user being edited.
   * @return version
   */
  @NotNull 
  @Schema(name = "version", description = "The version of the connected user being edited.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("version")
  public Integer getVersion() {
    return version;
  }

  @JsonProperty("version")
  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateConnectedUserRequestModel updateConnectedUserRequest = (UpdateConnectedUserRequestModel) o;
    return Objects.equals(this.email, updateConnectedUserRequest.email) &&
        Objects.equals(this.metadata, updateConnectedUserRequest.metadata) &&
        Objects.equals(this.name, updateConnectedUserRequest.name) &&
        Objects.equals(this.version, updateConnectedUserRequest.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(email, metadata, name, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateConnectedUserRequestModel {\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
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

