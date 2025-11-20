package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
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

@Schema(name = "ConnectionDefinitionBasic", description = "Definition of a connection to an outside service.")
@JsonTypeName("ConnectionDefinitionBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:33.876560+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class ConnectionDefinitionBasicModel {

  private @Nullable String componentDescription;

  private String componentName;

  private @Nullable String componentTitle;

  private Integer version;

  public ConnectionDefinitionBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ConnectionDefinitionBasicModel(String componentName, Integer version) {
    this.componentName = componentName;
    this.version = version;
  }

  public ConnectionDefinitionBasicModel componentDescription(@Nullable String componentDescription) {
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

  public ConnectionDefinitionBasicModel componentName(String componentName) {
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

  public ConnectionDefinitionBasicModel componentTitle(@Nullable String componentTitle) {
    this.componentTitle = componentTitle;
    return this;
  }

  /**
   * The title used from the connection's component
   * @return componentTitle
   */
  
  @Schema(name = "componentTitle", description = "The title used from the connection's component", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentTitle")
  public @Nullable String getComponentTitle() {
    return componentTitle;
  }

  public void setComponentTitle(@Nullable String componentTitle) {
    this.componentTitle = componentTitle;
  }

  public ConnectionDefinitionBasicModel version(Integer version) {
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
    ConnectionDefinitionBasicModel connectionDefinitionBasic = (ConnectionDefinitionBasicModel) o;
    return Objects.equals(this.componentDescription, connectionDefinitionBasic.componentDescription) &&
        Objects.equals(this.componentName, connectionDefinitionBasic.componentName) &&
        Objects.equals(this.componentTitle, connectionDefinitionBasic.componentTitle) &&
        Objects.equals(this.version, connectionDefinitionBasic.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentDescription, componentName, componentTitle, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectionDefinitionBasicModel {\n");
    sb.append("    componentDescription: ").append(toIndentedString(componentDescription)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
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

