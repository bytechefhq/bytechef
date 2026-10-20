package com.bytechef.ee.automation.apiplatform.configuration.web.rest.model;

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
 * A required input of the new project version that carries no value.
 */

@Schema(name = "ApiCollectionMissingInput", description = "A required input of the new project version that carries no value.")
@JsonTypeName("ApiCollectionMissingInput")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T01:38:17.244806+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ApiCollectionMissingInputModel {

  private @Nullable String endpointName;

  private @Nullable String inputName;

  public ApiCollectionMissingInputModel endpointName(@Nullable String endpointName) {
    this.endpointName = endpointName;
    return this;
  }

  /**
   * The name of the endpoint whose workflow needs the input.
   * @return endpointName
   */
  
  @Schema(name = "endpointName", description = "The name of the endpoint whose workflow needs the input.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("endpointName")
  public @Nullable String getEndpointName() {
    return endpointName;
  }

  @JsonProperty("endpointName")
  public void setEndpointName(@Nullable String endpointName) {
    this.endpointName = endpointName;
  }

  public ApiCollectionMissingInputModel inputName(@Nullable String inputName) {
    this.inputName = inputName;
    return this;
  }

  /**
   * The name of the input.
   * @return inputName
   */
  
  @Schema(name = "inputName", description = "The name of the input.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputName")
  public @Nullable String getInputName() {
    return inputName;
  }

  @JsonProperty("inputName")
  public void setInputName(@Nullable String inputName) {
    this.inputName = inputName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiCollectionMissingInputModel apiCollectionMissingInput = (ApiCollectionMissingInputModel) o;
    return Objects.equals(this.endpointName, apiCollectionMissingInput.endpointName) &&
        Objects.equals(this.inputName, apiCollectionMissingInput.inputName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(endpointName, inputName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCollectionMissingInputModel {\n");
    sb.append("    endpointName: ").append(toIndentedString(endpointName)).append("\n");
    sb.append("    inputName: ").append(toIndentedString(inputName)).append("\n");
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

