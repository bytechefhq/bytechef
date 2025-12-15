package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.AuthorizationModel;
import com.bytechef.platform.configuration.web.rest.model.PropertyModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-15T09:52:48.574632+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class ConnectionDefinitionModel {

  private Boolean authorizationRequired = true;

  @Valid
  private List<@Valid AuthorizationModel> authorizations = new ArrayList<>();

  private @Nullable String baseUri;

  private @Nullable String componentDescription;

  private String componentName;

  @Valid
  private List<@Valid PropertyModel> properties = new ArrayList<>();

  private @Nullable String componentTitle;

  private Integer version;

  public ConnectionDefinitionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ConnectionDefinitionModel(String componentName, Integer version) {
    this.componentName = componentName;
    this.version = version;
  }

  public ConnectionDefinitionModel authorizationRequired(Boolean authorizationRequired) {
    this.authorizationRequired = authorizationRequired;
    return this;
  }

  /**
   * If a connection requires an authorization to be defined or not
   * @return authorizationRequired
   */
  
  @Schema(name = "authorizationRequired", description = "If a connection requires an authorization to be defined or not", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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

  public ConnectionDefinitionModel baseUri(@Nullable String baseUri) {
    this.baseUri = baseUri;
    return this;
  }

  /**
   * Defines the base URI for all future HTTP requests.
   * @return baseUri
   */
  
  @Schema(name = "baseUri", description = "Defines the base URI for all future HTTP requests.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("baseUri")
  public @Nullable String getBaseUri() {
    return baseUri;
  }

  public void setBaseUri(@Nullable String baseUri) {
    this.baseUri = baseUri;
  }

  public ConnectionDefinitionModel componentDescription(@Nullable String componentDescription) {
    this.componentDescription = componentDescription;
    return this;
  }

  /**
   * The description used from the connection's component.
   * @return componentDescription
   */
  
  @Schema(name = "componentDescription", description = "The description used from the connection's component.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentDescription")
  public @Nullable String getComponentDescription() {
    return componentDescription;
  }

  public void setComponentDescription(@Nullable String componentDescription) {
    this.componentDescription = componentDescription;
  }

  public ConnectionDefinitionModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The component name used from the connection's component.
   * @return componentName
   */
  @NotNull 
  @Schema(name = "componentName", description = "The component name used from the connection's component.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
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

  public ConnectionDefinitionModel componentTitle(@Nullable String componentTitle) {
    this.componentTitle = componentTitle;
    return this;
  }

  /**
   * The title used from the connection's component.
   * @return componentTitle
   */
  
  @Schema(name = "componentTitle", description = "The title used from the connection's component.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentTitle")
  public @Nullable String getComponentTitle() {
    return componentTitle;
  }

  public void setComponentTitle(@Nullable String componentTitle) {
    this.componentTitle = componentTitle;
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
        Objects.equals(this.componentDescription, connectionDefinition.componentDescription) &&
        Objects.equals(this.componentName, connectionDefinition.componentName) &&
        Objects.equals(this.properties, connectionDefinition.properties) &&
        Objects.equals(this.componentTitle, connectionDefinition.componentTitle) &&
        Objects.equals(this.version, connectionDefinition.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorizationRequired, authorizations, baseUri, componentDescription, componentName, properties, componentTitle, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectionDefinitionModel {\n");
    sb.append("    authorizationRequired: ").append(toIndentedString(authorizationRequired)).append("\n");
    sb.append("    authorizations: ").append(toIndentedString(authorizations)).append("\n");
    sb.append("    baseUri: ").append(toIndentedString(baseUri)).append("\n");
    sb.append("    componentDescription: ").append(toIndentedString(componentDescription)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
    sb.append("    componentTitle: ").append(toIndentedString(componentTitle)).append("\n");
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

