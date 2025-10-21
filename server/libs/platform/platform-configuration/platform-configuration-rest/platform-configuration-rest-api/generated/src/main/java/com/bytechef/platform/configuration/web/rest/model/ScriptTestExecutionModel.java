package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.ExecutionErrorModel;
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
 * Contains information about test execution of a script.
 */

@Schema(name = "ScriptTestExecution", description = "Contains information about test execution of a script.")
@JsonTypeName("ScriptTestExecution")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-21T12:06:41.161145+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public class ScriptTestExecutionModel {

  private @Nullable ExecutionErrorModel error;

  private @Nullable Object output;

  public ScriptTestExecutionModel error(@Nullable ExecutionErrorModel error) {
    this.error = error;
    return this;
  }

  /**
   * Get error
   * @return error
   */
  @Valid 
  @Schema(name = "error", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("error")
  public @Nullable ExecutionErrorModel getError() {
    return error;
  }

  public void setError(@Nullable ExecutionErrorModel error) {
    this.error = error;
  }

  public ScriptTestExecutionModel output(@Nullable Object output) {
    this.output = output;
    return this;
  }

  /**
   * The result output of testing a script.
   * @return output
   */
  
  @Schema(name = "output", description = "The result output of testing a script.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("output")
  public @Nullable Object getOutput() {
    return output;
  }

  public void setOutput(@Nullable Object output) {
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
    ScriptTestExecutionModel scriptTestExecution = (ScriptTestExecutionModel) o;
    return Objects.equals(this.error, scriptTestExecution.error) &&
        Objects.equals(this.output, scriptTestExecution.output);
  }

  @Override
  public int hashCode() {
    return Objects.hash(error, output);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ScriptTestExecutionModel {\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
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

