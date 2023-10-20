package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.AuthorizationTypeModel;
import com.bytechef.hermes.definition.registry.web.rest.model.DisplayModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * Contains information required for a connection&#39;s authorization.
 */

@Schema(name = "Authorization", description = "Contains information required for a connection's authorization.")
@JsonTypeName("Authorization")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-05T16:27:34.189599+01:00[Europe/Zagreb]")
public class AuthorizationModel {

  @JsonProperty("display")
  private DisplayModel display;

  @JsonProperty("name")
  private String name;

  @JsonProperty("properties")
  @Valid
  private List<PropertyModel> properties = null;

  @JsonProperty("type")
  private AuthorizationTypeModel type;

  public AuthorizationModel display(DisplayModel display) {
    this.display = display;
    return this;
  }

  /**
   * Get display
   * @return display
  */
  @Valid 
  @Schema(name = "display", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public DisplayModel getDisplay() {
    return display;
  }

  public void setDisplay(DisplayModel display) {
    this.display = display;
  }

  public AuthorizationModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The authorization name.
   * @return name
  */
  
  @Schema(name = "name", description = "The authorization name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public AuthorizationModel properties(List<PropertyModel> properties) {
    this.properties = properties;
    return this;
  }

  public AuthorizationModel addPropertiesItem(PropertyModel propertiesItem) {
    if (this.properties == null) {
      this.properties = new ArrayList<>();
    }
    this.properties.add(propertiesItem);
    return this;
  }

  /**
   * Properties of the connection.
   * @return properties
  */
  @Valid 
  @Schema(name = "properties", description = "Properties of the connection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public List<PropertyModel> getProperties() {
    return properties;
  }

  public void setProperties(List<PropertyModel> properties) {
    this.properties = properties;
  }

  public AuthorizationModel type(AuthorizationTypeModel type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
  */
  @Valid 
  @Schema(name = "type", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public AuthorizationTypeModel getType() {
    return type;
  }

  public void setType(AuthorizationTypeModel type) {
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuthorizationModel authorization = (AuthorizationModel) o;
    return Objects.equals(this.display, authorization.display) &&
        Objects.equals(this.name, authorization.name) &&
        Objects.equals(this.properties, authorization.properties) &&
        Objects.equals(this.type, authorization.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(display, name, properties, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AuthorizationModel {\n");
    sb.append("    display: ").append(toIndentedString(display)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
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

