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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-01T22:58:40.927821+02:00[Europe/Zagreb]")
public class ConnectionDefinitionBasicModel {

  private String componentName;

  private DisplayModel componentDisplay;

  /**
   * Default constructor
   * @deprecated Use {@link ConnectionDefinitionBasicModel#ConnectionDefinitionBasicModel(String)}
   */
  @Deprecated
  public ConnectionDefinitionBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ConnectionDefinitionBasicModel(String componentName) {
    this.componentName = componentName;
  }

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
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public ConnectionDefinitionBasicModel componentDisplay(DisplayModel componentDisplay) {
    this.componentDisplay = componentDisplay;
    return this;
  }

  /**
   * Get componentDisplay
   * @return componentDisplay
  */
  @Valid 
  @Schema(name = "componentDisplay", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentDisplay")
  public DisplayModel getComponentDisplay() {
    return componentDisplay;
  }

  public void setComponentDisplay(DisplayModel componentDisplay) {
    this.componentDisplay = componentDisplay;
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
        Objects.equals(this.componentDisplay, connectionDefinitionBasic.componentDisplay);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, componentDisplay);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectionDefinitionBasicModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentDisplay: ").append(toIndentedString(componentDisplay)).append("\n");
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

