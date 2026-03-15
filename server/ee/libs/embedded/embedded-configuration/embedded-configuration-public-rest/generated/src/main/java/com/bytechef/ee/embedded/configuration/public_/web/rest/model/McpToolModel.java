package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

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
 * An MCP tool definition.
 */

@Schema(name = "McpTool", description = "An MCP tool definition.")
@JsonTypeName("McpTool")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-13T17:17:09.715729+01:00[Europe/Zagreb]", comments = "Generator version: 7.20.0")
public class McpToolModel {

  private @Nullable String description;

  private @Nullable Long id;

  private @Nullable String name;

  public McpToolModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of the MCP tool.
   * @return description
   */
  
  @Schema(name = "description", description = "The description of the MCP tool.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public McpToolModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an MCP tool.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an MCP tool.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public McpToolModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The display name of the MCP tool, formatted as 'MCP Component name - McpTool name'.
   * @return name
   */
  
  @Schema(name = "name", description = "The display name of the MCP tool, formatted as 'MCP Component name - McpTool name'.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

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
    McpToolModel mcpTool = (McpToolModel) o;
    return Objects.equals(this.description, mcpTool.description) &&
        Objects.equals(this.id, mcpTool.id) &&
        Objects.equals(this.name, mcpTool.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, id, name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class McpToolModel {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

