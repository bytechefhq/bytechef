package com.bytechef.automation.user.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.automation.user.web.rest.model.EnvironmentModel;
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
 * Contains generated key required for calling API.
 */

@Schema(name = "ApiKey", description = "Contains generated key required for calling API.")
@JsonTypeName("ApiKey")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-04-21T11:16:38.454213+02:00[Europe/Zagreb]", comments = "Generator version: 7.4.0")
public class ApiKeyModel {

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

  private String secretKey;

  public ApiKeyModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ApiKeyModel(String name) {
    this.name = name;
  }

  public ApiKeyModel createdBy(String createdBy) {
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

  public ApiKeyModel createdDate(LocalDateTime createdDate) {
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

  public ApiKeyModel environment(EnvironmentModel environment) {
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

  public ApiKeyModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an API key.
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an API key.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ApiKeyModel lastModifiedBy(String lastModifiedBy) {
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

  public ApiKeyModel lastModifiedDate(LocalDateTime lastModifiedDate) {
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

  public ApiKeyModel lastUsedDate(LocalDateTime lastUsedDate) {
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

  public ApiKeyModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of an API key.
   * @return name
  */
  @NotNull 
  @Schema(name = "name", description = "The name of an API key.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiKeyModel secretKey(String secretKey) {
    this.secretKey = secretKey;
    return this;
  }

  /**
   * The preview of secret API key.
   * @return secretKey
  */
  
  @Schema(name = "secretKey", description = "The preview of secret API key.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("secretKey")
  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiKeyModel apiKey = (ApiKeyModel) o;
    return Objects.equals(this.createdBy, apiKey.createdBy) &&
        Objects.equals(this.createdDate, apiKey.createdDate) &&
        Objects.equals(this.environment, apiKey.environment) &&
        Objects.equals(this.id, apiKey.id) &&
        Objects.equals(this.lastModifiedBy, apiKey.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, apiKey.lastModifiedDate) &&
        Objects.equals(this.lastUsedDate, apiKey.lastUsedDate) &&
        Objects.equals(this.name, apiKey.name) &&
        Objects.equals(this.secretKey, apiKey.secretKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, environment, id, lastModifiedBy, lastModifiedDate, lastUsedDate, name, secretKey);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiKeyModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    environment: ").append(toIndentedString(environment)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    lastUsedDate: ").append(toIndentedString(lastUsedDate)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    secretKey: ").append(toIndentedString(secretKey)).append("\n");
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

