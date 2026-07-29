/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.definition.unified.crm.model;

import com.bytechef.component.definition.unified.base.model.UnifiedInputModel;
import com.bytechef.component.definition.unified.crm.model.common.Address;
import com.bytechef.component.definition.unified.crm.model.common.Email;
import com.bytechef.component.definition.unified.crm.model.common.Phone;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Normalized, provider-agnostic representation of a CRM contact as supplied on write operations. This is the unified
 * shape into which each provider's native contact input model is translated, so that contact creates and updates can be
 * expressed identically across CRM providers. Custom fields that do not map onto a normalized property are carried in
 * the {@link #getCustomFields() custom fields} map.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ContactUnifiedInputModel implements UnifiedInputModel {

    private String firstName;
    private String lastName;
    private String userId;
    private List<Address> addresses;
    private List<Email> emails;
    private List<Phone> phoneNumbers;
    private Map<String, ?> customFields;

    protected ContactUnifiedInputModel() {
    }

    /**
     * Creates a fully populated unified contact input model.
     *
     * @param firstName    the contact's first name
     * @param lastName     the contact's last name
     * @param userId       the identifier of the user associated with the contact
     * @param addresses    the postal addresses of the contact
     * @param emails       the email addresses of the contact
     * @param phoneNumbers the phone numbers of the contact
     * @param customFields the provider-specific custom fields keyed by field name
     */
    public ContactUnifiedInputModel(
        String firstName, String lastName, String userId, List<Address> addresses, List<Email> emails,
        List<Phone> phoneNumbers, Map<String, ?> customFields) {

        this.firstName = firstName;
        this.lastName = lastName;
        this.userId = userId;
        this.addresses = addresses;
        this.emails = emails;
        this.phoneNumbers = phoneNumbers;
        this.customFields = customFields;
    }

    /**
     * Returns the contact's first name.
     *
     * @return the first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Returns the contact's last name.
     *
     * @return the last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Returns the identifier of the user associated with the contact.
     *
     * @return the user identifier
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the postal addresses associated with the contact.
     *
     * @return the contact addresses
     */
    public List<Address> getAddresses() {
        return addresses;
    }

    /**
     * Returns the email addresses associated with the contact.
     *
     * @return the contact emails
     */
    public List<Email> getEmails() {
        return emails;
    }

    /**
     * Returns the phone numbers associated with the contact.
     *
     * @return the contact phone numbers
     */
    public List<Phone> getPhoneNumbers() {
        return phoneNumbers;
    }

    /**
     * Returns the provider-specific custom fields that do not map onto a normalized contact property, keyed by field
     * name.
     *
     * @return the custom fields map
     */
    @Override
    public Map<String, ?> getCustomFields() {
        return customFields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ContactUnifiedInputModel that)) {
            return false;
        }

        return Objects.equals(firstName, that.firstName) && Objects.equals(lastName, that.lastName) &&
            Objects.equals(userId, that.userId) && Objects.equals(addresses, that.addresses) &&
            Objects.equals(emails, that.emails) && Objects.equals(phoneNumbers, that.phoneNumbers) &&
            Objects.equals(customFields, that.customFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, userId, addresses, emails, phoneNumbers, customFields);
    }

    @Override
    public String toString() {
        return "ContactUnifiedInputModel{" +
            "firstName='" + firstName + '\'' +
            ", lastName='" + lastName + '\'' +
            ", userId='" + userId + '\'' +
            ", addresses=" + addresses +
            ", emails=" + emails +
            ", phoneNumbers=" + phoneNumbers +
            ", customFields=" + customFields +
            '}';
    }
}
