package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.PropertyModel;
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
 * The output response
 */

@Schema(name = "OutputResponse", description = "The output response")
@JsonTypeName("OutputResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-02T08:44:56.413708+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class OutputResponseModel {

  private @Nullable PropertyModel outputSchema;

  private @Nullable Object placeholder;

  private @Nullable Object sampleOutput;

  public OutputResponseModel outputSchema(@Nullable PropertyModel outputSchema) {
    this.outputSchema = outputSchema;
    return this;
  }

  /**
   * Get outputSchema
   * @return outputSchema
   */
  @Valid 
  @Schema(name = "outputSchema", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("outputSchema")
  public @Nullable PropertyModel getOutputSchema() {
    return outputSchema;
  }

  public void setOutputSchema(@Nullable PropertyModel outputSchema) {
    this.outputSchema = outputSchema;
  }

  public OutputResponseModel placeholder(@Nullable Object placeholder) {
    this.placeholder = placeholder;
    return this;
  }

  /**
   * The placeholder of an output.
   * @return placeholder
   */
  
  @Schema(name = "placeholder", description = "The placeholder of an output.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("placeholder")
  public @Nullable Object getPlaceholder() {
    return placeholder;
  }

  public void setPlaceholder(@Nullable Object placeholder) {
    this.placeholder = placeholder;
  }

  public OutputResponseModel sampleOutput(@Nullable Object sampleOutput) {
    this.sampleOutput = sampleOutput;
    return this;
  }

  /**
   * The sample value of an output.
   * @return sampleOutput
   */
  
  @Schema(name = "sampleOutput", description = "The sample value of an output.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("sampleOutput")
  public @Nullable Object getSampleOutput() {
    return sampleOutput;
  }

  public void setSampleOutput(@Nullable Object sampleOutput) {
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
    OutputResponseModel outputResponse = (OutputResponseModel) o;
    return Objects.equals(this.outputSchema, outputResponse.outputSchema) &&
        Objects.equals(this.placeholder, outputResponse.placeholder) &&
        Objects.equals(this.sampleOutput, outputResponse.sampleOutput);
  }

  @Override
  public int hashCode() {
    return Objects.hash(outputSchema, placeholder, sampleOutput);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OutputResponseModel {\n");
    sb.append("    outputSchema: ").append(toIndentedString(outputSchema)).append("\n");
    sb.append("    placeholder: ").append(toIndentedString(placeholder)).append("\n");
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

