package com.bytechef.embedded.user.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.embedded.user.web.rest.model.EnvironmentModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-26T11:09:38.425772+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public class SigningKeyModel {

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  private EnvironmentModel environment;

  private Long id;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastUsedDate;

  private String name;

  public SigningKeyModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SigningKeyModel(String name) {
    this.name = name;
  }

  public SigningKeyModel createdBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * The created by.
   * @return createdBy
  */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The created by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdBy")
  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public SigningKeyModel createdDate(LocalDateTime createdDate) {
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
  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public SigningKeyModel environment(EnvironmentModel environment) {
    this.environment = environment;
    return this;
  }

  /**
   * Get environment
   * @return environment
  */
  @Valid 
  @Schema(name = "environment", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("environment")
  public EnvironmentModel getEnvironment() {
    return environment;
  }

  public void setEnvironment(EnvironmentModel environment) {
    this.environment = environment;
  }

  public SigningKeyModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a public key.
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a public key.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public SigningKeyModel lastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  /**
   * The last modified by.
   * @return lastModifiedBy
  */
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedBy")
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public SigningKeyModel lastModifiedDate(LocalDateTime lastModifiedDate) {
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
  public LocalDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public SigningKeyModel lastUsedDate(LocalDateTime lastUsedDate) {
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
  public LocalDateTime getLastUsedDate() {
    return lastUsedDate;
  }

  public void setLastUsedDate(LocalDateTime lastUsedDate) {
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
        Objects.equals(this.environment, signingKey.environment) &&
        Objects.equals(this.id, signingKey.id) &&
        Objects.equals(this.lastModifiedBy, signingKey.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, signingKey.lastModifiedDate) &&
        Objects.equals(this.lastUsedDate, signingKey.lastUsedDate) &&
        Objects.equals(this.name, signingKey.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, environment, id, lastModifiedBy, lastModifiedDate, lastUsedDate, name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SigningKeyModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    environment: ").append(toIndentedString(environment)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
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

