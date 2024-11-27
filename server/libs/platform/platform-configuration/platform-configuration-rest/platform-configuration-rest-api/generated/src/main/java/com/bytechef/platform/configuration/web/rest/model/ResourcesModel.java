package com.bytechef.platform.configuration.web.rest.model;

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
 * A set of available resources.
 */

@Schema(name = "Resources", description = "A set of available resources.")
@JsonTypeName("Resources")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-11-26T21:19:59.239958+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public class ResourcesModel {

  private String documentationUrl;

  public ResourcesModel documentationUrl(String documentationUrl) {
    this.documentationUrl = documentationUrl;
    return this;
  }

  /**
   * The url of available documentation.
   * @return documentationUrl
   */
  
  @Schema(name = "documentationUrl", description = "The url of available documentation.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("documentationUrl")
  public String getDocumentationUrl() {
    return documentationUrl;
  }

  public void setDocumentationUrl(String documentationUrl) {
    this.documentationUrl = documentationUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResourcesModel resources = (ResourcesModel) o;
    return Objects.equals(this.documentationUrl, resources.documentationUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(documentationUrl);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ResourcesModel {\n");
    sb.append("    documentationUrl: ").append(toIndentedString(documentationUrl)).append("\n");
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

