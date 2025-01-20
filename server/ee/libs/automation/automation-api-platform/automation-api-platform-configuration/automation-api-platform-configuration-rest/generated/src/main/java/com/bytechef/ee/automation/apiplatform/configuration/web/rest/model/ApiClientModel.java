package com.bytechef.ee.automation.apiplatform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.OffsetDateTime;
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

@Schema(name = "ApiClient", description = "Contains generated key required for calling API.")
@JsonTypeName("ApiClient")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-01-20T07:12:02.026867+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public class ApiClientModel {

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdDate;

  private Long id;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime lastModifiedDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime lastUsedDate;

  private String name;

  private String secretKey;

  public ApiClientModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ApiClientModel(String name, String secretKey) {
    this.name = name;
    this.secretKey = secretKey;
  }

  public ApiClientModel createdBy(String createdBy) {
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

  public ApiClientModel createdDate(OffsetDateTime createdDate) {
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
  public OffsetDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(OffsetDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public ApiClientModel id(Long id) {
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

  public ApiClientModel lastModifiedBy(String lastModifiedBy) {
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

  public ApiClientModel lastModifiedDate(OffsetDateTime lastModifiedDate) {
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
  public OffsetDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public ApiClientModel lastUsedDate(OffsetDateTime lastUsedDate) {
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
  public OffsetDateTime getLastUsedDate() {
    return lastUsedDate;
  }

  public void setLastUsedDate(OffsetDateTime lastUsedDate) {
    this.lastUsedDate = lastUsedDate;
  }

  public ApiClientModel name(String name) {
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

  public ApiClientModel secretKey(String secretKey) {
    this.secretKey = secretKey;
    return this;
  }

  /**
   * The preview of secret API key.
   * @return secretKey
   */
  
  @Schema(name = "secretKey", accessMode = Schema.AccessMode.READ_ONLY, description = "The preview of secret API key.", requiredMode = Schema.RequiredMode.REQUIRED)
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
    ApiClientModel _apiClient = (ApiClientModel) o;
    return Objects.equals(this.createdBy, _apiClient.createdBy) &&
        Objects.equals(this.createdDate, _apiClient.createdDate) &&
        Objects.equals(this.id, _apiClient.id) &&
        Objects.equals(this.lastModifiedBy, _apiClient.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, _apiClient.lastModifiedDate) &&
        Objects.equals(this.lastUsedDate, _apiClient.lastUsedDate) &&
        Objects.equals(this.name, _apiClient.name) &&
        Objects.equals(this.secretKey, _apiClient.secretKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, id, lastModifiedBy, lastModifiedDate, lastUsedDate, name, secretKey);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiClientModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
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

