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
 * ContactModel
 */

@JsonTypeName("contact")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:37:03.860012+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class ContactModel {

  private JsonNullable<String> accountId = JsonNullable.<String>undefined();

  private JsonNullable<String> ownerId = JsonNullable.<String>undefined();

  @Valid
  private List<@Valid AddressesInnerModel> addresses = new ArrayList<>();

  @Valid
  private List<@Valid EmailsInnerModel> emails = new ArrayList<>();

  private JsonNullable<String> firstName = JsonNullable.<String>undefined();

  private String id;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> lastActivityDate = JsonNullable.<OffsetDateTime>undefined();

  private JsonNullable<String> lastName = JsonNullable.<String>undefined();

  @Valid
  private List<@Valid PhonesInnerModel> phones = new ArrayList<>();

  private JsonNullable<LifecycleStageModel> lifecycleStage = JsonNullable.<LifecycleStageModel>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> createdDate = JsonNullable.<OffsetDateTime>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime lastModifiedDate;

  @Valid
  private Map<String, Object> rawData = new HashMap<>();

  public ContactModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ContactModel(String accountId, String ownerId, List<@Valid AddressesInnerModel> addresses, List<@Valid EmailsInnerModel> emails, String firstName, String id, OffsetDateTime lastActivityDate, String lastName, LifecycleStageModel lifecycleStage, OffsetDateTime createdDate, OffsetDateTime lastModifiedDate) {
    this.accountId = JsonNullable.of(accountId);
    this.ownerId = JsonNullable.of(ownerId);
    this.addresses = addresses;
    this.emails = emails;
    this.firstName = JsonNullable.of(firstName);
    this.id = id;
    this.lastActivityDate = JsonNullable.of(lastActivityDate);
    this.lastName = JsonNullable.of(lastName);
    this.lifecycleStage = JsonNullable.of(lifecycleStage);
    this.createdDate = JsonNullable.of(createdDate);
    this.lastModifiedDate = lastModifiedDate;
  }

  public ContactModel accountId(String accountId) {
    this.accountId = JsonNullable.of(accountId);
    return this;
  }

  /**
   * Get accountId
   * @return accountId
   */
  @NotNull 
  @Schema(name = "accountId", example = "fd089246-09b1-4e3b-a60a-7a76314bbcce", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("accountId")
  public JsonNullable<String> getAccountId() {
    return accountId;
  }

  public void setAccountId(JsonNullable<String> accountId) {
    this.accountId = accountId;
  }

  public ContactModel ownerId(String ownerId) {
    this.ownerId = JsonNullable.of(ownerId);
    return this;
  }

  /**
   * Get ownerId
   * @return ownerId
   */
  @NotNull 
  @Schema(name = "ownerId", example = "23e640fe-6105-4a11-a636-3aa6b6c6e762", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("ownerId")
  public JsonNullable<String> getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(JsonNullable<String> ownerId) {
    this.ownerId = ownerId;
  }

  public ContactModel addresses(List<@Valid AddressesInnerModel> addresses) {
    this.addresses = addresses;
    return this;
  }

  public ContactModel addAddressesItem(AddressesInnerModel addressesItem) {
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

  public ContactModel emails(List<@Valid EmailsInnerModel> emails) {
    this.emails = emails;
    return this;
  }

  public ContactModel addEmailsItem(EmailsInnerModel emailsItem) {
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

  public ContactModel firstName(String firstName) {
    this.firstName = JsonNullable.of(firstName);
    return this;
  }

  /**
   * Get firstName
   * @return firstName
   */
  @NotNull 
  @Schema(name = "firstName", example = "George", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("firstName")
  public JsonNullable<String> getFirstName() {
    return firstName;
  }

  public void setFirstName(JsonNullable<String> firstName) {
    this.firstName = firstName;
  }

  public ContactModel id(String id) {
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

  public ContactModel lastActivityDate(OffsetDateTime lastActivityDate) {
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

  public ContactModel lastName(String lastName) {
    this.lastName = JsonNullable.of(lastName);
    return this;
  }

  /**
   * Get lastName
   * @return lastName
   */
  @NotNull 
  @Schema(name = "lastName", example = "Xing", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("lastName")
  public JsonNullable<String> getLastName() {
    return lastName;
  }

  public void setLastName(JsonNullable<String> lastName) {
    this.lastName = lastName;
  }

  public ContactModel phones(List<@Valid PhonesInnerModel> phones) {
    this.phones = phones;
    return this;
  }

  public ContactModel addPhonesItem(PhonesInnerModel phonesItem) {
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
  @Valid 
  @Schema(name = "phones", example = "[{phoneNumber=+14151234567, phoneNumberType=primary}]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("phones")
  public List<@Valid PhonesInnerModel> getPhones() {
    return phones;
  }

  public void setPhones(List<@Valid PhonesInnerModel> phones) {
    this.phones = phones;
  }

  public ContactModel lifecycleStage(LifecycleStageModel lifecycleStage) {
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

  public ContactModel createdDate(OffsetDateTime createdDate) {
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

  public ContactModel lastModifiedDate(OffsetDateTime lastModifiedDate) {
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

  public ContactModel rawData(Map<String, Object> rawData) {
    this.rawData = rawData;
    return this;
  }

  public ContactModel putRawDataItem(String key, Object rawDataItem) {
    if (this.rawData == null) {
      this.rawData = new HashMap<>();
    }
    this.rawData.put(key, rawDataItem);
    return this;
  }

  /**
   * Get rawData
   * @return rawData
   */
  
  @Schema(name = "rawData", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("rawData")
  public Map<String, Object> getRawData() {
    return rawData;
  }

  public void setRawData(Map<String, Object> rawData) {
    this.rawData = rawData;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ContactModel contact = (ContactModel) o;
    return Objects.equals(this.accountId, contact.accountId) &&
        Objects.equals(this.ownerId, contact.ownerId) &&
        Objects.equals(this.addresses, contact.addresses) &&
        Objects.equals(this.emails, contact.emails) &&
        Objects.equals(this.firstName, contact.firstName) &&
        Objects.equals(this.id, contact.id) &&
        Objects.equals(this.lastActivityDate, contact.lastActivityDate) &&
        Objects.equals(this.lastName, contact.lastName) &&
        Objects.equals(this.phones, contact.phones) &&
        Objects.equals(this.lifecycleStage, contact.lifecycleStage) &&
        Objects.equals(this.createdDate, contact.createdDate) &&
        Objects.equals(this.lastModifiedDate, contact.lastModifiedDate) &&
        Objects.equals(this.rawData, contact.rawData);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountId, ownerId, addresses, emails, firstName, id, lastActivityDate, lastName, phones, lifecycleStage, createdDate, lastModifiedDate, rawData);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ContactModel {\n");
    sb.append("    accountId: ").append(toIndentedString(accountId)).append("\n");
    sb.append("    ownerId: ").append(toIndentedString(ownerId)).append("\n");
    sb.append("    addresses: ").append(toIndentedString(addresses)).append("\n");
    sb.append("    emails: ").append(toIndentedString(emails)).append("\n");
    sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastActivityDate: ").append(toIndentedString(lastActivityDate)).append("\n");
    sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
    sb.append("    phones: ").append(toIndentedString(phones)).append("\n");
    sb.append("    lifecycleStage: ").append(toIndentedString(lifecycleStage)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    rawData: ").append(toIndentedString(rawData)).append("\n");
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

