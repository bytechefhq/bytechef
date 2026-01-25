package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Contains all required information to open a connection to a service defined by componentName parameter
 */

@Schema(name = "Connection", description = "Contains all required information to open a connection to a service defined by componentName parameter")
@JsonTypeName("Connection")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-23T12:36:59.411987+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class ConnectionModel {

  private Long id;

  private String name;

  private @Nullable EnvironmentModel environment;

  public ConnectionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ConnectionModel(Long id, String name) {
    this.id = id;
    this.name = name;
  }

  public ConnectionModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an integration.
   * @return id
   */
  @NotNull 
  @Schema(name = "id", description = "The id of an integration.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ConnectionModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of a connection.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The name of a connection.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ConnectionModel environment(@Nullable EnvironmentModel environment) {
    this.environment = environment;
    return this;
  }

  /**
   * Get environment
   * @return environment
   */
  @Valid 
  @Schema(name = "environment", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("environment")
  public @Nullable EnvironmentModel getEnvironment() {
    return environment;
  }

  public void setEnvironment(@Nullable EnvironmentModel environment) {
    this.environment = environment;
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
    return Objects.equals(this.id, connection.id) &&
        Objects.equals(this.name, connection.name) &&
        Objects.equals(this.environment, connection.environment);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, environment);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectionModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    environment: ").append(toIndentedString(environment)).append("\n");
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

