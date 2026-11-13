package com.bytechef.ee.embedded.configuration.web.rest.model;

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
 * Contains the replacement credentials of a connection.
 */

@Schema(name = "UpdateConnectionCredentialsRequest", description = "Contains the replacement credentials of a connection.")
@JsonTypeName("UpdateConnectionCredentialsRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-13T19:43:07.740396+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class UpdateConnectionCredentialsRequestModel {

  private Map<String, Object> parameters = new HashMap<>();

  private Integer version;

  public UpdateConnectionCredentialsRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UpdateConnectionCredentialsRequestModel(Map<String, Object> parameters, Integer version) {
    this.parameters = parameters;
    this.version = version;
  }

  public UpdateConnectionCredentialsRequestModel parameters(Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  public UpdateConnectionCredentialsRequestModel putParametersItem(String key, Object parametersItem) {
    if (this.parameters == null) {
      this.parameters = new HashMap<>();
    }
    this.parameters.put(key, parametersItem);
    return this;
  }

  /**
   * The replacement authorization parameters.
   * @return parameters
   */
  @NotNull 
  @Schema(name = "parameters", description = "The replacement authorization parameters.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("parameters")
  public Map<String, Object> getParameters() {
    return parameters;
  }

  @JsonProperty("parameters")
  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  public UpdateConnectionCredentialsRequestModel version(Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * @return version
   */
  @NotNull 
  @Schema(name = "version", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("version")
  public Integer getVersion() {
    return version;
  }

  @JsonProperty("version")
  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateConnectionCredentialsRequestModel updateConnectionCredentialsRequest = (UpdateConnectionCredentialsRequestModel) o;
    return Objects.equals(this.parameters, updateConnectionCredentialsRequest.parameters) &&
        Objects.equals(this.version, updateConnectionCredentialsRequest.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parameters, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateConnectionCredentialsRequestModel {\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
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

