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
 * CreateUpdateAccountModel
 */

@JsonTypeName("create_update_account")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-07T14:49:44.711094+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public class CreateUpdateAccountModel {

  private JsonNullable<String> description = JsonNullable.<String>undefined();

  private JsonNullable<String> industry = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> lastActivityDate = JsonNullable.<OffsetDateTime>undefined();

  private JsonNullable<String> name = JsonNullable.<String>undefined();

  private JsonNullable<Integer> numberOfEmployees = JsonNullable.<Integer>undefined();

  private JsonNullable<String> website = JsonNullable.<String>undefined();

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

  public CreateUpdateAccountModel description(String description) {
    this.description = JsonNullable.of(description);
    return this;
  }

  /**
   * Get description
   * @return description
   */
  
  @Schema(name = "description", example = "Integration API", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public JsonNullable<String> getDescription() {
    return description;
  }

  public void setDescription(JsonNullable<String> description) {
    this.description = description;
  }

  public CreateUpdateAccountModel industry(String industry) {
    this.industry = JsonNullable.of(industry);
    return this;
  }

  /**
   * Get industry
   * @return industry
   */
  
  @Schema(name = "industry", example = "API's", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("industry")
  public JsonNullable<String> getIndustry() {
    return industry;
  }

  public void setIndustry(JsonNullable<String> industry) {
    this.industry = industry;
  }

  public CreateUpdateAccountModel lastActivityDate(OffsetDateTime lastActivityDate) {
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

  public CreateUpdateAccountModel name(String name) {
    this.name = JsonNullable.of(name);
    return this;
  }

  /**
   * Get name
   * @return name
   */
  
  @Schema(name = "name", example = "Sample Customer", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public JsonNullable<String> getName() {
    return name;
  }

  public void setName(JsonNullable<String> name) {
    this.name = name;
  }

  public CreateUpdateAccountModel numberOfEmployees(Integer numberOfEmployees) {
    this.numberOfEmployees = JsonNullable.of(numberOfEmployees);
    return this;
  }

  /**
   * Get numberOfEmployees
   * @return numberOfEmployees
   */
  
  @Schema(name = "numberOfEmployees", example = "276000", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("numberOfEmployees")
  public JsonNullable<Integer> getNumberOfEmployees() {
    return numberOfEmployees;
  }

  public void setNumberOfEmployees(JsonNullable<Integer> numberOfEmployees) {
    this.numberOfEmployees = numberOfEmployees;
  }

  public CreateUpdateAccountModel website(String website) {
    this.website = JsonNullable.of(website);
    return this;
  }

  /**
   * Get website
   * @return website
   */
  
  @Schema(name = "website", example = "https://bytechef.io/", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("website")
  public JsonNullable<String> getWebsite() {
    return website;
  }

  public void setWebsite(JsonNullable<String> website) {
    this.website = website;
  }

  public CreateUpdateAccountModel addresses(List<@Valid AddressesInnerModel> addresses) {
    this.addresses = addresses;
    return this;
  }

  public CreateUpdateAccountModel addAddressesItem(AddressesInnerModel addressesItem) {
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

  public CreateUpdateAccountModel emails(List<@Valid EmailsInnerModel> emails) {
    this.emails = emails;
    return this;
  }

  public CreateUpdateAccountModel addEmailsItem(EmailsInnerModel emailsItem) {
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

  public CreateUpdateAccountModel phones(List<@Valid PhonesInnerModel> phones) {
    this.phones = phones;
    return this;
  }

  public CreateUpdateAccountModel addPhonesItem(PhonesInnerModel phonesItem) {
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

  public CreateUpdateAccountModel ownerId(String ownerId) {
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

  public CreateUpdateAccountModel lifecycleStage(LifecycleStageModel lifecycleStage) {
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
    return equalsNullable(this.description, createUpdateAccount.description) &&
        equalsNullable(this.industry, createUpdateAccount.industry) &&
        equalsNullable(this.lastActivityDate, createUpdateAccount.lastActivityDate) &&
        equalsNullable(this.name, createUpdateAccount.name) &&
        equalsNullable(this.numberOfEmployees, createUpdateAccount.numberOfEmployees) &&
        equalsNullable(this.website, createUpdateAccount.website) &&
        Objects.equals(this.addresses, createUpdateAccount.addresses) &&
        Objects.equals(this.emails, createUpdateAccount.emails) &&
        Objects.equals(this.phones, createUpdateAccount.phones) &&
        equalsNullable(this.ownerId, createUpdateAccount.ownerId) &&
        equalsNullable(this.lifecycleStage, createUpdateAccount.lifecycleStage) &&
        Objects.equals(this.customFields, createUpdateAccount.customFields);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(description), hashCodeNullable(industry), hashCodeNullable(lastActivityDate), hashCodeNullable(name), hashCodeNullable(numberOfEmployees), hashCodeNullable(website), addresses, emails, phones, hashCodeNullable(ownerId), hashCodeNullable(lifecycleStage), customFields);
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
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    industry: ").append(toIndentedString(industry)).append("\n");
    sb.append("    lastActivityDate: ").append(toIndentedString(lastActivityDate)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    numberOfEmployees: ").append(toIndentedString(numberOfEmployees)).append("\n");
    sb.append("    website: ").append(toIndentedString(website)).append("\n");
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

