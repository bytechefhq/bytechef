package com.bytechef.ee.platform.apiconnector.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.platform.apiconnector.configuration.web.rest.model.ApiConnectorEndpointModel;
import com.bytechef.ee.platform.apiconnector.configuration.web.rest.model.TagModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

@Schema(name = "ApiConnector", description = "An API connector.")
@JsonTypeName("ApiConnector")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-23T12:37:00.237388+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class ApiConnectorModel {

  private String connectorVersion;

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private @Nullable String description;

  private @Nullable String definition;

  private @Nullable Boolean enabled;

  @Valid
  private List<@Valid ApiConnectorEndpointModel> endpoints = new ArrayList<>();

  private @Nullable String icon;

  private @Nullable Long id;

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  private String name;

  private @Nullable String specification;

  @Valid
  private List<@Valid TagModel> tags = new ArrayList<>();

  private @Nullable String title;

  private @Nullable Integer version;

  public ApiConnectorModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ApiConnectorModel(String connectorVersion, String name) {
    this.connectorVersion = connectorVersion;
    this.name = name;
  }

  public ApiConnectorModel connectorVersion(String connectorVersion) {
    this.connectorVersion = connectorVersion;
    return this;
  }

  /**
   * The version of an API connector.
   * @return connectorVersion
   */
  @NotNull 
  @Schema(name = "connectorVersion", description = "The version of an API connector.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connectorVersion")
  public String getConnectorVersion() {
    return connectorVersion;
  }

  public void setConnectorVersion(String connectorVersion) {
    this.connectorVersion = connectorVersion;
  }

  public ApiConnectorModel createdBy(@Nullable String createdBy) {
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

  public ApiConnectorModel createdDate(@Nullable OffsetDateTime createdDate) {
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

  public ApiConnectorModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of an API connector.
   * @return description
   */
  
  @Schema(name = "description", description = "The description of an API connector.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public ApiConnectorModel definition(@Nullable String definition) {
    this.definition = definition;
    return this;
  }

  /**
   * The definition of an API connector.
   * @return definition
   */
  
  @Schema(name = "definition", accessMode = Schema.AccessMode.READ_ONLY, description = "The definition of an API connector.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("definition")
  public @Nullable String getDefinition() {
    return definition;
  }

  public void setDefinition(@Nullable String definition) {
    this.definition = definition;
  }

  public ApiConnectorModel enabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If an API connector is enabled or not.
   * @return enabled
   */
  
  @Schema(name = "enabled", description = "If an API connector is enabled or not.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public @Nullable Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
  }

  public ApiConnectorModel endpoints(List<@Valid ApiConnectorEndpointModel> endpoints) {
    this.endpoints = endpoints;
    return this;
  }

  public ApiConnectorModel addEndpointsItem(ApiConnectorEndpointModel endpointsItem) {
    if (this.endpoints == null) {
      this.endpoints = new ArrayList<>();
    }
    this.endpoints.add(endpointsItem);
    return this;
  }

  /**
   * Get endpoints
   * @return endpoints
   */
  @Valid 
  @Schema(name = "endpoints", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("endpoints")
  public List<@Valid ApiConnectorEndpointModel> getEndpoints() {
    return endpoints;
  }

  public void setEndpoints(List<@Valid ApiConnectorEndpointModel> endpoints) {
    this.endpoints = endpoints;
  }

  public ApiConnectorModel icon(@Nullable String icon) {
    this.icon = icon;
    return this;
  }

  /**
   * The icon of an API connector.
   * @return icon
   */
  
  @Schema(name = "icon", description = "The icon of an API connector.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("icon")
  public @Nullable String getIcon() {
    return icon;
  }

  public void setIcon(@Nullable String icon) {
    this.icon = icon;
  }

  public ApiConnectorModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of the API connector.
   * @return id
   */
  
  @Schema(name = "id", description = "The id of the API connector.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public ApiConnectorModel lastModifiedBy(@Nullable String lastModifiedBy) {
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

  public ApiConnectorModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
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

  public ApiConnectorModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of an API connector.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The name of an API connector.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiConnectorModel specification(@Nullable String specification) {
    this.specification = specification;
    return this;
  }

  /**
   * The specification of an API connector.
   * @return specification
   */
  
  @Schema(name = "specification", accessMode = Schema.AccessMode.READ_ONLY, description = "The specification of an API connector.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("specification")
  public @Nullable String getSpecification() {
    return specification;
  }

  public void setSpecification(@Nullable String specification) {
    this.specification = specification;
  }

  public ApiConnectorModel tags(List<@Valid TagModel> tags) {
    this.tags = tags;
    return this;
  }

  public ApiConnectorModel addTagsItem(TagModel tagsItem) {
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
  @JsonProperty("tags")
  public List<@Valid TagModel> getTags() {
    return tags;
  }

  public void setTags(List<@Valid TagModel> tags) {
    this.tags = tags;
  }

  public ApiConnectorModel title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * The title of an API connector.
   * @return title
   */
  
  @Schema(name = "title", description = "The title of an API connector.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public ApiConnectorModel version(@Nullable Integer version) {
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
    ApiConnectorModel apiConnector = (ApiConnectorModel) o;
    return Objects.equals(this.connectorVersion, apiConnector.connectorVersion) &&
        Objects.equals(this.createdBy, apiConnector.createdBy) &&
        Objects.equals(this.createdDate, apiConnector.createdDate) &&
        Objects.equals(this.description, apiConnector.description) &&
        Objects.equals(this.definition, apiConnector.definition) &&
        Objects.equals(this.enabled, apiConnector.enabled) &&
        Objects.equals(this.endpoints, apiConnector.endpoints) &&
        Objects.equals(this.icon, apiConnector.icon) &&
        Objects.equals(this.id, apiConnector.id) &&
        Objects.equals(this.lastModifiedBy, apiConnector.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, apiConnector.lastModifiedDate) &&
        Objects.equals(this.name, apiConnector.name) &&
        Objects.equals(this.specification, apiConnector.specification) &&
        Objects.equals(this.tags, apiConnector.tags) &&
        Objects.equals(this.title, apiConnector.title) &&
        Objects.equals(this.version, apiConnector.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectorVersion, createdBy, createdDate, description, definition, enabled, endpoints, icon, id, lastModifiedBy, lastModifiedDate, name, specification, tags, title, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiConnectorModel {\n");
    sb.append("    connectorVersion: ").append(toIndentedString(connectorVersion)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    definition: ").append(toIndentedString(definition)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    endpoints: ").append(toIndentedString(endpoints)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    specification: ").append(toIndentedString(specification)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
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

