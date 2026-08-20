package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ReauthorizeConnectionRequestModel
 */

@JsonTypeName("ReauthorizeConnectionRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-18T00:19:21.376010+02:00[Europe/Zagreb]", comments = "Generator version: 7.24.0")
public class ReauthorizeConnectionRequestModel {

  private Map<String, Object> parameters = new HashMap<>();

  public ReauthorizeConnectionRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ReauthorizeConnectionRequestModel(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  public ReauthorizeConnectionRequestModel parameters(Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  public ReauthorizeConnectionRequestModel putParametersItem(String key, Object parametersItem) {
    if (this.parameters == null) {
      this.parameters = new HashMap<>();
    }
    this.parameters.put(key, parametersItem);
    return this;
  }

  /**
   * Get parameters
   * @return parameters
   */
  @NotNull 
  @Schema(name = "parameters", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("parameters")
  public Map<String, Object> getParameters() {
    return parameters;
  }

  @JsonProperty("parameters")
  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReauthorizeConnectionRequestModel reauthorizeConnectionRequest = (ReauthorizeConnectionRequestModel) o;
    return Objects.equals(this.parameters, reauthorizeConnectionRequest.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReauthorizeConnectionRequestModel {\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
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

