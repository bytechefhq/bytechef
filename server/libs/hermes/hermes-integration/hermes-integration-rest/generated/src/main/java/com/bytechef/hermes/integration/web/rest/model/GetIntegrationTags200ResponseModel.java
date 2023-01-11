package com.bytechef.hermes.integration.web.rest.model;

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
 * The response object that contains the array of tags.
 */

@Schema(name = "getIntegrationTags_200_response", description = "The response object that contains the array of tags.")
@JsonTypeName("getIntegrationTags_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-01-11T09:43:32.875615+01:00[Europe/Zagreb]")
public class GetIntegrationTags200ResponseModel {

  @JsonProperty("tags")
  @Valid
  private List<String> tags = null;

  public GetIntegrationTags200ResponseModel tags(List<String> tags) {
    this.tags = tags;
    return this;
  }

  public GetIntegrationTags200ResponseModel addTagsItem(String tagsItem) {
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
  
  @Schema(name = "tags", required = false)
  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
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
    GetIntegrationTags200ResponseModel getIntegrationTags200Response = (GetIntegrationTags200ResponseModel) o;
    return Objects.equals(this.tags, getIntegrationTags200Response.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tags);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetIntegrationTags200ResponseModel {\n");
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

