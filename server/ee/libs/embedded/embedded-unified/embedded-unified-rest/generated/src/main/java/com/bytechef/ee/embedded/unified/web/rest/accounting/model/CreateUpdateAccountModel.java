package com.bytechef.ee.embedded.unified.web.rest.accounting.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
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
 * CreateUpdateAccountModel
 */

@JsonTypeName("create_update_account")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:37:03.712673+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class CreateUpdateAccountModel {

  private JsonNullable<String> accountNumber = JsonNullable.<String>undefined();

  private JsonNullable<String> classification = JsonNullable.<String>undefined();

  private JsonNullable<String> companyInfoId = JsonNullable.<String>undefined();

  private JsonNullable<BigDecimal> currentBalance = JsonNullable.<BigDecimal>undefined();

  private JsonNullable<String> currency = JsonNullable.<String>undefined();

  private JsonNullable<String> description = JsonNullable.<String>undefined();

  private JsonNullable<String> name = JsonNullable.<String>undefined();

  private JsonNullable<String> status = JsonNullable.<String>undefined();

  private JsonNullable<String> parentAccountId = JsonNullable.<String>undefined();

  private JsonNullable<String> type = JsonNullable.<String>undefined();

  @Valid
  private Map<String, Object> customFields = new HashMap<>();

  public CreateUpdateAccountModel accountNumber(String accountNumber) {
    this.accountNumber = JsonNullable.of(accountNumber);
    return this;
  }

  /**
   * The account number
   * @return accountNumber
   */
  
  @Schema(name = "accountNumber", example = "12345", description = "The account number", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("accountNumber")
  public JsonNullable<String> getAccountNumber() {
    return accountNumber;
  }

  public void setAccountNumber(JsonNullable<String> accountNumber) {
    this.accountNumber = accountNumber;
  }

  public CreateUpdateAccountModel classification(String classification) {
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

  public CreateUpdateAccountModel companyInfoId(String companyInfoId) {
    this.companyInfoId = JsonNullable.of(companyInfoId);
    return this;
  }

  /**
   * The ID of the associated company info
   * @return companyInfoId
   */
  
  @Schema(name = "companyInfoId", example = "fb8e46-6e9fea1-e4a-7-e8cd84d10a6fc99", description = "The ID of the associated company info", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("companyInfoId")
  public JsonNullable<String> getCompanyInfoId() {
    return companyInfoId;
  }

  public void setCompanyInfoId(JsonNullable<String> companyInfoId) {
    this.companyInfoId = companyInfoId;
  }

  public CreateUpdateAccountModel currentBalance(BigDecimal currentBalance) {
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

  public CreateUpdateAccountModel currency(String currency) {
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

  public CreateUpdateAccountModel description(String description) {
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

  public CreateUpdateAccountModel name(String name) {
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

  public CreateUpdateAccountModel status(String status) {
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

  public CreateUpdateAccountModel parentAccountId(String parentAccountId) {
    this.parentAccountId = JsonNullable.of(parentAccountId);
    return this;
  }

  /**
   * The ID of the parent account
   * @return parentAccountId
   */
  
  @Schema(name = "parentAccountId", example = "7094a8c18d41e68e--9a6ac4fb-eef6edf-9", description = "The ID of the parent account", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("parentAccountId")
  public JsonNullable<String> getParentAccountId() {
    return parentAccountId;
  }

  public void setParentAccountId(JsonNullable<String> parentAccountId) {
    this.parentAccountId = parentAccountId;
  }

  public CreateUpdateAccountModel type(String type) {
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

  public CreateUpdateAccountModel customFields(Map<String, Object> customFields) {
    this.customFields = customFields;
    return this;
  }

  public CreateUpdateAccountModel putCustomFieldsItem(String key, Object customFieldsItem) {
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
    CreateUpdateAccountModel createUpdateAccount = (CreateUpdateAccountModel) o;
    return equalsNullable(this.accountNumber, createUpdateAccount.accountNumber) &&
        equalsNullable(this.classification, createUpdateAccount.classification) &&
        equalsNullable(this.companyInfoId, createUpdateAccount.companyInfoId) &&
        equalsNullable(this.currentBalance, createUpdateAccount.currentBalance) &&
        equalsNullable(this.currency, createUpdateAccount.currency) &&
        equalsNullable(this.description, createUpdateAccount.description) &&
        equalsNullable(this.name, createUpdateAccount.name) &&
        equalsNullable(this.status, createUpdateAccount.status) &&
        equalsNullable(this.parentAccountId, createUpdateAccount.parentAccountId) &&
        equalsNullable(this.type, createUpdateAccount.type) &&
        Objects.equals(this.customFields, createUpdateAccount.customFields);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(accountNumber), hashCodeNullable(classification), hashCodeNullable(companyInfoId), hashCodeNullable(currentBalance), hashCodeNullable(currency), hashCodeNullable(description), hashCodeNullable(name), hashCodeNullable(status), hashCodeNullable(parentAccountId), hashCodeNullable(type), customFields);
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
    sb.append("class CreateUpdateAccountModel {\n");
    sb.append("    accountNumber: ").append(toIndentedString(accountNumber)).append("\n");
    sb.append("    classification: ").append(toIndentedString(classification)).append("\n");
    sb.append("    companyInfoId: ").append(toIndentedString(companyInfoId)).append("\n");
    sb.append("    currentBalance: ").append(toIndentedString(currentBalance)).append("\n");
    sb.append("    currency: ").append(toIndentedString(currency)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    parentAccountId: ").append(toIndentedString(parentAccountId)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
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

