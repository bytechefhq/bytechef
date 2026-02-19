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
 * CreateUpdateContactModel
 */

@JsonTypeName("create_update_contact")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-02T08:44:57.236842+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class CreateUpdateContactModel {

  private JsonNullable<String> firstName = JsonNullable.<String>undefined();

  private JsonNullable<String> lastName = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> lastActivityDate = JsonNullable.<OffsetDateTime>undefined();

  private JsonNullable<String> accountId = JsonNullable.<String>undefined();

  @Valid
  private List<@Valid AddressesInnerModel> addresses = new ArrayList<>();

  @Valid
  private List<@Valid EmailsInnerModel> emails = new ArrayList<>();

  @Valid
  private List<@Valid PhonesInnerModel> phones = new ArrayList<>();

  private JsonNullable<String> ownerId = JsonNullable.<String>undefined();

  private JsonNullable<LifecycleStageModel> lifecycleStage = JsonNullable.<LifecycleStageModel>undefined();

  @Valid
  private Map<String, Object> customFields = new HashMap<>();

  public CreateUpdateContactModel firstName(String firstName) {
    this.firstName = JsonNullable.of(firstName);
    return this;
  }

  /**
   * Get firstName
   * @return firstName
   */
  
  @Schema(name = "firstName", example = "George", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("firstName")
  public JsonNullable<String> getFirstName() {
    return firstName;
  }

  public void setFirstName(JsonNullable<String> firstName) {
    this.firstName = firstName;
  }

  public CreateUpdateContactModel lastName(String lastName) {
    this.lastName = JsonNullable.of(lastName);
    return this;
  }

  /**
   * Get lastName
   * @return lastName
   */
  
  @Schema(name = "lastName", example = "Xing", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastName")
  public JsonNullable<String> getLastName() {
    return lastName;
  }

  public void setLastName(JsonNullable<String> lastName) {
    this.lastName = lastName;
  }

  public CreateUpdateContactModel lastActivityDate(OffsetDateTime lastActivityDate) {
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

  public CreateUpdateContactModel accountId(String accountId) {
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

  public CreateUpdateContactModel addresses(List<@Valid AddressesInnerModel> addresses) {
    this.addresses = addresses;
    return this;
  }

  public CreateUpdateContactModel addAddressesItem(AddressesInnerModel addressesItem) {
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
  @Valid 
  @Schema(name = "addresses", example = "[{addressType=shipping, city=San Francisco, country=US, postalCode=94107, state=CA, street1=525 Brannan, street2=null}]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("addresses")
  public List<@Valid AddressesInnerModel> getAddresses() {
    return addresses;
  }

  public void setAddresses(List<@Valid AddressesInnerModel> addresses) {
    this.addresses = addresses;
  }

  public CreateUpdateContactModel emails(List<@Valid EmailsInnerModel> emails) {
    this.emails = emails;
    return this;
  }

  public CreateUpdateContactModel addEmailsItem(EmailsInnerModel emailsItem) {
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
  @Valid 
  @Schema(name = "emails", example = "[{emailAddress=hello@bytechef.io, emailAddressType=work}]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("emails")
  public List<@Valid EmailsInnerModel> getEmails() {
    return emails;
  }

  public void setEmails(List<@Valid EmailsInnerModel> emails) {
    this.emails = emails;
  }

  public CreateUpdateContactModel phones(List<@Valid PhonesInnerModel> phones) {
    this.phones = phones;
    return this;
  }

  public CreateUpdateContactModel addPhonesItem(PhonesInnerModel phonesItem) {
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

  public CreateUpdateContactModel ownerId(String ownerId) {
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

  public CreateUpdateContactModel lifecycleStage(LifecycleStageModel lifecycleStage) {
    this.lifecycleStage = JsonNullable.of(lifecycleStage);
    return this;
  }

  /**
   * Get lifecycleStage
   * @return lifecycleStage
   */
  @Valid 
  @Schema(name = "lifecycleStage", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lifecycleStage")
  public JsonNullable<LifecycleStageModel> getLifecycleStage() {
    return lifecycleStage;
  }

  public void setLifecycleStage(JsonNullable<LifecycleStageModel> lifecycleStage) {
    this.lifecycleStage = lifecycleStage;
  }

  public CreateUpdateContactModel customFields(Map<String, Object> customFields) {
    this.customFields = customFields;
    return this;
  }

  public CreateUpdateContactModel putCustomFieldsItem(String key, Object customFieldsItem) {
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
    CreateUpdateContactModel createUpdateContact = (CreateUpdateContactModel) o;
    return equalsNullable(this.firstName, createUpdateContact.firstName) &&
        equalsNullable(this.lastName, createUpdateContact.lastName) &&
        equalsNullable(this.lastActivityDate, createUpdateContact.lastActivityDate) &&
        equalsNullable(this.accountId, createUpdateContact.accountId) &&
        Objects.equals(this.addresses, createUpdateContact.addresses) &&
        Objects.equals(this.emails, createUpdateContact.emails) &&
        Objects.equals(this.phones, createUpdateContact.phones) &&
        equalsNullable(this.ownerId, createUpdateContact.ownerId) &&
        equalsNullable(this.lifecycleStage, createUpdateContact.lifecycleStage) &&
        Objects.equals(this.customFields, createUpdateContact.customFields);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(firstName), hashCodeNullable(lastName), hashCodeNullable(lastActivityDate), hashCodeNullable(accountId), addresses, emails, phones, hashCodeNullable(ownerId), hashCodeNullable(lifecycleStage), customFields);
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
    sb.append("class CreateUpdateContactModel {\n");
    sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
    sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
    sb.append("    lastActivityDate: ").append(toIndentedString(lastActivityDate)).append("\n");
    sb.append("    accountId: ").append(toIndentedString(accountId)).append("\n");
    sb.append("    addresses: ").append(toIndentedString(addresses)).append("\n");
    sb.append("    emails: ").append(toIndentedString(emails)).append("\n");
    sb.append("    phones: ").append(toIndentedString(phones)).append("\n");
    sb.append("    ownerId: ").append(toIndentedString(ownerId)).append("\n");
    sb.append("    lifecycleStage: ").append(toIndentedString(lifecycleStage)).append("\n");
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

