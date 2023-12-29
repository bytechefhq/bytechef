package com.bytechef.hermes.configuration.web.rest.model;

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
 * A sample output response.
 */

@Schema(name = "EditorDescriptionResponse", description = "A sample output response.")
@JsonTypeName("EditorDescriptionResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-01-03T07:47:14.476436+01:00[Europe/Zagreb]")
public class EditorDescriptionResponseModel {

  private String errorMessage;

  private Object description;

  public EditorDescriptionResponseModel errorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  /**
   * The error message.
   * @return errorMessage
  */
  
  @Schema(name = "errorMessage", description = "The error message.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("errorMessage")
  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public EditorDescriptionResponseModel description(Object description) {
    this.description = description;
    return this;
  }

  /**
   * The description.
   * @return description
  */
  
  @Schema(name = "description", description = "The description.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public Object getDescription() {
    return description;
  }

  public void setDescription(Object description) {
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
    EditorDescriptionResponseModel editorDescriptionResponse = (EditorDescriptionResponseModel) o;
    return Objects.equals(this.errorMessage, editorDescriptionResponse.errorMessage) &&
        Objects.equals(this.description, editorDescriptionResponse.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(errorMessage, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EditorDescriptionResponseModel {\n");
    sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
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

