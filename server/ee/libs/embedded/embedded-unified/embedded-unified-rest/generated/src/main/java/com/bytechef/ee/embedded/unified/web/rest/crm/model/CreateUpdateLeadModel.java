package com.bytechef.ee.embedded.unified.web.rest.crm.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.unified.web.rest.crm.model.AddressesInnerModel;
import com.bytechef.ee.embedded.unified.web.rest.crm.model.EmailsInnerModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
 * CreateUpdateLeadModel
 */

@JsonTypeName("create_update_lead")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-07T14:49:44.711094+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public class CreateUpdateLeadModel {

  private JsonNullable<String> company = JsonNullable.<String>undefined();

  private JsonNullable<String> firstName = JsonNullable.<String>undefined();

  private JsonNullable<String> lastName = JsonNullable.<String>undefined();

  private JsonNullable<String> leadSource = JsonNullable.<String>undefined();

  private @Nullable String title;

  @Valid
  private List<@Valid EmailsInnerModel> emails = new ArrayList<>();

  @Valid
  private List<@Valid AddressesInnerModel> addresses = new ArrayList<>();

  private JsonNullable<String> ownerId = JsonNullable.<String>undefined();

  private JsonNullable<String> convertedContactId = JsonNullable.<String>undefined();

  private JsonNullable<String> convertedAccountId = JsonNullable.<String>undefined();

  @Valid
  private Map<String, Object> customFields = new HashMap<>();

  public CreateUpdateLeadModel company(String company) {
    this.company = JsonNullable.of(company);
    return this;
  }

  /**
   * Get company
   * @return company
   */
  
  @Schema(name = "company", example = "ByteChef", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("company")
  public JsonNullable<String> getCompany() {
    return company;
  }

  public void setCompany(JsonNullable<String> company) {
    this.company = company;
  }

  public CreateUpdateLeadModel firstName(String firstName) {
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

  public CreateUpdateLeadModel lastName(String lastName) {
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

  public CreateUpdateLeadModel leadSource(String leadSource) {
    this.leadSource = JsonNullable.of(leadSource);
    return this;
  }

  /**
   * Get leadSource
   * @return leadSource
   */
  
  @Schema(name = "leadSource", example = "API Blogger", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("leadSource")
  public JsonNullable<String> getLeadSource() {
    return leadSource;
  }

  public void setLeadSource(JsonNullable<String> leadSource) {
    this.leadSource = leadSource;
  }

  public CreateUpdateLeadModel title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Get title
   * @return title
   */
  
  @Schema(name = "title", example = "Co-Founder", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public CreateUpdateLeadModel emails(List<@Valid EmailsInnerModel> emails) {
    this.emails = emails;
    return this;
  }

  public CreateUpdateLeadModel addEmailsItem(EmailsInnerModel emailsItem) {
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

  public CreateUpdateLeadModel addresses(List<@Valid AddressesInnerModel> addresses) {
    this.addresses = addresses;
    return this;
  }

  public CreateUpdateLeadModel addAddressesItem(AddressesInnerModel addressesItem) {
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

  public CreateUpdateLeadModel ownerId(String ownerId) {
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

  public CreateUpdateLeadModel convertedContactId(String convertedContactId) {
    this.convertedContactId = JsonNullable.of(convertedContactId);
    return this;
  }

  /**
   * Get convertedContactId
   * @return convertedContactId
   */
  
  @Schema(name = "convertedContactId", example = "ad43955d-2b27-4ec3-b38a-0ca07a76d43b", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("convertedContactId")
  public JsonNullable<String> getConvertedContactId() {
    return convertedContactId;
  }

  public void setConvertedContactId(JsonNullable<String> convertedContactId) {
    this.convertedContactId = convertedContactId;
  }

  public CreateUpdateLeadModel convertedAccountId(String convertedAccountId) {
    this.convertedAccountId = JsonNullable.of(convertedAccountId);
    return this;
  }

  /**
   * Get convertedAccountId
   * @return convertedAccountId
   */
  
  @Schema(name = "convertedAccountId", example = "2e1e6813-0459-47f5-ad4c-3d137c0e1fdd", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("convertedAccountId")
  public JsonNullable<String> getConvertedAccountId() {
    return convertedAccountId;
  }

  public void setConvertedAccountId(JsonNullable<String> convertedAccountId) {
    this.convertedAccountId = convertedAccountId;
  }

  public CreateUpdateLeadModel customFields(Map<String, Object> customFields) {
    this.customFields = customFields;
    return this;
  }

  public CreateUpdateLeadModel putCustomFieldsItem(String key, Object customFieldsItem) {
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
    CreateUpdateLeadModel createUpdateLead = (CreateUpdateLeadModel) o;
    return equalsNullable(this.company, createUpdateLead.company) &&
        equalsNullable(this.firstName, createUpdateLead.firstName) &&
        equalsNullable(this.lastName, createUpdateLead.lastName) &&
        equalsNullable(this.leadSource, createUpdateLead.leadSource) &&
        Objects.equals(this.title, createUpdateLead.title) &&
        Objects.equals(this.emails, createUpdateLead.emails) &&
        Objects.equals(this.addresses, createUpdateLead.addresses) &&
        equalsNullable(this.ownerId, createUpdateLead.ownerId) &&
        equalsNullable(this.convertedContactId, createUpdateLead.convertedContactId) &&
        equalsNullable(this.convertedAccountId, createUpdateLead.convertedAccountId) &&
        Objects.equals(this.customFields, createUpdateLead.customFields);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(company), hashCodeNullable(firstName), hashCodeNullable(lastName), hashCodeNullable(leadSource), title, emails, addresses, hashCodeNullable(ownerId), hashCodeNullable(convertedContactId), hashCodeNullable(convertedAccountId), customFields);
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
    sb.append("class CreateUpdateLeadModel {\n");
    sb.append("    company: ").append(toIndentedString(company)).append("\n");
    sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
    sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
    sb.append("    leadSource: ").append(toIndentedString(leadSource)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    emails: ").append(toIndentedString(emails)).append("\n");
    sb.append("    addresses: ").append(toIndentedString(addresses)).append("\n");
    sb.append("    ownerId: ").append(toIndentedString(ownerId)).append("\n");
    sb.append("    convertedContactId: ").append(toIndentedString(convertedContactId)).append("\n");
    sb.append("    convertedAccountId: ").append(toIndentedString(convertedAccountId)).append("\n");
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

