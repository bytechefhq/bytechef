package com.bytechef.platform.workflow.test.web.rest.model;

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
 * Single-use short-lived token authorizing a browser to open a workflow-test voice WebSocket.
 */

@Schema(name = "WorkflowTestVoiceSessionToken", description = "Single-use short-lived token authorizing a browser to open a workflow-test voice WebSocket.")
@JsonTypeName("WorkflowTestVoiceSessionToken")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-03T17:34:58.854224+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class WorkflowTestVoiceSessionTokenModel {

  private @Nullable String token;

  private @Nullable Long expiresInSeconds;

  public WorkflowTestVoiceSessionTokenModel token(@Nullable String token) {
    this.token = token;
    return this;
  }

  /**
   * The single-use token.
   * @return token
   */
  
  @Schema(name = "token", description = "The single-use token.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("token")
  public @Nullable String getToken() {
    return token;
  }

  @JsonProperty("token")
  public void setToken(@Nullable String token) {
    this.token = token;
  }

  public WorkflowTestVoiceSessionTokenModel expiresInSeconds(@Nullable Long expiresInSeconds) {
    this.expiresInSeconds = expiresInSeconds;
    return this;
  }

  /**
   * Token TTL in seconds (typically 60).
   * @return expiresInSeconds
   */
  
  @Schema(name = "expiresInSeconds", description = "Token TTL in seconds (typically 60).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("expiresInSeconds")
  public @Nullable Long getExpiresInSeconds() {
    return expiresInSeconds;
  }

  @JsonProperty("expiresInSeconds")
  public void setExpiresInSeconds(@Nullable Long expiresInSeconds) {
    this.expiresInSeconds = expiresInSeconds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkflowTestVoiceSessionTokenModel workflowTestVoiceSessionToken = (WorkflowTestVoiceSessionTokenModel) o;
    return Objects.equals(this.token, workflowTestVoiceSessionToken.token) &&
        Objects.equals(this.expiresInSeconds, workflowTestVoiceSessionToken.expiresInSeconds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(token, expiresInSeconds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowTestVoiceSessionTokenModel {\n");
    sb.append("    token: ").append(toIndentedString(token)).append("\n");
    sb.append("    expiresInSeconds: ").append(toIndentedString(expiresInSeconds)).append("\n");
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

