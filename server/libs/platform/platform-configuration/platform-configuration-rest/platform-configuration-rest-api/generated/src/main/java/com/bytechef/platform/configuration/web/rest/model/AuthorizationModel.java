package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.AuthorizationTypeModel;
import com.bytechef.platform.configuration.web.rest.model.PropertyModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * Contains information required for a connection&#39;s authorization.
 */

@Schema(name = "Authorization", description = "Contains information required for a connection's authorization.")
@JsonTypeName("Authorization")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-19T05:46:41.627972+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public class AuthorizationModel {

  private @Nullable String description;

  private @Nullable String name;

  @Valid
  private List<@Valid PropertyModel> properties = new ArrayList<>();

  private @Nullable String title;

  private @Nullable AuthorizationTypeModel type;

  public AuthorizationModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description.
   * @return description
   */
  
  @Schema(name = "description", description = "The description.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public AuthorizationModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The authorization name.
   * @return name
   */
  
  @Schema(name = "name", description = "The authorization name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }

  public AuthorizationModel properties(List<@Valid PropertyModel> properties) {
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
  @JsonProperty("properties")
  public List<@Valid PropertyModel> getProperties() {
    return properties;
  }

  public void setProperties(List<@Valid PropertyModel> properties) {
    this.properties = properties;
  }

  public AuthorizationModel title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * The title
   * @return title
   */
  
  @Schema(name = "title", description = "The title", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public AuthorizationModel type(@Nullable AuthorizationTypeModel type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @Valid 
  @Schema(name = "type", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("type")
  public @Nullable AuthorizationTypeModel getType() {
    return type;
  }

  public void setType(@Nullable AuthorizationTypeModel type) {
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
    return Objects.equals(this.description, authorization.description) &&
        Objects.equals(this.name, authorization.name) &&
        Objects.equals(this.properties, authorization.properties) &&
        Objects.equals(this.title, authorization.title) &&
        Objects.equals(this.type, authorization.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, name, properties, title, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AuthorizationModel {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

