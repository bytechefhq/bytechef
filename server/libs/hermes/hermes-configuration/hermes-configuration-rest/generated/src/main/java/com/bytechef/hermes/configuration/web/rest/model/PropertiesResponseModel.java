package com.bytechef.hermes.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.configuration.web.rest.model.PropertyModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A dynamic properties response.
 */

@Schema(name = "PropertiesResponse", description = "A dynamic properties response.")
@JsonTypeName("PropertiesResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-12-28T18:24:23.377490+01:00[Europe/Zagreb]")
public class PropertiesResponseModel {

  private String errorMessage;

  @Valid
  private List<@Valid PropertyModel> properties;

  public PropertiesResponseModel errorMessage(String errorMessage) {
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

  public PropertiesResponseModel properties(List<@Valid PropertyModel> properties) {
    this.properties = properties;
    return this;
  }

  public PropertiesResponseModel addPropertiesItem(PropertyModel propertiesItem) {
    if (this.properties == null) {
      this.properties = new ArrayList<>();
    }
    this.properties.add(propertiesItem);
    return this;
  }

  /**
   * The list of properties.
   * @return properties
  */
  @Valid 
  @Schema(name = "properties", description = "The list of properties.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("properties")
  public List<@Valid PropertyModel> getProperties() {
    return properties;
  }

  public void setProperties(List<@Valid PropertyModel> properties) {
    this.properties = properties;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PropertiesResponseModel propertiesResponse = (PropertiesResponseModel) o;
    return Objects.equals(this.errorMessage, propertiesResponse.errorMessage) &&
        Objects.equals(this.properties, propertiesResponse.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(errorMessage, properties);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PropertiesResponseModel {\n");
    sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
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

