package com.bytechef.platform.workflow.execution.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.workflow.execution.web.rest.model.WebhookRetryModel;
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
 * Used to register to receive notifications for certain events.
 */

@Schema(name = "Webhook", description = "Used to register to receive notifications for certain events.")
@JsonTypeName("Webhook")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-07T14:49:50.244288+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public class WebhookModel {

  private @Nullable String type;

  private @Nullable String url;

  private @Nullable WebhookRetryModel retry;

  public WebhookModel type(@Nullable String type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  
  @Schema(name = "type", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("type")
  public @Nullable String getType() {
    return type;
  }

  public void setType(@Nullable String type) {
    this.type = type;
  }

  public WebhookModel url(@Nullable String url) {
    this.url = url;
    return this;
  }

  /**
   * Get url
   * @return url
   */
  
  @Schema(name = "url", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("url")
  public @Nullable String getUrl() {
    return url;
  }

  public void setUrl(@Nullable String url) {
    this.url = url;
  }

  public WebhookModel retry(@Nullable WebhookRetryModel retry) {
    this.retry = retry;
    return this;
  }

  /**
   * Get retry
   * @return retry
   */
  @Valid 
  @Schema(name = "retry", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("retry")
  public @Nullable WebhookRetryModel getRetry() {
    return retry;
  }

  public void setRetry(@Nullable WebhookRetryModel retry) {
    this.retry = retry;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WebhookModel webhook = (WebhookModel) o;
    return Objects.equals(this.type, webhook.type) &&
        Objects.equals(this.url, webhook.url) &&
        Objects.equals(this.retry, webhook.retry);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, url, retry);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WebhookModel {\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    retry: ").append(toIndentedString(retry)).append("\n");
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

