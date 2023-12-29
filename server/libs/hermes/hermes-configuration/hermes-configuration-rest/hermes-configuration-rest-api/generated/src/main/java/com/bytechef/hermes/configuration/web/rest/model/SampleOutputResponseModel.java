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

@Schema(name = "SampleOutputResponse", description = "A sample output response.")
@JsonTypeName("SampleOutputResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-01-03T07:47:14.476436+01:00[Europe/Zagreb]")
public class SampleOutputResponseModel {

  private String errorMessage;

  private Object sampleOutput;

  public SampleOutputResponseModel errorMessage(String errorMessage) {
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

  public SampleOutputResponseModel sampleOutput(Object sampleOutput) {
    this.sampleOutput = sampleOutput;
    return this;
  }

  /**
   * The sample output value.
   * @return sampleOutput
  */
  
  @Schema(name = "sampleOutput", description = "The sample output value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("sampleOutput")
  public Object getSampleOutput() {
    return sampleOutput;
  }

  public void setSampleOutput(Object sampleOutput) {
    this.sampleOutput = sampleOutput;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SampleOutputResponseModel sampleOutputResponse = (SampleOutputResponseModel) o;
    return Objects.equals(this.errorMessage, sampleOutputResponse.errorMessage) &&
        Objects.equals(this.sampleOutput, sampleOutputResponse.sampleOutput);
  }

  @Override
  public int hashCode() {
    return Objects.hash(errorMessage, sampleOutput);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SampleOutputResponseModel {\n");
    sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
    sb.append("    sampleOutput: ").append(toIndentedString(sampleOutput)).append("\n");
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

