package com.bytechef.ee.embedded.security.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Contains generated public key used for signing JWT tokens.
 */

@Schema(name = "SigningKey", description = "Contains generated public key used for signing JWT tokens.")
@JsonTypeName("SigningKey")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-23T12:36:59.376196+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class SigningKeyModel {

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private @Nullable Long environmentId;

  private @Nullable Long id;

  private String keyId;

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastUsedDate;

  private String name;

  public SigningKeyModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SigningKeyModel(String keyId, String name) {
    this.keyId = keyId;
    this.name = name;
  }

  public SigningKeyModel createdBy(@Nullable String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * The created by.
   * @return createdBy
   */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The created by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdBy")
  public @Nullable String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(@Nullable String createdBy) {
    this.createdBy = createdBy;
  }

  public SigningKeyModel createdDate(@Nullable OffsetDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  /**
   * The created date.
   * @return createdDate
   */
  @Valid 
  @Schema(name = "createdDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The created date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdDate")
  public @Nullable OffsetDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(@Nullable OffsetDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public SigningKeyModel environmentId(@Nullable Long environmentId) {
    this.environmentId = environmentId;
    return this;
  }

  /**
   * The id of an environment.
   * @return environmentId
   */
  
  @Schema(name = "environmentId", description = "The id of an environment.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("environmentId")
  public @Nullable Long getEnvironmentId() {
    return environmentId;
  }

  public void setEnvironmentId(@Nullable Long environmentId) {
    this.environmentId = environmentId;
  }

  public SigningKeyModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a public key.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a public key.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public SigningKeyModel keyId(String keyId) {
    this.keyId = keyId;
    return this;
  }

  /**
   * The id of a key used for identifying corresponding private key when validating the JWT token.
   * @return keyId
   */
  
  @Schema(name = "keyId", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a key used for identifying corresponding private key when validating the JWT token.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("keyId")
  public String getKeyId() {
    return keyId;
  }

  public void setKeyId(String keyId) {
    this.keyId = keyId;
  }

  public SigningKeyModel lastModifiedBy(@Nullable String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  /**
   * The last modified by.
   * @return lastModifiedBy
   */
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedBy")
  public @Nullable String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(@Nullable String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public SigningKeyModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
    return this;
  }

  /**
   * The last modified date.
   * @return lastModifiedDate
   */
  @Valid 
  @Schema(name = "lastModifiedDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedDate")
  public @Nullable OffsetDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public SigningKeyModel lastUsedDate(@Nullable OffsetDateTime lastUsedDate) {
    this.lastUsedDate = lastUsedDate;
    return this;
  }

  /**
   * The last used date.
   * @return lastUsedDate
   */
  @Valid 
  @Schema(name = "lastUsedDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The last used date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastUsedDate")
  public @Nullable OffsetDateTime getLastUsedDate() {
    return lastUsedDate;
  }

  public void setLastUsedDate(@Nullable OffsetDateTime lastUsedDate) {
    this.lastUsedDate = lastUsedDate;
  }

  public SigningKeyModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of a public key.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The name of a public key.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
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
    SigningKeyModel signingKey = (SigningKeyModel) o;
    return Objects.equals(this.createdBy, signingKey.createdBy) &&
        Objects.equals(this.createdDate, signingKey.createdDate) &&
        Objects.equals(this.environmentId, signingKey.environmentId) &&
        Objects.equals(this.id, signingKey.id) &&
        Objects.equals(this.keyId, signingKey.keyId) &&
        Objects.equals(this.lastModifiedBy, signingKey.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, signingKey.lastModifiedDate) &&
        Objects.equals(this.lastUsedDate, signingKey.lastUsedDate) &&
        Objects.equals(this.name, signingKey.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, environmentId, id, keyId, lastModifiedBy, lastModifiedDate, lastUsedDate, name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SigningKeyModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    environmentId: ").append(toIndentedString(environmentId)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    keyId: ").append(toIndentedString(keyId)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    lastUsedDate: ").append(toIndentedString(lastUsedDate)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

