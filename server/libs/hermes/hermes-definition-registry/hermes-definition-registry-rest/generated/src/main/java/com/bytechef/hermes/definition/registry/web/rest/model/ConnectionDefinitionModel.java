package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.AuthorizationModel;
import com.bytechef.hermes.definition.registry.web.rest.model.DisplayModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ResourcesModel;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-29T08:21:11.145214+02:00[Europe/Zagreb]")
public class ConnectionDefinitionModel {

  @JsonProperty("authorizationRequired")
  private Boolean authorizationRequired;

  @JsonProperty("authorizations")
  @Valid
  private List<AuthorizationModel> authorizations = null;

  @JsonProperty("baseUri")
  private String baseUri;

  @JsonProperty("componentName")
  private String componentName;

  @JsonProperty("componentDisplay")
  private DisplayModel componentDisplay;

  @JsonProperty("properties")
  @Valid
  private List<PropertyModel> properties = null;

  @JsonProperty("resources")
  private ResourcesModel resources;

  public ConnectionDefinitionModel authorizationRequired(Boolean authorizationRequired) {
    this.authorizationRequired = authorizationRequired;
    return this;
  }

  /**
   * If connection requires an authorization to be configured or not.
   * @return authorizationRequired
  */
  
  @Schema(name = "authorizationRequired", description = "If connection requires an authorization to be configured or not.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Boolean getAuthorizationRequired() {
    return authorizationRequired;
  }

  public void setAuthorizationRequired(Boolean authorizationRequired) {
    this.authorizationRequired = authorizationRequired;
  }

  public ConnectionDefinitionModel authorizations(List<AuthorizationModel> authorizations) {
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
  public List<AuthorizationModel> getAuthorizations() {
    return authorizations;
  }

  public void setAuthorizations(List<AuthorizationModel> authorizations) {
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
  public String getBaseUri() {
    return baseUri;
  }

  public void setBaseUri(String baseUri) {
    this.baseUri = baseUri;
  }

  public ConnectionDefinitionModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of a component this connection can be used for.
   * @return componentName
  */
  
  @Schema(name = "componentName", description = "The name of a component this connection can be used for.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public ConnectionDefinitionModel componentDisplay(DisplayModel componentDisplay) {
    this.componentDisplay = componentDisplay;
    return this;
  }

  /**
   * Get componentDisplay
   * @return componentDisplay
  */
  @Valid 
  @Schema(name = "componentDisplay", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public DisplayModel getComponentDisplay() {
    return componentDisplay;
  }

  public void setComponentDisplay(DisplayModel componentDisplay) {
    this.componentDisplay = componentDisplay;
  }

  public ConnectionDefinitionModel properties(List<PropertyModel> properties) {
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
  public List<PropertyModel> getProperties() {
    return properties;
  }

  public void setProperties(List<PropertyModel> properties) {
    this.properties = properties;
  }

  public ConnectionDefinitionModel resources(ResourcesModel resources) {
    this.resources = resources;
    return this;
  }

  /**
   * Get resources
   * @return resources
  */
  @Valid 
  @Schema(name = "resources", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public ResourcesModel getResources() {
    return resources;
  }

  public void setResources(ResourcesModel resources) {
    this.resources = resources;
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
        Objects.equals(this.componentName, connectionDefinition.componentName) &&
        Objects.equals(this.componentDisplay, connectionDefinition.componentDisplay) &&
        Objects.equals(this.properties, connectionDefinition.properties) &&
        Objects.equals(this.resources, connectionDefinition.resources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorizationRequired, authorizations, baseUri, componentName, componentDisplay, properties, resources);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectionDefinitionModel {\n");
    sb.append("    authorizationRequired: ").append(toIndentedString(authorizationRequired)).append("\n");
    sb.append("    authorizations: ").append(toIndentedString(authorizations)).append("\n");
    sb.append("    baseUri: ").append(toIndentedString(baseUri)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentDisplay: ").append(toIndentedString(componentDisplay)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
    sb.append("    resources: ").append(toIndentedString(resources)).append("\n");
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

