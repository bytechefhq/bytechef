package com.bytechef.ee.embedded.unified.web.rest.crm.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.unified.web.rest.crm.model.AddressesInnerModel;
import com.bytechef.ee.embedded.unified.web.rest.crm.model.EmailsInnerModel;
import com.bytechef.ee.embedded.unified.web.rest.crm.model.LifecycleStageModel;
import com.bytechef.ee.embedded.unified.web.rest.crm.model.PhonesInnerModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:37:03.860012+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class AccountModel {

  @Valid
  private List<@Valid AddressesInnerModel> addresses = new ArrayList<>();

  private JsonNullable<String> description = JsonNullable.<String>undefined();

  @Valid
  private List<@Valid EmailsInnerModel> emails = new ArrayList<>();

  private String id;

  private JsonNullable<String> industry = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> lastActivityDate = JsonNullable.<OffsetDateTime>undefined();

  private JsonNullable<LifecycleStageModel> lifecycleStage = JsonNullable.<LifecycleStageModel>undefined();

  private JsonNullable<String> name = JsonNullable.<String>undefined();

  private JsonNullable<Integer> numberOfEmployees = JsonNullable.<Integer>undefined();

  private JsonNullable<String> ownerId = JsonNullable.<String>undefined();

  @Valid
  private List<@Valid PhonesInnerModel> phones = new ArrayList<>();

  private JsonNullable<String> website = JsonNullable.<String>undefined();

  @Valid
  private Map<String, Object> customFields = new HashMap<>();

  private JsonNullable<String> remoteId = JsonNullable.<String>undefined();

  private JsonNullable<Object> remoteData = JsonNullable.<Object>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> createdDate = JsonNullable.<OffsetDateTime>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime lastModifiedDate;

  public AccountModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AccountModel(List<@Valid AddressesInnerModel> addresses, String description, List<@Valid EmailsInnerModel> emails, String id, String industry, OffsetDateTime lastActivityDate, LifecycleStageModel lifecycleStage, String name, Integer numberOfEmployees, String ownerId, List<@Valid PhonesInnerModel> phones, String website, OffsetDateTime createdDate, OffsetDateTime lastModifiedDate) {
    this.addresses = addresses;
    this.description = JsonNullable.of(description);
    this.emails = emails;
    this.id = id;
    this.industry = JsonNullable.of(industry);
    this.lastActivityDate = JsonNullable.of(lastActivityDate);
    this.lifecycleStage = JsonNullable.of(lifecycleStage);
    this.name = JsonNullable.of(name);
    this.numberOfEmployees = JsonNullable.of(numberOfEmployees);
    this.ownerId = JsonNullable.of(ownerId);
    this.phones = phones;
    this.website = JsonNullable.of(website);
    this.createdDate = JsonNullable.of(createdDate);
    this.lastModifiedDate = lastModifiedDate;
  }

  public AccountModel addresses(List<@Valid AddressesInnerModel> addresses) {
    this.addresses = addresses;
    return this;
  }

  public AccountModel addAddressesItem(AddressesInnerModel addressesItem) {
    if (this.addresses == null) {
      this.addresses = new ArrayList<>();
    }
    this.addresses.add(addressesItem);
    return this;
  }

  /**
   * Get addresses
   * @return addresses
   */
  @NotNull @Valid 
  @Schema(name = "addresses", example = "[{addressType=shipping, city=San Francisco, country=US, postalCode=94107, state=CA, street1=525 Brannan, street2=null}]", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("addresses")
  public List<@Valid AddressesInnerModel> getAddresses() {
    return addresses;
  }

  public void setAddresses(List<@Valid AddressesInnerModel> addresses) {
    this.addresses = addresses;
  }

  public AccountModel description(String description) {
    this.description = JsonNullable.of(description);
    return this;
  }

  /**
   * Get description
   * @return description
   */
  @NotNull 
  @Schema(name = "description", example = "Integration API", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("description")
  public JsonNullable<String> getDescription() {
    return description;
  }

  public void setDescription(JsonNullable<String> description) {
    this.description = description;
  }

  public AccountModel emails(List<@Valid EmailsInnerModel> emails) {
    this.emails = emails;
    return this;
  }

  public AccountModel addEmailsItem(EmailsInnerModel emailsItem) {
    if (this.emails == null) {
      this.emails = new ArrayList<>();
    }
    this.emails.add(emailsItem);
    return this;
  }

  /**
   * Get emails
   * @return emails
   */
  @NotNull @Valid 
  @Schema(name = "emails", example = "[{emailAddress=hello@bytechef.io, emailAddressType=work}]", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("emails")
  public List<@Valid EmailsInnerModel> getEmails() {
    return emails;
  }

  public void setEmails(List<@Valid EmailsInnerModel> emails) {
    this.emails = emails;
  }

  public AccountModel id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @Schema(name = "id", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public AccountModel industry(String industry) {
    this.industry = JsonNullable.of(industry);
    return this;
  }

  /**
   * Get industry
   * @return industry
   */
  @NotNull 
  @Schema(name = "industry", example = "API's", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("industry")
  public JsonNullable<String> getIndustry() {
    return industry;
  }

  public void setIndustry(JsonNullable<String> industry) {
    this.industry = industry;
  }

  public AccountModel lastActivityDate(OffsetDateTime lastActivityDate) {
    this.lastActivityDate = JsonNullable.of(lastActivityDate);
    return this;
  }

  /**
   * Get lastActivityDate
   * @return lastActivityDate
   */
  @NotNull @Valid 
  @Schema(name = "lastActivityDate", example = "2022-02-27T00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("lastActivityDate")
  public JsonNullable<OffsetDateTime> getLastActivityDate() {
    return lastActivityDate;
  }

  public void setLastActivityDate(JsonNullable<OffsetDateTime> lastActivityDate) {
    this.lastActivityDate = lastActivityDate;
  }

  public AccountModel lifecycleStage(LifecycleStageModel lifecycleStage) {
    this.lifecycleStage = JsonNullable.of(lifecycleStage);
    return this;
  }

  /**
   * Get lifecycleStage
   * @return lifecycleStage
   */
  @NotNull @Valid 
  @Schema(name = "lifecycleStage", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("lifecycleStage")
  public JsonNullable<LifecycleStageModel> getLifecycleStage() {
    return lifecycleStage;
  }

  public void setLifecycleStage(JsonNullable<LifecycleStageModel> lifecycleStage) {
    this.lifecycleStage = lifecycleStage;
  }

  public AccountModel name(String name) {
    this.name = JsonNullable.of(name);
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @Schema(name = "name", example = "Sample Customer", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public JsonNullable<String> getName() {
    return name;
  }

  public void setName(JsonNullable<String> name) {
    this.name = name;
  }

  public AccountModel numberOfEmployees(Integer numberOfEmployees) {
    this.numberOfEmployees = JsonNullable.of(numberOfEmployees);
    return this;
  }

  /**
   * Get numberOfEmployees
   * @return numberOfEmployees
   */
  @NotNull 
  @Schema(name = "numberOfEmployees", example = "276000", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("numberOfEmployees")
  public JsonNullable<Integer> getNumberOfEmployees() {
    return numberOfEmployees;
  }

  public void setNumberOfEmployees(JsonNullable<Integer> numberOfEmployees) {
    this.numberOfEmployees = numberOfEmployees;
  }

  public AccountModel ownerId(String ownerId) {
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

  public AccountModel phones(List<@Valid PhonesInnerModel> phones) {
    this.phones = phones;
    return this;
  }

  public AccountModel addPhonesItem(PhonesInnerModel phonesItem) {
    if (this.phones == null) {
      this.phones = new ArrayList<>();
    }
    this.phones.add(phonesItem);
    return this;
  }

  /**
   * Get phones
   * @return phones
   */
  @NotNull @Valid 
  @Schema(name = "phones", example = "[{phoneNumber=+14151234567, phoneNumberType=primary}]", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("phones")
  public List<@Valid PhonesInnerModel> getPhones() {
    return phones;
  }

  public void setPhones(List<@Valid PhonesInnerModel> phones) {
    this.phones = phones;
  }

  public AccountModel website(String website) {
    this.website = JsonNullable.of(website);
    return this;
  }

  /**
   * Get website
   * @return website
   */
  @NotNull 
  @Schema(name = "website", example = "https://bytechef.io/", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("website")
  public JsonNullable<String> getWebsite() {
    return website;
  }

  public void setWebsite(JsonNullable<String> website) {
    this.website = website;
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
  @NotNull @Valid 
  @Schema(name = "createdDate", example = "2022-02-27T00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("createdDate")
  public JsonNullable<OffsetDateTime> getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(JsonNullable<OffsetDateTime> createdDate) {
    this.createdDate = createdDate;
  }

  public AccountModel lastModifiedDate(OffsetDateTime lastModifiedDate) {
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
    AccountModel account = (AccountModel) o;
    return Objects.equals(this.addresses, account.addresses) &&
        Objects.equals(this.description, account.description) &&
        Objects.equals(this.emails, account.emails) &&
        Objects.equals(this.id, account.id) &&
        Objects.equals(this.industry, account.industry) &&
        Objects.equals(this.lastActivityDate, account.lastActivityDate) &&
        Objects.equals(this.lifecycleStage, account.lifecycleStage) &&
        Objects.equals(this.name, account.name) &&
        Objects.equals(this.numberOfEmployees, account.numberOfEmployees) &&
        Objects.equals(this.ownerId, account.ownerId) &&
        Objects.equals(this.phones, account.phones) &&
        Objects.equals(this.website, account.website) &&
        Objects.equals(this.customFields, account.customFields) &&
        equalsNullable(this.remoteId, account.remoteId) &&
        equalsNullable(this.remoteData, account.remoteData) &&
        Objects.equals(this.createdDate, account.createdDate) &&
        Objects.equals(this.lastModifiedDate, account.lastModifiedDate);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(addresses, description, emails, id, industry, lastActivityDate, lifecycleStage, name, numberOfEmployees, ownerId, phones, website, customFields, hashCodeNullable(remoteId), hashCodeNullable(remoteData), createdDate, lastModifiedDate);
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
    sb.append("    addresses: ").append(toIndentedString(addresses)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    emails: ").append(toIndentedString(emails)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    industry: ").append(toIndentedString(industry)).append("\n");
    sb.append("    lastActivityDate: ").append(toIndentedString(lastActivityDate)).append("\n");
    sb.append("    lifecycleStage: ").append(toIndentedString(lifecycleStage)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    numberOfEmployees: ").append(toIndentedString(numberOfEmployees)).append("\n");
    sb.append("    ownerId: ").append(toIndentedString(ownerId)).append("\n");
    sb.append("    phones: ").append(toIndentedString(phones)).append("\n");
    sb.append("    website: ").append(toIndentedString(website)).append("\n");
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

