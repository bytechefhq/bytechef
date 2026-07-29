package com.bytechef.ee.automation.configuration.public_.web.rest.model;

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
 * The project a workflow execution belongs to.
 */

@Schema(name = "ProjectReference", description = "The project a workflow execution belongs to.")
@JsonTypeName("ProjectReference")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-18T16:40:02.539074685Z[Etc/UTC]", comments = "Generator version: 7.22.0")
public class ProjectReferenceModel {

  private @Nullable Long id;

  private @Nullable String name;

  public ProjectReferenceModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a project.
   * @return id
   */
  
  @Schema(name = "id", description = "The id of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public ProjectReferenceModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of a project.
   * @return name
   */
  
  @Schema(name = "name", description = "The name of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    ProjectReferenceModel projectReference = (ProjectReferenceModel) o;
    return Objects.equals(this.id, projectReference.id) &&
        Objects.equals(this.name, projectReference.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectReferenceModel {\n");
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

