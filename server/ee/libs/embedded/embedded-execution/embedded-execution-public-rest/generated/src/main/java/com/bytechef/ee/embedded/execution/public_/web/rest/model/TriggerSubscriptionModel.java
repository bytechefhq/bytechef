package com.bytechef.ee.embedded.execution.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.TriggerSubscriptionErrorModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.TriggerSubscriptionStatusModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A subscription of a connected user to a component trigger.
 */

@Schema(name = "TriggerSubscription", description = "A subscription of a connected user to a component trigger.")
@JsonTypeName("TriggerSubscription")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-12T01:42:14.080303+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class TriggerSubscriptionModel {

  private String componentName;

  private Integer componentVersion;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdDate;

  private @Nullable TriggerSubscriptionErrorModel error;

  private @Nullable String externalId;

  private Long id;

  private Map<String, Object> input = new HashMap<>();

  private @Nullable String lastDeliveryError;

  private @Nullable String lastDeliveryStatus;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastEventDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime lastModifiedDate;

  private @Nullable String name;

  private @Nullable String staticWebhookUrl;

  private TriggerSubscriptionStatusModel status;

  private String triggerName;

  private @Nullable String webhookUrl;

  public TriggerSubscriptionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TriggerSubscriptionModel(String componentName, Integer componentVersion, OffsetDateTime createdDate, Long id, Map<String, Object> input, OffsetDateTime lastModifiedDate, TriggerSubscriptionStatusModel status, String triggerName) {
    this.componentName = componentName;
    this.componentVersion = componentVersion;
    this.createdDate = createdDate;
    this.id = id;
    this.input = input;
    this.lastModifiedDate = lastModifiedDate;
    this.status = status;
    this.triggerName = triggerName;
  }

  public TriggerSubscriptionModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of the component which contains the trigger.
   * @return componentName
   */
  @NotNull 
  @Schema(name = "componentName", description = "The name of the component which contains the trigger.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  @JsonProperty("componentName")
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public TriggerSubscriptionModel componentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * The version of the component.
   * @return componentVersion
   */
  @NotNull 
  @Schema(name = "componentVersion", description = "The version of the component.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentVersion")
  public Integer getComponentVersion() {
    return componentVersion;
  }

  @JsonProperty("componentVersion")
  public void setComponentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  public TriggerSubscriptionModel createdDate(OffsetDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  /**
   * When the subscription was created.
   * @return createdDate
   */
  @NotNull @Valid 
  @Schema(name = "createdDate", description = "When the subscription was created.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("createdDate")
  public OffsetDateTime getCreatedDate() {
    return createdDate;
  }

  @JsonProperty("createdDate")
  public void setCreatedDate(OffsetDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public TriggerSubscriptionModel error(@Nullable TriggerSubscriptionErrorModel error) {
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
  public @Nullable TriggerSubscriptionErrorModel getError() {
    return error;
  }

  @JsonProperty("error")
  public void setError(@Nullable TriggerSubscriptionErrorModel error) {
    this.error = error;
  }

  public TriggerSubscriptionModel externalId(@Nullable String externalId) {
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

  public TriggerSubscriptionModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The unique id of the subscription.
   * @return id
   */
  @NotNull 
  @Schema(name = "id", description = "The unique id of the subscription.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(Long id) {
    this.id = id;
  }

  public TriggerSubscriptionModel input(Map<String, Object> input) {
    this.input = input;
    return this;
  }

  public TriggerSubscriptionModel putInputItem(String key, Object inputItem) {
    if (this.input == null) {
      this.input = new HashMap<>();
    }
    this.input.put(key, inputItem);
    return this;
  }

  /**
   * The input parameters for the trigger.
   * @return input
   */
  @NotNull 
  @Schema(name = "input", description = "The input parameters for the trigger.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("input")
  public Map<String, Object> getInput() {
    return input;
  }

  @JsonProperty("input")
  public void setInput(Map<String, Object> input) {
    this.input = input;
  }

  public TriggerSubscriptionModel lastDeliveryError(@Nullable String lastDeliveryError) {
    this.lastDeliveryError = lastDeliveryError;
    return this;
  }

  /**
   * The error of the last event delivery, or null when it succeeded. Cut to 1024 characters.
   * @return lastDeliveryError
   */
  
  @Schema(name = "lastDeliveryError", description = "The error of the last event delivery, or null when it succeeded. Cut to 1024 characters.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastDeliveryError")
  public @Nullable String getLastDeliveryError() {
    return lastDeliveryError;
  }

  @JsonProperty("lastDeliveryError")
  public void setLastDeliveryError(@Nullable String lastDeliveryError) {
    this.lastDeliveryError = lastDeliveryError;
  }

  public TriggerSubscriptionModel lastDeliveryStatus(@Nullable String lastDeliveryStatus) {
    this.lastDeliveryStatus = lastDeliveryStatus;
    return this;
  }

  /**
   * The status of the last event delivery: SENT, FAILED or DROPPED.
   * @return lastDeliveryStatus
   */
  
  @Schema(name = "lastDeliveryStatus", description = "The status of the last event delivery: SENT, FAILED or DROPPED.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastDeliveryStatus")
  public @Nullable String getLastDeliveryStatus() {
    return lastDeliveryStatus;
  }

  @JsonProperty("lastDeliveryStatus")
  public void setLastDeliveryStatus(@Nullable String lastDeliveryStatus) {
    this.lastDeliveryStatus = lastDeliveryStatus;
  }

  public TriggerSubscriptionModel lastEventDate(@Nullable OffsetDateTime lastEventDate) {
    this.lastEventDate = lastEventDate;
    return this;
  }

  /**
   * When the last trigger event was produced, whether or not it was delivered.
   * @return lastEventDate
   */
  @Valid 
  @Schema(name = "lastEventDate", description = "When the last trigger event was produced, whether or not it was delivered.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastEventDate")
  public @Nullable OffsetDateTime getLastEventDate() {
    return lastEventDate;
  }

  @JsonProperty("lastEventDate")
  public void setLastEventDate(@Nullable OffsetDateTime lastEventDate) {
    this.lastEventDate = lastEventDate;
  }

  public TriggerSubscriptionModel lastModifiedDate(OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
    return this;
  }

  /**
   * When the subscription was last modified.
   * @return lastModifiedDate
   */
  @NotNull @Valid 
  @Schema(name = "lastModifiedDate", description = "When the subscription was last modified.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("lastModifiedDate")
  public OffsetDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  @JsonProperty("lastModifiedDate")
  public void setLastModifiedDate(OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public TriggerSubscriptionModel name(@Nullable String name) {
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

  public TriggerSubscriptionModel staticWebhookUrl(@Nullable String staticWebhookUrl) {
    this.staticWebhookUrl = staticWebhookUrl;
    return this;
  }

  /**
   * The ByteChef URL to register in the app; only for static webhook triggers.
   * @return staticWebhookUrl
   */
  
  @Schema(name = "staticWebhookUrl", description = "The ByteChef URL to register in the app; only for static webhook triggers.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("staticWebhookUrl")
  public @Nullable String getStaticWebhookUrl() {
    return staticWebhookUrl;
  }

  @JsonProperty("staticWebhookUrl")
  public void setStaticWebhookUrl(@Nullable String staticWebhookUrl) {
    this.staticWebhookUrl = staticWebhookUrl;
  }

  public TriggerSubscriptionModel status(TriggerSubscriptionStatusModel status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @Schema(name = "status", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public TriggerSubscriptionStatusModel getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(TriggerSubscriptionStatusModel status) {
    this.status = status;
  }

  public TriggerSubscriptionModel triggerName(String triggerName) {
    this.triggerName = triggerName;
    return this;
  }

  /**
   * The name of the trigger.
   * @return triggerName
   */
  @NotNull 
  @Schema(name = "triggerName", description = "The name of the trigger.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("triggerName")
  public String getTriggerName() {
    return triggerName;
  }

  @JsonProperty("triggerName")
  public void setTriggerName(String triggerName) {
    this.triggerName = triggerName;
  }

  public TriggerSubscriptionModel webhookUrl(@Nullable String webhookUrl) {
    this.webhookUrl = webhookUrl;
    return this;
  }

  /**
   * The URL where trigger events of this subscription are delivered, when it overrides the environment webhook URL.
   * @return webhookUrl
   */
  
  @Schema(name = "webhookUrl", description = "The URL where trigger events of this subscription are delivered, when it overrides the environment webhook URL.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    TriggerSubscriptionModel triggerSubscription = (TriggerSubscriptionModel) o;
    return Objects.equals(this.componentName, triggerSubscription.componentName) &&
        Objects.equals(this.componentVersion, triggerSubscription.componentVersion) &&
        Objects.equals(this.createdDate, triggerSubscription.createdDate) &&
        Objects.equals(this.error, triggerSubscription.error) &&
        Objects.equals(this.externalId, triggerSubscription.externalId) &&
        Objects.equals(this.id, triggerSubscription.id) &&
        Objects.equals(this.input, triggerSubscription.input) &&
        Objects.equals(this.lastDeliveryError, triggerSubscription.lastDeliveryError) &&
        Objects.equals(this.lastDeliveryStatus, triggerSubscription.lastDeliveryStatus) &&
        Objects.equals(this.lastEventDate, triggerSubscription.lastEventDate) &&
        Objects.equals(this.lastModifiedDate, triggerSubscription.lastModifiedDate) &&
        Objects.equals(this.name, triggerSubscription.name) &&
        Objects.equals(this.staticWebhookUrl, triggerSubscription.staticWebhookUrl) &&
        Objects.equals(this.status, triggerSubscription.status) &&
        Objects.equals(this.triggerName, triggerSubscription.triggerName) &&
        Objects.equals(this.webhookUrl, triggerSubscription.webhookUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, componentVersion, createdDate, error, externalId, id, input, lastDeliveryError, lastDeliveryStatus, lastEventDate, lastModifiedDate, name, staticWebhookUrl, status, triggerName, webhookUrl);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TriggerSubscriptionModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    externalId: ").append(toIndentedString(externalId)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    input: ").append(toIndentedString(input)).append("\n");
    sb.append("    lastDeliveryError: ").append(toIndentedString(lastDeliveryError)).append("\n");
    sb.append("    lastDeliveryStatus: ").append(toIndentedString(lastDeliveryStatus)).append("\n");
    sb.append("    lastEventDate: ").append(toIndentedString(lastEventDate)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    staticWebhookUrl: ").append(toIndentedString(staticWebhookUrl)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    triggerName: ").append(toIndentedString(triggerName)).append("\n");
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

