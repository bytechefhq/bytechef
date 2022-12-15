package com.bytechef.hermes.connection.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * TODO
 */

@Schema(name = "Connection", description = "TODO")
@JsonTypeName("Connection")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-12-13T15:12:27.587783+01:00[Europe/Zagreb]")
public class ConnectionModel {

  @JsonProperty("authorizationName")
  private String authorizationName;

  @JsonProperty("componentName")
  private String componentName;

  @JsonProperty("componentVersion")
  private String componentVersion;

  @JsonProperty("createdBy")
  private String createdBy;

  @JsonProperty("createdDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  @JsonProperty("name")
  private String name;

  @JsonProperty("id")
  private String id;

  @JsonProperty("lastModifiedBy")
  private String lastModifiedBy;

  @JsonProperty("lastModifiedDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  @JsonProperty("parameters")
  @Valid
  private Map<String, Object> parameters = null;

  public ConnectionModel authorizationName(String authorizationName) {
    this.authorizationName = authorizationName;
    return this;
  }

  /**
   * TODO
   * @return authorizationName
  */
  
  @Schema(name = "authorizationName", description = "TODO", required = false)
  public String getAuthorizationName() {
    return authorizationName;
  }

  public void setAuthorizationName(String authorizationName) {
    this.authorizationName = authorizationName;
  }

  public ConnectionModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * TODO
   * @return componentName
  */
  
  @Schema(name = "componentName", description = "TODO", required = false)
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public ConnectionModel componentVersion(String componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * TODO
   * @return componentVersion
  */
  
  @Schema(name = "componentVersion", description = "TODO", required = false)
  public String getComponentVersion() {
    return componentVersion;
  }

  public void setComponentVersion(String componentVersion) {
    this.componentVersion = componentVersion;
  }

  public ConnectionModel createdBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * TODO
   * @return createdBy
  */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "TODO", required = false)
  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public ConnectionModel createdDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  /**
   * TODO
   * @return createdDate
  */
  @Valid 
  @Schema(name = "createdDate", accessMode = Schema.AccessMode.READ_ONLY, description = "TODO", required = false)
  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public ConnectionModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * TODO
   * @return name
  */
  
  @Schema(name = "name", description = "TODO", required = false)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ConnectionModel id(String id) {
    this.id = id;
    return this;
  }

  /**
   * TODO
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "TODO", required = false)
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public ConnectionModel lastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  /**
   * TODO
   * @return lastModifiedBy
  */
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, description = "TODO", required = false)
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public ConnectionModel lastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
    return this;
  }

  /**
   * TODO
   * @return lastModifiedDate
  */
  @Valid 
  @Schema(name = "lastModifiedDate", accessMode = Schema.AccessMode.READ_ONLY, description = "TODO", required = false)
  public LocalDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public ConnectionModel parameters(Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  public ConnectionModel putParametersItem(String key, Object parametersItem) {
    if (this.parameters == null) {
      this.parameters = new HashMap<>();
    }
    this.parameters.put(key, parametersItem);
    return this;
  }

  /**
   * TODO
   * @return parameters
  */
  
  @Schema(name = "parameters", description = "TODO", required = false)
  public Map<String, Object> getParameters() {
    return parameters;
  }

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
    ConnectionModel connection = (ConnectionModel) o;
    return Objects.equals(this.authorizationName, connection.authorizationName) &&
        Objects.equals(this.componentName, connection.componentName) &&
        Objects.equals(this.componentVersion, connection.componentVersion) &&
        Objects.equals(this.createdBy, connection.createdBy) &&
        Objects.equals(this.createdDate, connection.createdDate) &&
        Objects.equals(this.name, connection.name) &&
        Objects.equals(this.id, connection.id) &&
        Objects.equals(this.lastModifiedBy, connection.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, connection.lastModifiedDate) &&
        Objects.equals(this.parameters, connection.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorizationName, componentName, componentVersion, createdBy, createdDate, name, id, lastModifiedBy, lastModifiedDate, parameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectionModel {\n");
    sb.append("    authorizationName: ").append(toIndentedString(authorizationName)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
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

