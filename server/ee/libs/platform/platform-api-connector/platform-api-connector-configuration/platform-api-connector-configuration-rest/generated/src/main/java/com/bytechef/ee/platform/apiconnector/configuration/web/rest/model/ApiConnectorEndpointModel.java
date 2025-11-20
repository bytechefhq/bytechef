package com.bytechef.ee.platform.apiconnector.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.platform.apiconnector.configuration.web.rest.model.HttpMethodModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * An API connector.
 */

@Schema(name = "ApiConnectorEndpoint", description = "An API connector.")
@JsonTypeName("ApiConnectorEndpoint")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:36.540292+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class ApiConnectorEndpointModel {

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private @Nullable String description;

  private @Nullable HttpMethodModel httpMethod;

  private @Nullable Long id;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastExecutionDate;

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  private String name;

  private @Nullable String path;

  private @Nullable Integer version;

  public ApiConnectorEndpointModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ApiConnectorEndpointModel(String name) {
    this.name = name;
  }

  public ApiConnectorEndpointModel createdBy(@Nullable String createdBy) {
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

  public ApiConnectorEndpointModel createdDate(@Nullable OffsetDateTime createdDate) {
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

  public ApiConnectorEndpointModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of an API connector's endpoint.
   * @return description
   */
  
  @Schema(name = "description", description = "The description of an API connector's endpoint.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public ApiConnectorEndpointModel httpMethod(@Nullable HttpMethodModel httpMethod) {
    this.httpMethod = httpMethod;
    return this;
  }

  /**
   * Get httpMethod
   * @return httpMethod
   */
  @Valid 
  @Schema(name = "httpMethod", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("httpMethod")
  public @Nullable HttpMethodModel getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(@Nullable HttpMethodModel httpMethod) {
    this.httpMethod = httpMethod;
  }

  public ApiConnectorEndpointModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of the API connector's endpoint.
   * @return id
   */
  
  @Schema(name = "id", description = "The id of the API connector's endpoint.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public ApiConnectorEndpointModel lastExecutionDate(@Nullable OffsetDateTime lastExecutionDate) {
    this.lastExecutionDate = lastExecutionDate;
    return this;
  }

  /**
   * The last execution date.
   * @return lastExecutionDate
   */
  @Valid 
  @Schema(name = "lastExecutionDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The last execution date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastExecutionDate")
  public @Nullable OffsetDateTime getLastExecutionDate() {
    return lastExecutionDate;
  }

  public void setLastExecutionDate(@Nullable OffsetDateTime lastExecutionDate) {
    this.lastExecutionDate = lastExecutionDate;
  }

  public ApiConnectorEndpointModel lastModifiedBy(@Nullable String lastModifiedBy) {
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

  public ApiConnectorEndpointModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
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

  public ApiConnectorEndpointModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of an API connector's endpoint.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The name of an API connector's endpoint.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiConnectorEndpointModel path(@Nullable String path) {
    this.path = path;
    return this;
  }

  /**
   * The path of an API connector's endpoint.
   * @return path
   */
  
  @Schema(name = "path", description = "The path of an API connector's endpoint.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("path")
  public @Nullable String getPath() {
    return path;
  }

  public void setPath(@Nullable String path) {
    this.path = path;
  }

  public ApiConnectorEndpointModel version(@Nullable Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * @return version
   */
  
  @Schema(name = "__version", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("__version")
  public @Nullable Integer getVersion() {
    return version;
  }

  public void setVersion(@Nullable Integer version) {
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
    ApiConnectorEndpointModel apiConnectorEndpoint = (ApiConnectorEndpointModel) o;
    return Objects.equals(this.createdBy, apiConnectorEndpoint.createdBy) &&
        Objects.equals(this.createdDate, apiConnectorEndpoint.createdDate) &&
        Objects.equals(this.description, apiConnectorEndpoint.description) &&
        Objects.equals(this.httpMethod, apiConnectorEndpoint.httpMethod) &&
        Objects.equals(this.id, apiConnectorEndpoint.id) &&
        Objects.equals(this.lastExecutionDate, apiConnectorEndpoint.lastExecutionDate) &&
        Objects.equals(this.lastModifiedBy, apiConnectorEndpoint.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, apiConnectorEndpoint.lastModifiedDate) &&
        Objects.equals(this.name, apiConnectorEndpoint.name) &&
        Objects.equals(this.path, apiConnectorEndpoint.path) &&
        Objects.equals(this.version, apiConnectorEndpoint.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, description, httpMethod, id, lastExecutionDate, lastModifiedBy, lastModifiedDate, name, path, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiConnectorEndpointModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    httpMethod: ").append(toIndentedString(httpMethod)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastExecutionDate: ").append(toIndentedString(lastExecutionDate)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
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

