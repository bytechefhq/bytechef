package com.bytechef.ee.embedded.unified.web.rest.crm.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.unified.web.rest.crm.model.AccountModel;
import com.bytechef.ee.embedded.unified.web.rest.crm.model.AddressesInnerModel;
import com.bytechef.ee.embedded.unified.web.rest.crm.model.ContactModel;
import com.bytechef.ee.embedded.unified.web.rest.crm.model.EmailsInnerModel;
import com.bytechef.ee.embedded.unified.web.rest.crm.model.PhonesInnerModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
 * LeadModel
 */

@JsonTypeName("lead")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-07T14:49:44.711094+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public class LeadModel {

  @Valid
  private List<@Valid AddressesInnerModel> addresses = new ArrayList<>();

  private JsonNullable<String> company = JsonNullable.<String>undefined();

  private JsonNullable<String> convertedAccountId = JsonNullable.<String>undefined();

  private @Nullable AccountModel convertedAccount;

  private JsonNullable<String> convertedContactId = JsonNullable.<String>undefined();

  private @Nullable ContactModel convertedContact;

  @Valid
  private List<@Valid EmailsInnerModel> emails = new ArrayList<>();

  private JsonNullable<String> firstName = JsonNullable.<String>undefined();

  private String id;

  private JsonNullable<String> lastLame = JsonNullable.<String>undefined();

  private JsonNullable<String> leadSource = JsonNullable.<String>undefined();

  private JsonNullable<String> ownerId = JsonNullable.<String>undefined();

  @Valid
  private List<@Valid PhonesInnerModel> phones = new ArrayList<>();

  private JsonNullable<String> title = JsonNullable.<String>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private JsonNullable<OffsetDateTime> createdDate = JsonNullable.<OffsetDateTime>undefined();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime lastModifiedDate;

  public LeadModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LeadModel(List<@Valid AddressesInnerModel> addresses, String company, String convertedAccountId, String convertedContactId, String id, String leadSource, String ownerId, String title, OffsetDateTime createdDate, OffsetDateTime lastModifiedDate) {
    this.addresses = addresses;
    this.company = JsonNullable.of(company);
    this.convertedAccountId = JsonNullable.of(convertedAccountId);
    this.convertedContactId = JsonNullable.of(convertedContactId);
    this.id = id;
    this.leadSource = JsonNullable.of(leadSource);
    this.ownerId = JsonNullable.of(ownerId);
    this.title = JsonNullable.of(title);
    this.createdDate = JsonNullable.of(createdDate);
    this.lastModifiedDate = lastModifiedDate;
  }

  public LeadModel addresses(List<@Valid AddressesInnerModel> addresses) {
    this.addresses = addresses;
    return this;
  }

  public LeadModel addAddressesItem(AddressesInnerModel addressesItem) {
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

  public LeadModel company(String company) {
    this.company = JsonNullable.of(company);
    return this;
  }

  /**
   * Get company
   * @return company
   */
  @NotNull 
  @Schema(name = "company", example = "ByteChef", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("company")
  public JsonNullable<String> getCompany() {
    return company;
  }

  public void setCompany(JsonNullable<String> company) {
    this.company = company;
  }

  public LeadModel convertedAccountId(String convertedAccountId) {
    this.convertedAccountId = JsonNullable.of(convertedAccountId);
    return this;
  }

  /**
   * Get convertedAccountId
   * @return convertedAccountId
   */
  @NotNull 
  @Schema(name = "convertedAccountId", example = "88cc44ca-7a34-4e8b-b0da-51c3aae34daf", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("convertedAccountId")
  public JsonNullable<String> getConvertedAccountId() {
    return convertedAccountId;
  }

  public void setConvertedAccountId(JsonNullable<String> convertedAccountId) {
    this.convertedAccountId = convertedAccountId;
  }

  public LeadModel convertedAccount(@Nullable AccountModel convertedAccount) {
    this.convertedAccount = convertedAccount;
    return this;
  }

  /**
   * Get convertedAccount
   * @return convertedAccount
   */
  @Valid 
  @Schema(name = "convertedAccount", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("convertedAccount")
  public @Nullable AccountModel getConvertedAccount() {
    return convertedAccount;
  }

  public void setConvertedAccount(@Nullable AccountModel convertedAccount) {
    this.convertedAccount = convertedAccount;
  }

  public LeadModel convertedContactId(String convertedContactId) {
    this.convertedContactId = JsonNullable.of(convertedContactId);
    return this;
  }

  /**
   * Get convertedContactId
   * @return convertedContactId
   */
  @NotNull 
  @Schema(name = "convertedContactId", example = "8c8de778-a219-4d6c-848c-1d57b52149f6", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("convertedContactId")
  public JsonNullable<String> getConvertedContactId() {
    return convertedContactId;
  }

  public void setConvertedContactId(JsonNullable<String> convertedContactId) {
    this.convertedContactId = convertedContactId;
  }

  public LeadModel convertedContact(@Nullable ContactModel convertedContact) {
    this.convertedContact = convertedContact;
    return this;
  }

  /**
   * Get convertedContact
   * @return convertedContact
   */
  @Valid 
  @Schema(name = "convertedContact", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("convertedContact")
  public @Nullable ContactModel getConvertedContact() {
    return convertedContact;
  }

  public void setConvertedContact(@Nullable ContactModel convertedContact) {
    this.convertedContact = convertedContact;
  }

  public LeadModel emails(List<@Valid EmailsInnerModel> emails) {
    this.emails = emails;
    return this;
  }

  public LeadModel addEmailsItem(EmailsInnerModel emailsItem) {
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

  public LeadModel firstName(String firstName) {
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

  public LeadModel id(String id) {
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

  public LeadModel lastLame(String lastLame) {
    this.lastLame = JsonNullable.of(lastLame);
    return this;
  }

  /**
   * Get lastLame
   * @return lastLame
   */
  
  @Schema(name = "lastLame", example = "Xing", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastLame")
  public JsonNullable<String> getLastLame() {
    return lastLame;
  }

  public void setLastLame(JsonNullable<String> lastLame) {
    this.lastLame = lastLame;
  }

  public LeadModel leadSource(String leadSource) {
    this.leadSource = JsonNullable.of(leadSource);
    return this;
  }

  /**
   * Get leadSource
   * @return leadSource
   */
  @NotNull 
  @Schema(name = "leadSource", example = "API Blogger", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("leadSource")
  public JsonNullable<String> getLeadSource() {
    return leadSource;
  }

  public void setLeadSource(JsonNullable<String> leadSource) {
    this.leadSource = leadSource;
  }

  public LeadModel ownerId(String ownerId) {
    this.ownerId = JsonNullable.of(ownerId);
    return this;
  }

  /**
   * Get ownerId
   * @return ownerId
   */
  @NotNull 
  @Schema(name = "ownerId", example = "62e5e0f7-becd-4ae2-be82-8b4e1d5ed8a2", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("ownerId")
  public JsonNullable<String> getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(JsonNullable<String> ownerId) {
    this.ownerId = ownerId;
  }

  public LeadModel phones(List<@Valid PhonesInnerModel> phones) {
    this.phones = phones;
    return this;
  }

  public LeadModel addPhonesItem(PhonesInnerModel phonesItem) {
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

  public LeadModel title(String title) {
    this.title = JsonNullable.of(title);
    return this;
  }

  /**
   * Get title
   * @return title
   */
  @NotNull 
  @Schema(name = "title", example = "Co-Founder", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("title")
  public JsonNullable<String> getTitle() {
    return title;
  }

  public void setTitle(JsonNullable<String> title) {
    this.title = title;
  }

  public LeadModel createdDate(OffsetDateTime createdDate) {
    this.createdDate = JsonNullable.of(createdDate);
    return this;
  }

  /**
   * Get createdDate
   * @return createdDate
   */
  @NotNull @Valid 
  @Schema(name = "createdDate", example = "2023-02-10T00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("createdDate")
  public JsonNullable<OffsetDateTime> getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(JsonNullable<OffsetDateTime> createdDate) {
    this.createdDate = createdDate;
  }

  public LeadModel lastModifiedDate(OffsetDateTime lastModifiedDate) {
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
    LeadModel lead = (LeadModel) o;
    return Objects.equals(this.addresses, lead.addresses) &&
        Objects.equals(this.company, lead.company) &&
        Objects.equals(this.convertedAccountId, lead.convertedAccountId) &&
        Objects.equals(this.convertedAccount, lead.convertedAccount) &&
        Objects.equals(this.convertedContactId, lead.convertedContactId) &&
        Objects.equals(this.convertedContact, lead.convertedContact) &&
        Objects.equals(this.emails, lead.emails) &&
        equalsNullable(this.firstName, lead.firstName) &&
        Objects.equals(this.id, lead.id) &&
        equalsNullable(this.lastLame, lead.lastLame) &&
        Objects.equals(this.leadSource, lead.leadSource) &&
        Objects.equals(this.ownerId, lead.ownerId) &&
        Objects.equals(this.phones, lead.phones) &&
        Objects.equals(this.title, lead.title) &&
        Objects.equals(this.createdDate, lead.createdDate) &&
        Objects.equals(this.lastModifiedDate, lead.lastModifiedDate);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(addresses, company, convertedAccountId, convertedAccount, convertedContactId, convertedContact, emails, hashCodeNullable(firstName), id, hashCodeNullable(lastLame), leadSource, ownerId, phones, title, createdDate, lastModifiedDate);
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
    sb.append("class LeadModel {\n");
    sb.append("    addresses: ").append(toIndentedString(addresses)).append("\n");
    sb.append("    company: ").append(toIndentedString(company)).append("\n");
    sb.append("    convertedAccountId: ").append(toIndentedString(convertedAccountId)).append("\n");
    sb.append("    convertedAccount: ").append(toIndentedString(convertedAccount)).append("\n");
    sb.append("    convertedContactId: ").append(toIndentedString(convertedContactId)).append("\n");
    sb.append("    convertedContact: ").append(toIndentedString(convertedContact)).append("\n");
    sb.append("    emails: ").append(toIndentedString(emails)).append("\n");
    sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastLame: ").append(toIndentedString(lastLame)).append("\n");
    sb.append("    leadSource: ").append(toIndentedString(leadSource)).append("\n");
    sb.append("    ownerId: ").append(toIndentedString(ownerId)).append("\n");
    sb.append("    phones: ").append(toIndentedString(phones)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
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

