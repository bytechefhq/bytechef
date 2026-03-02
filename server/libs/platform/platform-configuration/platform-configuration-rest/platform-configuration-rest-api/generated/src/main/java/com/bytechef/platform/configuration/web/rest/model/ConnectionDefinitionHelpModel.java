package com.bytechef.platform.configuration.web.rest.model;

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
 * ConnectionDefinitionHelpModel
 */

@JsonTypeName("ConnectionDefinition_help")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-02T12:16:06.542836107+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public class ConnectionDefinitionHelpModel {

  private @Nullable String body;

  private @Nullable String learnMoreUrl;

  public ConnectionDefinitionHelpModel body(@Nullable String body) {
    this.body = body;
    return this;
  }

  /**
   * Get body
   * @return body
   */
  
  @Schema(name = "body", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("body")
  public @Nullable String getBody() {
    return body;
  }

  public void setBody(@Nullable String body) {
    this.body = body;
  }

  public ConnectionDefinitionHelpModel learnMoreUrl(@Nullable String learnMoreUrl) {
    this.learnMoreUrl = learnMoreUrl;
    return this;
  }

  /**
   * Get learnMoreUrl
   * @return learnMoreUrl
   */
  
  @Schema(name = "learnMoreUrl", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("learnMoreUrl")
  public @Nullable String getLearnMoreUrl() {
    return learnMoreUrl;
  }

  public void setLearnMoreUrl(@Nullable String learnMoreUrl) {
    this.learnMoreUrl = learnMoreUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConnectionDefinitionHelpModel connectionDefinitionHelp = (ConnectionDefinitionHelpModel) o;
    return Objects.equals(this.body, connectionDefinitionHelp.body) &&
        Objects.equals(this.learnMoreUrl, connectionDefinitionHelp.learnMoreUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(body, learnMoreUrl);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectionDefinitionHelpModel {\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    learnMoreUrl: ").append(toIndentedString(learnMoreUrl)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

