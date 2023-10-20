package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-10T17:56:08.674559+02:00[Europe/Zagreb]")
public class ConnectionDefinitionBasicModel {

  private String description;

  private String name;

  private String title;

  private Integer version;

  /**
   * Default constructor
   * @deprecated Use {@link ConnectionDefinitionBasicModel#ConnectionDefinitionBasicModel(String, Integer)}
   */
  @Deprecated
  public ConnectionDefinitionBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ConnectionDefinitionBasicModel(String name, Integer version) {
    this.name = name;
    this.version = version;
  }

  public ConnectionDefinitionBasicModel description(String description) {
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

  public ConnectionDefinitionBasicModel name(String name) {
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

  public ConnectionDefinitionBasicModel title(String title) {
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
    return Objects.equals(this.description, connectionDefinitionBasic.description) &&
        Objects.equals(this.name, connectionDefinitionBasic.name) &&
        Objects.equals(this.title, connectionDefinitionBasic.title) &&
        Objects.equals(this.version, connectionDefinitionBasic.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, name, title, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectionDefinitionBasicModel {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

