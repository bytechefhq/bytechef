package com.bytechef.ee.embedded.unified.web.rest.crm.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CreateUpdateOpportunityModel
 */

@JsonTypeName("create_update_opportunity")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-02T08:44:57.236842+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class CreateUpdateOpportunityModel {

  private JsonNullable<Integer> amount = JsonNullable.<Integer>undefined();

  private JsonNullable<String> closeDate = JsonNullable.<String>undefined();

  private JsonNullable<String> description = JsonNullable.<String>undefined();

  private JsonNullable<String> name = JsonNullable.<String>undefined();

  private @Nullable String stage;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> lastActivityDate = JsonNullable.<OffsetDateTime>undefined();

  private JsonNullable<String> accountId = JsonNullable.<String>undefined();

  private JsonNullable<String> ownerId = JsonNullable.<String>undefined();

  private JsonNullable<String> pipeline = JsonNullable.<String>undefined();

  @Valid
  private Map<String, Object> customFields = new HashMap<>();

  public CreateUpdateOpportunityModel amount(Integer amount) {
    this.amount = JsonNullable.of(amount);
    return this;
  }

  /**
   * Get amount
   * @return amount
   */
  
  @Schema(name = "amount", example = "100000", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("amount")
  public JsonNullable<Integer> getAmount() {
    return amount;
  }

  public void setAmount(JsonNullable<Integer> amount) {
    this.amount = amount;
  }

  public CreateUpdateOpportunityModel closeDate(String closeDate) {
    this.closeDate = JsonNullable.of(closeDate);
    return this;
  }

  /**
   * Get closeDate
   * @return closeDate
   */
  
  @Schema(name = "closeDate", example = "2022-02-10T00:00:00Z", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("closeDate")
  public JsonNullable<String> getCloseDate() {
    return closeDate;
  }

  public void setCloseDate(JsonNullable<String> closeDate) {
    this.closeDate = closeDate;
  }

  public CreateUpdateOpportunityModel description(String description) {
    this.description = JsonNullable.of(description);
    return this;
  }

  /**
   * Get description
   * @return description
   */
  
  @Schema(name = "description", example = "Wants to use open source unified API for third-party integrations", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public JsonNullable<String> getDescription() {
    return description;
  }

  public void setDescription(JsonNullable<String> description) {
    this.description = description;
  }

  public CreateUpdateOpportunityModel name(String name) {
    this.name = JsonNullable.of(name);
    return this;
  }

  /**
   * Get name
   * @return name
   */
  
  @Schema(name = "name", example = "Needs Integrations", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public JsonNullable<String> getName() {
    return name;
  }

  public void setName(JsonNullable<String> name) {
    this.name = name;
  }

  public CreateUpdateOpportunityModel stage(@Nullable String stage) {
    this.stage = stage;
    return this;
  }

  /**
   * Get stage
   * @return stage
   */
  
  @Schema(name = "stage", example = "Closed Won", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("stage")
  public @Nullable String getStage() {
    return stage;
  }

  public void setStage(@Nullable String stage) {
    this.stage = stage;
  }

  public CreateUpdateOpportunityModel lastActivityDate(OffsetDateTime lastActivityDate) {
    this.lastActivityDate = JsonNullable.of(lastActivityDate);
    return this;
  }

  /**
   * Get lastActivityDate
   * @return lastActivityDate
   */
  @Valid 
  @Schema(name = "lastActivityDate", example = "2022-02-27T00:00Z", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastActivityDate")
  public JsonNullable<OffsetDateTime> getLastActivityDate() {
    return lastActivityDate;
  }

  public void setLastActivityDate(JsonNullable<OffsetDateTime> lastActivityDate) {
    this.lastActivityDate = lastActivityDate;
  }

  public CreateUpdateOpportunityModel accountId(String accountId) {
    this.accountId = JsonNullable.of(accountId);
    return this;
  }

  /**
   * Get accountId
   * @return accountId
   */
  
  @Schema(name = "accountId", example = "64571bff-48ea-4469-9fa0-ee1a0bab38bd", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("accountId")
  public JsonNullable<String> getAccountId() {
    return accountId;
  }

  public void setAccountId(JsonNullable<String> accountId) {
    this.accountId = accountId;
  }

  public CreateUpdateOpportunityModel ownerId(String ownerId) {
    this.ownerId = JsonNullable.of(ownerId);
    return this;
  }

  /**
   * Get ownerId
   * @return ownerId
   */
  
  @Schema(name = "ownerId", example = "9f3e97fd-4d5d-4efc-959d-bbebfac079f5", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("ownerId")
  public JsonNullable<String> getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(JsonNullable<String> ownerId) {
    this.ownerId = ownerId;
  }

  public CreateUpdateOpportunityModel pipeline(String pipeline) {
    this.pipeline = JsonNullable.of(pipeline);
    return this;
  }

  /**
   * Get pipeline
   * @return pipeline
   */
  
  @Schema(name = "pipeline", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("pipeline")
  public JsonNullable<String> getPipeline() {
    return pipeline;
  }

  public void setPipeline(JsonNullable<String> pipeline) {
    this.pipeline = pipeline;
  }

  public CreateUpdateOpportunityModel customFields(Map<String, Object> customFields) {
    this.customFields = customFields;
    return this;
  }

  public CreateUpdateOpportunityModel putCustomFieldsItem(String key, Object customFieldsItem) {
    if (this.customFields == null) {
      this.customFields = new HashMap<>();
    }
    this.customFields.put(key, customFieldsItem);
    return this;
  }

  /**
   * Custom properties to be inserted that are not covered by the common object. Object keys must match exactly to the corresponding provider API.
   * @return customFields
   */
  
  @Schema(name = "customFields", description = "Custom properties to be inserted that are not covered by the common object. Object keys must match exactly to the corresponding provider API.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("customFields")
  public Map<String, Object> getCustomFields() {
    return customFields;
  }

  public void setCustomFields(Map<String, Object> customFields) {
    this.customFields = customFields;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateUpdateOpportunityModel createUpdateOpportunity = (CreateUpdateOpportunityModel) o;
    return equalsNullable(this.amount, createUpdateOpportunity.amount) &&
        equalsNullable(this.closeDate, createUpdateOpportunity.closeDate) &&
        equalsNullable(this.description, createUpdateOpportunity.description) &&
        equalsNullable(this.name, createUpdateOpportunity.name) &&
        Objects.equals(this.stage, createUpdateOpportunity.stage) &&
        equalsNullable(this.lastActivityDate, createUpdateOpportunity.lastActivityDate) &&
        equalsNullable(this.accountId, createUpdateOpportunity.accountId) &&
        equalsNullable(this.ownerId, createUpdateOpportunity.ownerId) &&
        equalsNullable(this.pipeline, createUpdateOpportunity.pipeline) &&
        Objects.equals(this.customFields, createUpdateOpportunity.customFields);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(amount), hashCodeNullable(closeDate), hashCodeNullable(description), hashCodeNullable(name), stage, hashCodeNullable(lastActivityDate), hashCodeNullable(accountId), hashCodeNullable(ownerId), hashCodeNullable(pipeline), customFields);
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateUpdateOpportunityModel {\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
    sb.append("    closeDate: ").append(toIndentedString(closeDate)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    stage: ").append(toIndentedString(stage)).append("\n");
    sb.append("    lastActivityDate: ").append(toIndentedString(lastActivityDate)).append("\n");
    sb.append("    accountId: ").append(toIndentedString(accountId)).append("\n");
    sb.append("    ownerId: ").append(toIndentedString(ownerId)).append("\n");
    sb.append("    pipeline: ").append(toIndentedString(pipeline)).append("\n");
    sb.append("    customFields: ").append(toIndentedString(customFields)).append("\n");
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

