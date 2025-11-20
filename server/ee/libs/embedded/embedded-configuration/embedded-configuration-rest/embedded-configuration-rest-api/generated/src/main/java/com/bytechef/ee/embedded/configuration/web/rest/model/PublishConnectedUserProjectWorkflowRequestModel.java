package com.bytechef.ee.embedded.configuration.web.rest.model;

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
 * PublishConnectedUserProjectWorkflowRequestModel
 */

@JsonTypeName("publishConnectedUserProjectWorkflow_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:35.556332+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class PublishConnectedUserProjectWorkflowRequestModel {

  private @Nullable String description;

  public PublishConnectedUserProjectWorkflowRequestModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of a connected user project workflow version.
   * @return description
   */
  
  @Schema(name = "description", description = "The description of a connected user project workflow version.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    PublishConnectedUserProjectWorkflowRequestModel publishConnectedUserProjectWorkflowRequest = (PublishConnectedUserProjectWorkflowRequestModel) o;
    return Objects.equals(this.description, publishConnectedUserProjectWorkflowRequest.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PublishConnectedUserProjectWorkflowRequestModel {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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

