package com.bytechef.automation.workflow.execution.web.rest.model;

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
 * WebhookRetryModel
 */

@JsonTypeName("Webhook_retry")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-11-26T21:19:58.251228+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public class WebhookRetryModel {

  private Integer initialInterval;

  private Integer maxInterval;

  private Integer maxAttempts;

  private Integer multiplier;

  public WebhookRetryModel initialInterval(Integer initialInterval) {
    this.initialInterval = initialInterval;
    return this;
  }

  /**
   * Get initialInterval
   * @return initialInterval
   */
  
  @Schema(name = "initialInterval", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("initialInterval")
  public Integer getInitialInterval() {
    return initialInterval;
  }

  public void setInitialInterval(Integer initialInterval) {
    this.initialInterval = initialInterval;
  }

  public WebhookRetryModel maxInterval(Integer maxInterval) {
    this.maxInterval = maxInterval;
    return this;
  }

  /**
   * Get maxInterval
   * @return maxInterval
   */
  
  @Schema(name = "maxInterval", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("maxInterval")
  public Integer getMaxInterval() {
    return maxInterval;
  }

  public void setMaxInterval(Integer maxInterval) {
    this.maxInterval = maxInterval;
  }

  public WebhookRetryModel maxAttempts(Integer maxAttempts) {
    this.maxAttempts = maxAttempts;
    return this;
  }

  /**
   * Get maxAttempts
   * @return maxAttempts
   */
  
  @Schema(name = "maxAttempts", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("maxAttempts")
  public Integer getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(Integer maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public WebhookRetryModel multiplier(Integer multiplier) {
    this.multiplier = multiplier;
    return this;
  }

  /**
   * Get multiplier
   * @return multiplier
   */
  
  @Schema(name = "multiplier", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("multiplier")
  public Integer getMultiplier() {
    return multiplier;
  }

  public void setMultiplier(Integer multiplier) {
    this.multiplier = multiplier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WebhookRetryModel webhookRetry = (WebhookRetryModel) o;
    return Objects.equals(this.initialInterval, webhookRetry.initialInterval) &&
        Objects.equals(this.maxInterval, webhookRetry.maxInterval) &&
        Objects.equals(this.maxAttempts, webhookRetry.maxAttempts) &&
        Objects.equals(this.multiplier, webhookRetry.multiplier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(initialInterval, maxInterval, maxAttempts, multiplier);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WebhookRetryModel {\n");
    sb.append("    initialInterval: ").append(toIndentedString(initialInterval)).append("\n");
    sb.append("    maxInterval: ").append(toIndentedString(maxInterval)).append("\n");
    sb.append("    maxAttempts: ").append(toIndentedString(maxAttempts)).append("\n");
    sb.append("    multiplier: ").append(toIndentedString(multiplier)).append("\n");
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

