package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.DisplayModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-05T16:27:34.189599+01:00[Europe/Zagreb]")
public class ConnectionDefinitionBasicModel {

  @JsonProperty("componentName")
  private String componentName;

  @JsonProperty("display")
  private DisplayModel display;

  @JsonProperty("version")
  private Integer version;

  public ConnectionDefinitionBasicModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of a component this connection can be used for.
   * @return componentName
  */
  @NotNull 
  @Schema(name = "componentName", description = "The name of a component this connection can be used for.", requiredMode = Schema.RequiredMode.REQUIRED)
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public ConnectionDefinitionBasicModel display(DisplayModel display) {
    this.display = display;
    return this;
  }

  /**
   * Get display
   * @return display
  */
  @NotNull @Valid 
  @Schema(name = "display", requiredMode = Schema.RequiredMode.REQUIRED)
  public DisplayModel getDisplay() {
    return display;
  }

  public void setDisplay(DisplayModel display) {
    this.display = display;
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
    return Objects.equals(this.componentName, connectionDefinitionBasic.componentName) &&
        Objects.equals(this.display, connectionDefinitionBasic.display) &&
        Objects.equals(this.version, connectionDefinitionBasic.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, display, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectionDefinitionBasicModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    display: ").append(toIndentedString(display)).append("\n");
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

