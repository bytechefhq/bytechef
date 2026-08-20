package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
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
 * CreateConnectionRequestModel
 */

@JsonTypeName("CreateConnectionRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-18T00:19:21.376010+02:00[Europe/Zagreb]", comments = "Generator version: 7.24.0")
public class CreateConnectionRequestModel {

  private String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String authorizationType;

  private Integer connectionVersion;

  private Map<String, Object> parameters = new HashMap<>();

  public CreateConnectionRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateConnectionRequestModel(String name, Integer connectionVersion, Map<String, Object> parameters) {
    this.name = name;
    this.connectionVersion = connectionVersion;
    this.parameters = parameters;
  }

  public CreateConnectionRequestModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public CreateConnectionRequestModel authorizationType(@Nullable String authorizationType) {
    this.authorizationType = authorizationType;
    return this;
  }

  /**
   * The authorization type name; null for connections without authorization.
   * @return authorizationType
   */
  
  @Schema(name = "authorizationType", description = "The authorization type name; null for connections without authorization.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("authorizationType")
  public @Nullable String getAuthorizationType() {
    return authorizationType;
  }

  @JsonProperty("authorizationType")
  public void setAuthorizationType(@Nullable String authorizationType) {
    this.authorizationType = authorizationType;
  }

  public CreateConnectionRequestModel connectionVersion(Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
    return this;
  }

  /**
   * Get connectionVersion
   * @return connectionVersion
   */
  @NotNull 
  @Schema(name = "connectionVersion", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connectionVersion")
  public Integer getConnectionVersion() {
    return connectionVersion;
  }

  @JsonProperty("connectionVersion")
  public void setConnectionVersion(Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
  }

  public CreateConnectionRequestModel parameters(Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  public CreateConnectionRequestModel putParametersItem(String key, Object parametersItem) {
    if (this.parameters == null) {
      this.parameters = new HashMap<>();
    }
    this.parameters.put(key, parametersItem);
    return this;
  }

  /**
   * Get parameters
   * @return parameters
   */
  @NotNull 
  @Schema(name = "parameters", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("parameters")
  public Map<String, Object> getParameters() {
    return parameters;
  }

  @JsonProperty("parameters")
  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateConnectionRequestModel createConnectionRequest = (CreateConnectionRequestModel) o;
    return Objects.equals(this.name, createConnectionRequest.name) &&
        Objects.equals(this.authorizationType, createConnectionRequest.authorizationType) &&
        Objects.equals(this.connectionVersion, createConnectionRequest.connectionVersion) &&
        Objects.equals(this.parameters, createConnectionRequest.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, authorizationType, connectionVersion, parameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateConnectionRequestModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    authorizationType: ").append(toIndentedString(authorizationType)).append("\n");
    sb.append("    connectionVersion: ").append(toIndentedString(connectionVersion)).append("\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
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

