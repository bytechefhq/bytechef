package com.bytechef.ee.embedded.execution.public_.web.rest.model;

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
 * The parameters of a trigger subscription.
 */

@Schema(name = "TriggerSubscriptionRequest", description = "The parameters of a trigger subscription.")
@JsonTypeName("TriggerSubscriptionRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-12T01:42:14.080303+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class TriggerSubscriptionRequestModel {

  private @Nullable String externalId;

  private Map<String, Object> input = new HashMap<>();

  private @Nullable String name;

  private @Nullable String webhookUrl;

  public TriggerSubscriptionRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TriggerSubscriptionRequestModel(Map<String, Object> input) {
    this.input = input;
  }

  public TriggerSubscriptionRequestModel externalId(@Nullable String externalId) {
    this.externalId = externalId;
    return this;
  }

  /**
   * Your own id for the subscription. It is echoed in every event.
   * @return externalId
   */
  
  @Schema(name = "externalId", description = "Your own id for the subscription. It is echoed in every event.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("externalId")
  public @Nullable String getExternalId() {
    return externalId;
  }

  @JsonProperty("externalId")
  public void setExternalId(@Nullable String externalId) {
    this.externalId = externalId;
  }

  public TriggerSubscriptionRequestModel input(Map<String, Object> input) {
    this.input = input;
    return this;
  }

  public TriggerSubscriptionRequestModel putInputItem(String key, Object inputItem) {
    if (this.input == null) {
      this.input = new HashMap<>();
    }
    this.input.put(key, inputItem);
    return this;
  }

  /**
   * The input parameters for the trigger. Values are literals; expressions are not allowed.
   * @return input
   */
  @NotNull 
  @Schema(name = "input", description = "The input parameters for the trigger. Values are literals; expressions are not allowed.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("input")
  public Map<String, Object> getInput() {
    return input;
  }

  @JsonProperty("input")
  public void setInput(Map<String, Object> input) {
    this.input = input;
  }

  public TriggerSubscriptionRequestModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the subscription, e.g. Mailchimp subscription.
   * @return name
   */
  
  @Schema(name = "name", description = "The name of the subscription, e.g. Mailchimp subscription.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public TriggerSubscriptionRequestModel webhookUrl(@Nullable String webhookUrl) {
    this.webhookUrl = webhookUrl;
    return this;
  }

  /**
   * The URL where trigger events of this subscription are delivered. Overrides the environment webhook URL; events are still signed with the environment signing secret. Only a call authenticated with an API key may set or change it; a call with a connected user's JWT answers 400.
   * @return webhookUrl
   */
  
  @Schema(name = "webhookUrl", description = "The URL where trigger events of this subscription are delivered. Overrides the environment webhook URL; events are still signed with the environment signing secret. Only a call authenticated with an API key may set or change it; a call with a connected user's JWT answers 400.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("webhookUrl")
  public @Nullable String getWebhookUrl() {
    return webhookUrl;
  }

  @JsonProperty("webhookUrl")
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
    TriggerSubscriptionRequestModel triggerSubscriptionRequest = (TriggerSubscriptionRequestModel) o;
    return Objects.equals(this.externalId, triggerSubscriptionRequest.externalId) &&
        Objects.equals(this.input, triggerSubscriptionRequest.input) &&
        Objects.equals(this.name, triggerSubscriptionRequest.name) &&
        Objects.equals(this.webhookUrl, triggerSubscriptionRequest.webhookUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(externalId, input, name, webhookUrl);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TriggerSubscriptionRequestModel {\n");
    sb.append("    externalId: ").append(toIndentedString(externalId)).append("\n");
    sb.append("    input: ").append(toIndentedString(input)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    webhookUrl: ").append(toIndentedString(webhookUrl)).append("\n");
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

