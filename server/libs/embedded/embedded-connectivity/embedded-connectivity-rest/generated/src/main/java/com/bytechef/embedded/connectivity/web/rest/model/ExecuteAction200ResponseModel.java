package com.bytechef.embedded.connectivity.web.rest.model;

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
 * ExecuteAction200ResponseModel
 */

@JsonTypeName("executeAction_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-12T12:40:27.480948+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public class ExecuteAction200ResponseModel {

  private Object output;

  public ExecuteAction200ResponseModel output(Object output) {
    this.output = output;
    return this;
  }

  /**
   * The result of the action call.
   * @return output
  */
  
  @Schema(name = "output", description = "The result of the action call.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("output")
  public Object getOutput() {
    return output;
  }

  public void setOutput(Object output) {
    this.output = output;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExecuteAction200ResponseModel executeAction200Response = (ExecuteAction200ResponseModel) o;
    return Objects.equals(this.output, executeAction200Response.output);
  }

  @Override
  public int hashCode() {
    return Objects.hash(output);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExecuteAction200ResponseModel {\n");
    sb.append("    output: ").append(toIndentedString(output)).append("\n");
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

