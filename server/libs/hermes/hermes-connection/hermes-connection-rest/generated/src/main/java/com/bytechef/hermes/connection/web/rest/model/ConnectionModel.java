package com.bytechef.hermes.connection.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.connection.web.rest.model.TagModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-02-23T12:36:20.615811+01:00[Europe/Zagreb]")
public class ConnectionModel {

  @JsonProperty("authorizationName")
  private String authorizationName;

  @JsonProperty("componentName")
  private String componentName;

  @JsonProperty("connectionVersion")
  private Integer connectionVersion;

  @JsonProperty("createdBy")
  private String createdBy;

  @JsonProperty("createdDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  @JsonProperty("name")
  private String name;

  @JsonProperty("id")
  private Long id;

  @JsonProperty("lastModifiedBy")
  private String lastModifiedBy;

  @JsonProperty("lastModifiedDate")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  @JsonProperty("parameters")
  @Valid
  private Map<String, Object> parameters = new HashMap<>();

  @JsonProperty("tags")
  @Valid
  private List<TagModel> tags = null;

  @JsonProperty("version")
  private Integer version;

  public ConnectionModel authorizationName(String authorizationName) {
    this.authorizationName = authorizationName;
    return this;
  }

  /**
   * TODO
   * @return authorizationName
  */
  
  @Schema(name = "authorizationName", description = "TODO", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
  @NotNull 
  @Schema(name = "componentName", description = "TODO", requiredMode = Schema.RequiredMode.REQUIRED)
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public ConnectionModel connectionVersion(Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
    return this;
  }

  /**
   * TODO
   * @return connectionVersion
  */
  @NotNull 
  @Schema(name = "connectionVersion", description = "TODO", requiredMode = Schema.RequiredMode.REQUIRED)
  public Integer getConnectionVersion() {
    return connectionVersion;
  }

  public void setConnectionVersion(Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
  }

  public ConnectionModel createdBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * TODO
   * @return createdBy
  */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "TODO", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
  @Schema(name = "createdDate", accessMode = Schema.AccessMode.READ_ONLY, description = "TODO", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
  @NotNull 
  @Schema(name = "name", description = "TODO", requiredMode = Schema.RequiredMode.REQUIRED)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ConnectionModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * TODO
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "TODO", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
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
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, description = "TODO", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
  @Schema(name = "lastModifiedDate", accessMode = Schema.AccessMode.READ_ONLY, description = "TODO", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    this.parameters.put(key, parametersItem);
    return this;
  }

  /**
   * TODO
   * @return parameters
  */
  @NotNull 
  @Schema(name = "parameters", description = "TODO", requiredMode = Schema.RequiredMode.REQUIRED)
  public Map<String, Object> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  public ConnectionModel tags(List<TagModel> tags) {
    this.tags = tags;
    return this;
  }

  public ConnectionModel addTagsItem(TagModel tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Get tags
   * @return tags
  */
  @Valid 
  @Schema(name = "tags", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public List<TagModel> getTags() {
    return tags;
  }

  public void setTags(List<TagModel> tags) {
    this.tags = tags;
  }

  public ConnectionModel version(Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * @return version
  */
  
  @Schema(name = "version", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Integer getVersion() {
    return version;
  }

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
    ConnectionModel connection = (ConnectionModel) o;
    return Objects.equals(this.authorizationName, connection.authorizationName) &&
        Objects.equals(this.componentName, connection.componentName) &&
        Objects.equals(this.connectionVersion, connection.connectionVersion) &&
        Objects.equals(this.createdBy, connection.createdBy) &&
        Objects.equals(this.createdDate, connection.createdDate) &&
        Objects.equals(this.name, connection.name) &&
        Objects.equals(this.id, connection.id) &&
        Objects.equals(this.lastModifiedBy, connection.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, connection.lastModifiedDate) &&
        Objects.equals(this.parameters, connection.parameters) &&
        Objects.equals(this.tags, connection.tags) &&
        Objects.equals(this.version, connection.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorizationName, componentName, connectionVersion, createdBy, createdDate, name, id, lastModifiedBy, lastModifiedDate, parameters, tags, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectionModel {\n");
    sb.append("    authorizationName: ").append(toIndentedString(authorizationName)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    connectionVersion: ").append(toIndentedString(connectionVersion)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
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

