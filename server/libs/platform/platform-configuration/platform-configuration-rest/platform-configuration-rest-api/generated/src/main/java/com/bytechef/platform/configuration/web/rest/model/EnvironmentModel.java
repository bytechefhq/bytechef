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
 * The environment.
 */

@Schema(name = "Environment", description = "The environment.")
@JsonTypeName("Environment")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-15T09:52:48.574632+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class EnvironmentModel {

  private @Nullable Long id;

  private String name;

  public EnvironmentModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EnvironmentModel(String name) {
    this.name = name;
  }

  public EnvironmentModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The environment id.
   * @return id
   */
  
  @Schema(name = "id", description = "The environment id.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public EnvironmentModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The environment name.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The environment name.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnvironmentModel environment = (EnvironmentModel) o;
    return Objects.equals(this.id, environment.id) &&
        Objects.equals(this.name, environment.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EnvironmentModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

