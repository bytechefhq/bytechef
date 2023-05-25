package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.AuthorizationModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Definition of a connection to an outside service.
 */

@Schema(name = "ConnectionDefinition", description = "Definition of a connection to an outside service.")
@JsonTypeName("ConnectionDefinition")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-25T15:46:35.328005+02:00[Europe/Zagreb]")
public class ConnectionDefinitionModel {

  private Boolean authorizationRequired;

  @Valid
  private List<@Valid AuthorizationModel> authorizations;

  private String baseUri;

  private String description;

  private String name;

  @Valid
  private List<@Valid PropertyModel> properties;

  private String title;

  private Integer version;

  /**
   * Default constructor
   * @deprecated Use {@link ConnectionDefinitionModel#ConnectionDefinitionModel(String, Integer)}
   */
  @Deprecated
  public ConnectionDefinitionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ConnectionDefinitionModel(String name, Integer version) {
    this.name = name;
    this.version = version;
  }

  public ConnectionDefinitionModel authorizationRequired(Boolean authorizationRequired) {
    this.authorizationRequired = authorizationRequired;
    return this;
  }

  /**
   * If connection requires an authorization to be configured or not.
   * @return authorizationRequired
  */
  
  @Schema(name = "authorizationRequired", description = "If connection requires an authorization to be configured or not.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("authorizationRequired")
  public Boolean getAuthorizationRequired() {
    return authorizationRequired;
  }

  public void setAuthorizationRequired(Boolean authorizationRequired) {
    this.authorizationRequired = authorizationRequired;
  }

  public ConnectionDefinitionModel authorizations(List<@Valid AuthorizationModel> authorizations) {
    this.authorizations = authorizations;
    return this;
  }

  public ConnectionDefinitionModel addAuthorizationsItem(AuthorizationModel authorizationsItem) {
    if (this.authorizations == null) {
      this.authorizations = new ArrayList<>();
    }
    this.authorizations.add(authorizationsItem);
    return this;
  }

  /**
   * Get authorizations
   * @return authorizations
  */
  @Valid 
  @Schema(name = "authorizations", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("authorizations")
  public List<@Valid AuthorizationModel> getAuthorizations() {
    return authorizations;
  }

  public void setAuthorizations(List<@Valid AuthorizationModel> authorizations) {
    this.authorizations = authorizations;
  }

  public ConnectionDefinitionModel baseUri(String baseUri) {
    this.baseUri = baseUri;
    return this;
  }

  /**
   * Defines the base URI for all future HTTP requests.
   * @return baseUri
  */
  
  @Schema(name = "baseUri", description = "Defines the base URI for all future HTTP requests.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("baseUri")
  public String getBaseUri() {
    return baseUri;
  }

  public void setBaseUri(String baseUri) {
    this.baseUri = baseUri;
  }

  public ConnectionDefinitionModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The description.
   * @return description
  */
  
  @Schema(name = "description", description = "The description.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ConnectionDefinitionModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The connection name.
   * @return name
  */
  @NotNull 
  @Schema(name = "name", description = "The connection name.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ConnectionDefinitionModel properties(List<@Valid PropertyModel> properties) {
    this.properties = properties;
    return this;
  }

  public ConnectionDefinitionModel addPropertiesItem(PropertyModel propertiesItem) {
    if (this.properties == null) {
      this.properties = new ArrayList<>();
    }
    this.properties.add(propertiesItem);
    return this;
  }

  /**
   * The properties of the connection.
   * @return properties
  */
  @Valid 
  @Schema(name = "properties", description = "The properties of the connection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("properties")
  public List<@Valid PropertyModel> getProperties() {
    return properties;
  }

  public void setProperties(List<@Valid PropertyModel> properties) {
    this.properties = properties;
  }

  public ConnectionDefinitionModel title(String title) {
    this.title = title;
    return this;
  }

  /**
   * The title
   * @return title
  */
  
  @Schema(name = "title", description = "The title", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public ConnectionDefinitionModel version(Integer version) {
    this.version = version;
    return this;
  }

  /**
   * The version of a connection.
   * @return version
  */
  @NotNull 
  @Schema(name = "version", description = "The version of a connection.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("version")
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
    ConnectionDefinitionModel connectionDefinition = (ConnectionDefinitionModel) o;
    return Objects.equals(this.authorizationRequired, connectionDefinition.authorizationRequired) &&
        Objects.equals(this.authorizations, connectionDefinition.authorizations) &&
        Objects.equals(this.baseUri, connectionDefinition.baseUri) &&
        Objects.equals(this.description, connectionDefinition.description) &&
        Objects.equals(this.name, connectionDefinition.name) &&
        Objects.equals(this.properties, connectionDefinition.properties) &&
        Objects.equals(this.title, connectionDefinition.title) &&
        Objects.equals(this.version, connectionDefinition.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorizationRequired, authorizations, baseUri, description, name, properties, title, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectionDefinitionModel {\n");
    sb.append("    authorizationRequired: ").append(toIndentedString(authorizationRequired)).append("\n");
    sb.append("    authorizations: ").append(toIndentedString(authorizations)).append("\n");
    sb.append("    baseUri: ").append(toIndentedString(baseUri)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
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

