package com.bytechef.ee.embedded.unified.web.rest.accounting.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.math.BigDecimal;
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
 * AccountModel
 */

@JsonTypeName("account")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-23T12:36:59.766054+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class AccountModel {

  private JsonNullable<String> accountNumber = JsonNullable.<String>undefined();

  private JsonNullable<String> companyInfoId = JsonNullable.<String>undefined();

  private JsonNullable<String> classification = JsonNullable.<String>undefined();

  private JsonNullable<BigDecimal> currentBalance = JsonNullable.<BigDecimal>undefined();

  private JsonNullable<String> currency = JsonNullable.<String>undefined();

  private JsonNullable<String> description = JsonNullable.<String>undefined();

  private JsonNullable<String> id = JsonNullable.<String>undefined();

  private JsonNullable<String> name = JsonNullable.<String>undefined();

  private JsonNullable<String> parentAccountId = JsonNullable.<String>undefined();

  private JsonNullable<String> status = JsonNullable.<String>undefined();

  private JsonNullable<String> type = JsonNullable.<String>undefined();

  @Valid
  private Map<String, Object> customFields = new HashMap<>();

  private JsonNullable<String> remoteId = JsonNullable.<String>undefined();

  private JsonNullable<Object> remoteData = JsonNullable.<Object>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> createdDate = JsonNullable.<OffsetDateTime>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  public AccountModel accountNumber(String accountNumber) {
    this.accountNumber = JsonNullable.of(accountNumber);
    return this;
  }

  /**
   * The account number
   * @return accountNumber
   */
  
  @Schema(name = "accountNumber", example = "1000", description = "The account number", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("accountNumber")
  public JsonNullable<String> getAccountNumber() {
    return accountNumber;
  }

  public void setAccountNumber(JsonNullable<String> accountNumber) {
    this.accountNumber = accountNumber;
  }

  public AccountModel companyInfoId(String companyInfoId) {
    this.companyInfoId = JsonNullable.of(companyInfoId);
    return this;
  }

  /**
   * The ID of the associated company info
   * @return companyInfoId
   */
  
  @Schema(name = "companyInfoId", example = "01a-99f1eefd-c98e-ed464fba8c-e64a867", description = "The ID of the associated company info", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("companyInfoId")
  public JsonNullable<String> getCompanyInfoId() {
    return companyInfoId;
  }

  public void setCompanyInfoId(JsonNullable<String> companyInfoId) {
    this.companyInfoId = companyInfoId;
  }

  public AccountModel classification(String classification) {
    this.classification = JsonNullable.of(classification);
    return this;
  }

  /**
   * The classification of the account
   * @return classification
   */
  
  @Schema(name = "classification", example = "Asset", description = "The classification of the account", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("classification")
  public JsonNullable<String> getClassification() {
    return classification;
  }

  public void setClassification(JsonNullable<String> classification) {
    this.classification = classification;
  }

  public AccountModel currentBalance(BigDecimal currentBalance) {
    this.currentBalance = JsonNullable.of(currentBalance);
    return this;
  }

  /**
   * The current balance of the account
   * @return currentBalance
   */
  @Valid 
  @Schema(name = "currentBalance", example = "10000", description = "The current balance of the account", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("currentBalance")
  public JsonNullable<BigDecimal> getCurrentBalance() {
    return currentBalance;
  }

  public void setCurrentBalance(JsonNullable<BigDecimal> currentBalance) {
    this.currentBalance = currentBalance;
  }

  public AccountModel currency(String currency) {
    this.currency = JsonNullable.of(currency);
    return this;
  }

  /**
   * The currency of the account
   * @return currency
   */
  
  @Schema(name = "currency", example = "USD", description = "The currency of the account", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("currency")
  public JsonNullable<String> getCurrency() {
    return currency;
  }

  public void setCurrency(JsonNullable<String> currency) {
    this.currency = currency;
  }

  public AccountModel description(String description) {
    this.description = JsonNullable.of(description);
    return this;
  }

  /**
   * A description of the account
   * @return description
   */
  
  @Schema(name = "description", example = "Main cash account for daily operations", description = "A description of the account", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public JsonNullable<String> getDescription() {
    return description;
  }

  public void setDescription(JsonNullable<String> description) {
    this.description = description;
  }

  public AccountModel id(String id) {
    this.id = JsonNullable.of(id);
    return this;
  }

  /**
   * The ID of the account record
   * @return id
   */
  
  @Schema(name = "id", example = "801f9ede-c698-4e66-a7fc-48d19eebaa4f", description = "The ID of the account record", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public JsonNullable<String> getId() {
    return id;
  }

  public void setId(JsonNullable<String> id) {
    this.id = id;
  }

  public AccountModel name(String name) {
    this.name = JsonNullable.of(name);
    return this;
  }

  /**
   * The name of the account
   * @return name
   */
  
  @Schema(name = "name", example = "Cash", description = "The name of the account", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public JsonNullable<String> getName() {
    return name;
  }

  public void setName(JsonNullable<String> name) {
    this.name = name;
  }

  public AccountModel parentAccountId(String parentAccountId) {
    this.parentAccountId = JsonNullable.of(parentAccountId);
    return this;
  }

  /**
   * The ID of the parent account
   * @return parentAccountId
   */
  
  @Schema(name = "parentAccountId", example = "8049a64c9-68ec6781eae-94fe-ebdda-1ff", description = "The ID of the parent account", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("parentAccountId")
  public JsonNullable<String> getParentAccountId() {
    return parentAccountId;
  }

  public void setParentAccountId(JsonNullable<String> parentAccountId) {
    this.parentAccountId = parentAccountId;
  }

  public AccountModel status(String status) {
    this.status = JsonNullable.of(status);
    return this;
  }

  /**
   * The status of the account
   * @return status
   */
  
  @Schema(name = "status", example = "Active", description = "The status of the account", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("status")
  public JsonNullable<String> getStatus() {
    return status;
  }

  public void setStatus(JsonNullable<String> status) {
    this.status = status;
  }

  public AccountModel type(String type) {
    this.type = JsonNullable.of(type);
    return this;
  }

  /**
   * The type of the account
   * @return type
   */
  
  @Schema(name = "type", example = "Current Asset", description = "The type of the account", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("type")
  public JsonNullable<String> getType() {
    return type;
  }

  public void setType(JsonNullable<String> type) {
    this.type = type;
  }

  public AccountModel customFields(Map<String, Object> customFields) {
    this.customFields = customFields;
    return this;
  }

  public AccountModel putCustomFieldsItem(String key, Object customFieldsItem) {
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

  public AccountModel remoteId(String remoteId) {
    this.remoteId = JsonNullable.of(remoteId);
    return this;
  }

  /**
   * The remote ID of the account in the context of the 3rd Party
   * @return remoteId
   */
  
  @Schema(name = "remoteId", example = "account_1234", description = "The remote ID of the account in the context of the 3rd Party", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("remoteId")
  public JsonNullable<String> getRemoteId() {
    return remoteId;
  }

  public void setRemoteId(JsonNullable<String> remoteId) {
    this.remoteId = remoteId;
  }

  public AccountModel remoteData(Object remoteData) {
    this.remoteData = JsonNullable.of(remoteData);
    return this;
  }

  /**
   * The remote data of the account in the context of the 3rd Party
   * @return remoteData
   */
  
  @Schema(name = "remoteData", example = "{raw_data={additional_field=some value}}", description = "The remote data of the account in the context of the 3rd Party", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("remoteData")
  public JsonNullable<Object> getRemoteData() {
    return remoteData;
  }

  public void setRemoteData(JsonNullable<Object> remoteData) {
    this.remoteData = remoteData;
  }

  public AccountModel createdDate(OffsetDateTime createdDate) {
    this.createdDate = JsonNullable.of(createdDate);
    return this;
  }

  /**
   * Get createdDate
   * @return createdDate
   */
  @Valid 
  @Schema(name = "createdDate", example = "2022-02-27T00:00Z", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdDate")
  public JsonNullable<OffsetDateTime> getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(JsonNullable<OffsetDateTime> createdDate) {
    this.createdDate = createdDate;
  }

  public AccountModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
    return this;
  }

  /**
   * Get lastModifiedDate
   * @return lastModifiedDate
   */
  @Valid 
  @Schema(name = "lastModifiedDate", example = "2022-02-27T00:00Z", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedDate")
  public @Nullable OffsetDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
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
    AccountModel account = (AccountModel) o;
    return equalsNullable(this.accountNumber, account.accountNumber) &&
        equalsNullable(this.companyInfoId, account.companyInfoId) &&
        equalsNullable(this.classification, account.classification) &&
        equalsNullable(this.currentBalance, account.currentBalance) &&
        equalsNullable(this.currency, account.currency) &&
        equalsNullable(this.description, account.description) &&
        equalsNullable(this.id, account.id) &&
        equalsNullable(this.name, account.name) &&
        equalsNullable(this.parentAccountId, account.parentAccountId) &&
        equalsNullable(this.status, account.status) &&
        equalsNullable(this.type, account.type) &&
        Objects.equals(this.customFields, account.customFields) &&
        equalsNullable(this.remoteId, account.remoteId) &&
        equalsNullable(this.remoteData, account.remoteData) &&
        equalsNullable(this.createdDate, account.createdDate) &&
        Objects.equals(this.lastModifiedDate, account.lastModifiedDate);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(accountNumber), hashCodeNullable(companyInfoId), hashCodeNullable(classification), hashCodeNullable(currentBalance), hashCodeNullable(currency), hashCodeNullable(description), hashCodeNullable(id), hashCodeNullable(name), hashCodeNullable(parentAccountId), hashCodeNullable(status), hashCodeNullable(type), customFields, hashCodeNullable(remoteId), hashCodeNullable(remoteData), hashCodeNullable(createdDate), lastModifiedDate);
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
    sb.append("class AccountModel {\n");
    sb.append("    accountNumber: ").append(toIndentedString(accountNumber)).append("\n");
    sb.append("    companyInfoId: ").append(toIndentedString(companyInfoId)).append("\n");
    sb.append("    classification: ").append(toIndentedString(classification)).append("\n");
    sb.append("    currentBalance: ").append(toIndentedString(currentBalance)).append("\n");
    sb.append("    currency: ").append(toIndentedString(currency)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    parentAccountId: ").append(toIndentedString(parentAccountId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    customFields: ").append(toIndentedString(customFields)).append("\n");
    sb.append("    remoteId: ").append(toIndentedString(remoteId)).append("\n");
    sb.append("    remoteData: ").append(toIndentedString(remoteData)).append("\n");
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

