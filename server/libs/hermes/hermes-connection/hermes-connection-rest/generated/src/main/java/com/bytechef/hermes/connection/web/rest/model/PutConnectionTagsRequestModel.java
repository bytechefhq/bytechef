package com.bytechef.hermes.connection.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
 * The request object that contains the array of tags.
 */

@Schema(name = "putConnectionTags_request", description = "The request object that contains the array of tags.")
@JsonTypeName("putConnectionTags_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-01T11:45:15.933556+01:00[Europe/Zagreb]")
public class PutConnectionTagsRequestModel {

  @JsonProperty("tags")
  @Valid
  private List<com.bytechef.tag.web.rest.model.TagModel> tags = null;

  public PutConnectionTagsRequestModel tags(List<com.bytechef.tag.web.rest.model.TagModel> tags) {
    this.tags = tags;
    return this;
  }

  public PutConnectionTagsRequestModel addTagsItem(com.bytechef.tag.web.rest.model.TagModel tagsItem) {
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
  @Valid 
  @Schema(name = "tags", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public List<com.bytechef.tag.web.rest.model.TagModel> getTags() {
    return tags;
  }

  public void setTags(List<com.bytechef.tag.web.rest.model.TagModel> tags) {
    this.tags = tags;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PutConnectionTagsRequestModel putConnectionTagsRequest = (PutConnectionTagsRequestModel) o;
    return Objects.equals(this.tags, putConnectionTagsRequest.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tags);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PutConnectionTagsRequestModel {\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
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

