package com.bytechef.automation.configuration.web.rest.model;

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
 * StartWebhookTriggerTest200ResponseModel
 */

@JsonTypeName("startWebhookTriggerTest_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-23T12:36:59.892484+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class StartWebhookTriggerTest200ResponseModel {

  private @Nullable String webhookUrl;

  public StartWebhookTriggerTest200ResponseModel webhookUrl(@Nullable String webhookUrl) {
    this.webhookUrl = webhookUrl;
    return this;
  }

  /**
   * The webhook URL.
   * @return webhookUrl
   */
  
  @Schema(name = "webhookUrl", description = "The webhook URL.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("webhookUrl")
  public @Nullable String getWebhookUrl() {
    return webhookUrl;
  }

  public void setWebhookUrl(@Nullable String webhookUrl) {
    this.webhookUrl = webhookUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StartWebhookTriggerTest200ResponseModel startWebhookTriggerTest200Response = (StartWebhookTriggerTest200ResponseModel) o;
    return Objects.equals(this.webhookUrl, startWebhookTriggerTest200Response.webhookUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(webhookUrl);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StartWebhookTriggerTest200ResponseModel {\n");
    sb.append("    webhookUrl: ").append(toIndentedString(webhookUrl)).append("\n");
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

