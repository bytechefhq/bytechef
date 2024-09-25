/*
 * Copyright 2023-present ByteChef Inc.
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
 * Contact unified input model.
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

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUserId() {
        return userId;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public List<Email> getEmails() {
        return emails;
    }

    public List<Phone> getPhoneNumbers() {
        return phoneNumbers;
    }

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
