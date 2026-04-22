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
 * The result of creating a score.
 */

@Schema(name = "ScoreResponse", description = "The result of creating a score.")
@JsonTypeName("ScoreResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-27T13:29:16.334179+02:00[Europe/Zagreb]", comments = "Generator version: 7.21.0")
public class ScoreResponseModel {

  private @Nullable Long id;

  private @Nullable String name;

  public ScoreResponseModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The generated score ID.
   * @return id
   */
  
  @Schema(name = "id", description = "The generated score ID.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public ScoreResponseModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The score name.
   * @return name
   */
  
  @Schema(name = "name", description = "The score name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
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
    ScoreResponseModel scoreResponse = (ScoreResponseModel) o;
    return Objects.equals(this.id, scoreResponse.id) &&
        Objects.equals(this.name, scoreResponse.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ScoreResponseModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

