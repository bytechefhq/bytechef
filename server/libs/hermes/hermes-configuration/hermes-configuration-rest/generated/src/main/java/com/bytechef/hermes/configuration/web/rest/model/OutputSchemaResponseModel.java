package com.bytechef.hermes.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.configuration.web.rest.model.PropertyModel;
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
 * An output schema response.
 */

@Schema(name = "OutputSchemaResponse", description = "An output schema response.")
@JsonTypeName("OutputSchemaResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-12-28T18:24:23.377490+01:00[Europe/Zagreb]")
public class OutputSchemaResponseModel {

  private String errorMessage;

  private PropertyModel property;

  public OutputSchemaResponseModel errorMessage(String errorMessage) {
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

  public OutputSchemaResponseModel property(PropertyModel property) {
    this.property = property;
    return this;
  }

  /**
   * Get property
   * @return property
  */
  @Valid 
  @Schema(name = "property", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("property")
  public PropertyModel getProperty() {
    return property;
  }

  public void setProperty(PropertyModel property) {
    this.property = property;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OutputSchemaResponseModel outputSchemaResponse = (OutputSchemaResponseModel) o;
    return Objects.equals(this.errorMessage, outputSchemaResponse.errorMessage) &&
        Objects.equals(this.property, outputSchemaResponse.property);
  }

  @Override
  public int hashCode() {
    return Objects.hash(errorMessage, property);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OutputSchemaResponseModel {\n");
    sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
    sb.append("    property: ").append(toIndentedString(property)).append("\n");
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

