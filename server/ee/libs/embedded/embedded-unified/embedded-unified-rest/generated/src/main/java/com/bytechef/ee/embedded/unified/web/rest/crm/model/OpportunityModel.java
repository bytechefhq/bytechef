package com.bytechef.ee.embedded.unified.web.rest.crm.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.OffsetDateTime;
import java.util.Arrays;
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
 * OpportunityModel
 */

@JsonTypeName("opportunity")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-23T12:37:00.707409+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class OpportunityModel {

  private JsonNullable<String> accountId = JsonNullable.<String>undefined();

  private JsonNullable<Integer> amount = JsonNullable.<Integer>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> closeDate = JsonNullable.<OffsetDateTime>undefined();

  private JsonNullable<String> description = JsonNullable.<String>undefined();

  private String id;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> lastActivityDate = JsonNullable.<OffsetDateTime>undefined();

  private JsonNullable<String> name = JsonNullable.<String>undefined();

  private JsonNullable<String> ownerId = JsonNullable.<String>undefined();

  private JsonNullable<String> pipeline = JsonNullable.<String>undefined();

  private JsonNullable<String> stage = JsonNullable.<String>undefined();

  private JsonNullable<String> status = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> createdDate = JsonNullable.<OffsetDateTime>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime lastModifiedDate;

  public OpportunityModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public OpportunityModel(Integer amount, String description, String id, OffsetDateTime lastActivityDate, String name, String ownerId, String pipeline, String stage, String status, OffsetDateTime createdDate, OffsetDateTime lastModifiedDate) {
    this.amount = JsonNullable.of(amount);
    this.description = JsonNullable.of(description);
    this.id = id;
    this.lastActivityDate = JsonNullable.of(lastActivityDate);
    this.name = JsonNullable.of(name);
    this.ownerId = JsonNullable.of(ownerId);
    this.pipeline = JsonNullable.of(pipeline);
    this.stage = JsonNullable.of(stage);
    this.status = JsonNullable.of(status);
    this.createdDate = JsonNullable.of(createdDate);
    this.lastModifiedDate = lastModifiedDate;
  }

  public OpportunityModel accountId(String accountId) {
    this.accountId = JsonNullable.of(accountId);
    return this;
  }

  /**
   * Get accountId
   * @return accountId
   */
  
  @Schema(name = "account_id", example = "fd089246-09b1-4e3b-a60a-7a76314bbcce", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("account_id")
  public JsonNullable<String> getAccountId() {
    return accountId;
  }

  public void setAccountId(JsonNullable<String> accountId) {
    this.accountId = accountId;
  }

  public OpportunityModel amount(Integer amount) {
    this.amount = JsonNullable.of(amount);
    return this;
  }

  /**
   * Get amount
   * @return amount
   */
  @NotNull 
  @Schema(name = "amount", example = "100000", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("amount")
  public JsonNullable<Integer> getAmount() {
    return amount;
  }

  public void setAmount(JsonNullable<Integer> amount) {
    this.amount = amount;
  }

  public OpportunityModel closeDate(OffsetDateTime closeDate) {
    this.closeDate = JsonNullable.of(closeDate);
    return this;
  }

  /**
   * Get closeDate
   * @return closeDate
   */
  @Valid 
  @Schema(name = "closeDate", example = "2023-02-27T00:00Z", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("closeDate")
  public JsonNullable<OffsetDateTime> getCloseDate() {
    return closeDate;
  }

  public void setCloseDate(JsonNullable<OffsetDateTime> closeDate) {
    this.closeDate = closeDate;
  }

  public OpportunityModel description(String description) {
    this.description = JsonNullable.of(description);
    return this;
  }

  /**
   * Get description
   * @return description
   */
  @NotNull 
  @Schema(name = "description", example = "Wants to use open source unified API for third-party integrations", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("description")
  public JsonNullable<String> getDescription() {
    return description;
  }

  public void setDescription(JsonNullable<String> description) {
    this.description = description;
  }

  public OpportunityModel id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @Schema(name = "id", example = "54312", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public OpportunityModel lastActivityDate(OffsetDateTime lastActivityDate) {
    this.lastActivityDate = JsonNullable.of(lastActivityDate);
    return this;
  }

  /**
   * Get lastActivityDate
   * @return lastActivityDate
   */
  @NotNull @Valid 
  @Schema(name = "lastActivityDate", example = "2023-02-27T00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("lastActivityDate")
  public JsonNullable<OffsetDateTime> getLastActivityDate() {
    return lastActivityDate;
  }

  public void setLastActivityDate(JsonNullable<OffsetDateTime> lastActivityDate) {
    this.lastActivityDate = lastActivityDate;
  }

  public OpportunityModel name(String name) {
    this.name = JsonNullable.of(name);
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @Schema(name = "name", example = "Needs third-party integrations", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public JsonNullable<String> getName() {
    return name;
  }

  public void setName(JsonNullable<String> name) {
    this.name = name;
  }

  public OpportunityModel ownerId(String ownerId) {
    this.ownerId = JsonNullable.of(ownerId);
    return this;
  }

  /**
   * Get ownerId
   * @return ownerId
   */
  @NotNull 
  @Schema(name = "ownerId", example = "d8ceb3ff-8b7f-4fa7-b8de-849292f6ca69", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("ownerId")
  public JsonNullable<String> getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(JsonNullable<String> ownerId) {
    this.ownerId = ownerId;
  }

  public OpportunityModel pipeline(String pipeline) {
    this.pipeline = JsonNullable.of(pipeline);
    return this;
  }

  /**
   * Get pipeline
   * @return pipeline
   */
  @NotNull 
  @Schema(name = "pipeline", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("pipeline")
  public JsonNullable<String> getPipeline() {
    return pipeline;
  }

  public void setPipeline(JsonNullable<String> pipeline) {
    this.pipeline = pipeline;
  }

  public OpportunityModel stage(String stage) {
    this.stage = JsonNullable.of(stage);
    return this;
  }

  /**
   * Get stage
   * @return stage
   */
  @NotNull 
  @Schema(name = "stage", example = "Closed Won", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("stage")
  public JsonNullable<String> getStage() {
    return stage;
  }

  public void setStage(JsonNullable<String> stage) {
    this.stage = stage;
  }

  public OpportunityModel status(String status) {
    this.status = JsonNullable.of(status);
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull 
  @Schema(name = "status", example = "OPEN", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public JsonNullable<String> getStatus() {
    return status;
  }

  public void setStatus(JsonNullable<String> status) {
    this.status = status;
  }

  public OpportunityModel createdDate(OffsetDateTime createdDate) {
    this.createdDate = JsonNullable.of(createdDate);
    return this;
  }

  /**
   * Get createdDate
   * @return createdDate
   */
  @NotNull @Valid 
  @Schema(name = "createdDate", example = "2023-02-27T00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("createdDate")
  public JsonNullable<OffsetDateTime> getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(JsonNullable<OffsetDateTime> createdDate) {
    this.createdDate = createdDate;
  }

  public OpportunityModel lastModifiedDate(OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
    return this;
  }

  /**
   * Get lastModifiedDate
   * @return lastModifiedDate
   */
  @NotNull @Valid 
  @Schema(name = "lastModifiedDate", example = "2022-02-27T00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("lastModifiedDate")
  public OffsetDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OpportunityModel opportunity = (OpportunityModel) o;
    return equalsNullable(this.accountId, opportunity.accountId) &&
        Objects.equals(this.amount, opportunity.amount) &&
        equalsNullable(this.closeDate, opportunity.closeDate) &&
        Objects.equals(this.description, opportunity.description) &&
        Objects.equals(this.id, opportunity.id) &&
        Objects.equals(this.lastActivityDate, opportunity.lastActivityDate) &&
        Objects.equals(this.name, opportunity.name) &&
        Objects.equals(this.ownerId, opportunity.ownerId) &&
        Objects.equals(this.pipeline, opportunity.pipeline) &&
        Objects.equals(this.stage, opportunity.stage) &&
        Objects.equals(this.status, opportunity.status) &&
        Objects.equals(this.createdDate, opportunity.createdDate) &&
        Objects.equals(this.lastModifiedDate, opportunity.lastModifiedDate);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(accountId), amount, hashCodeNullable(closeDate), description, id, lastActivityDate, name, ownerId, pipeline, stage, status, createdDate, lastModifiedDate);
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
    sb.append("class OpportunityModel {\n");
    sb.append("    accountId: ").append(toIndentedString(accountId)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
    sb.append("    closeDate: ").append(toIndentedString(closeDate)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastActivityDate: ").append(toIndentedString(lastActivityDate)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    ownerId: ").append(toIndentedString(ownerId)).append("\n");
    sb.append("    pipeline: ").append(toIndentedString(pipeline)).append("\n");
    sb.append("    stage: ").append(toIndentedString(stage)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
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

