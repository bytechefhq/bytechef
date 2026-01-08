package com.bytechef.automation.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.automation.configuration.web.rest.model.TagModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
 * Contains all connection parameters that can be updated.
 */

@Schema(name = "UpdateConnectionRequest", description = "Contains all connection parameters that can be updated.")
@JsonTypeName("UpdateConnectionRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-05T14:11:26.033736+01:00[Europe/Ljubljana]", comments = "Generator version: 7.18.0")
public class UpdateConnectionRequestModel {

  private String name;

  @Valid
  private List<@Valid TagModel> tags = new ArrayList<>();

  private @Nullable Integer version;

  public UpdateConnectionRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UpdateConnectionRequestModel(String name, List<@Valid TagModel> tags) {
    this.name = name;
    this.tags = tags;
  }

  public UpdateConnectionRequestModel name(String name) {
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

  public UpdateConnectionRequestModel tags(List<@Valid TagModel> tags) {
    this.tags = tags;
    return this;
  }

  public UpdateConnectionRequestModel addTagsItem(TagModel tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Get tags
   * @return tags
   */
  @NotNull @Valid 
  @Schema(name = "tags", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("tags")
  public List<@Valid TagModel> getTags() {
    return tags;
  }

  public void setTags(List<@Valid TagModel> tags) {
    this.tags = tags;
  }

  public UpdateConnectionRequestModel version(@Nullable Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * @return version
   */
  
  @Schema(name = "__version", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("__version")
  public @Nullable Integer getVersion() {
    return version;
  }

  public void setVersion(@Nullable Integer version) {
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
    UpdateConnectionRequestModel updateConnectionRequest = (UpdateConnectionRequestModel) o;
    return Objects.equals(this.name, updateConnectionRequest.name) &&
        Objects.equals(this.tags, updateConnectionRequest.tags) &&
        Objects.equals(this.version, updateConnectionRequest.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, tags, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateConnectionRequestModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
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

