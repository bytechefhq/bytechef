package com.bytechef.ee.automation.ai.gateway.public_.web.rest.model;

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
 * A routing strategy.
 */

@Schema(name = "RoutingStrategy", description = "A routing strategy.")
@JsonTypeName("RoutingStrategy")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-07T21:40:02.582901+02:00[Europe/Zagreb]", comments = "Generator version: 7.20.0")
public class RoutingStrategyModel {

  private @Nullable String id;

  private @Nullable String name;

  private @Nullable String description;

  public RoutingStrategyModel id(@Nullable String id) {
    this.id = id;
    return this;
  }

  /**
   * The strategy identifier.
   * @return id
   */
  
  @Schema(name = "id", description = "The strategy identifier.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable String getId() {
    return id;
  }

  public void setId(@Nullable String id) {
    this.id = id;
  }

  public RoutingStrategyModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The display name.
   * @return name
   */
  
  @Schema(name = "name", description = "The display name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }

  public RoutingStrategyModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * A description of the strategy behavior.
   * @return description
   */
  
  @Schema(name = "description", description = "A description of the strategy behavior.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoutingStrategyModel routingStrategy = (RoutingStrategyModel) o;
    return Objects.equals(this.id, routingStrategy.id) &&
        Objects.equals(this.name, routingStrategy.name) &&
        Objects.equals(this.description, routingStrategy.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoutingStrategyModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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

