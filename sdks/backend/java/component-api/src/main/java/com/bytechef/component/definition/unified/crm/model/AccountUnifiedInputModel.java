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
import com.bytechef.component.definition.unified.crm.model.common.LifecycleStage;
import com.bytechef.component.definition.unified.crm.model.common.Phone;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Normalized, provider-agnostic representation of a CRM account as supplied on write operations. This is the unified
 * shape into which each provider's native account input model is translated, so that account creates and updates can be
 * expressed identically across CRM providers. Custom fields that do not map onto a normalized property are carried in
 * the {@link #getCustomFields() custom fields} map.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class AccountUnifiedInputModel implements UnifiedInputModel {

    private String name;
    private String description;
    private String industry; // Industry
    private int numberOfEmployees;
    private LifecycleStage lifecycleStage;
    private OffsetDateTime lastActivityDate;
    private String website;
    private String ownerId;
    private List<Address> addresses;
    private List<Email> emails;
    private List<Phone> phones;
    private Map<String, ?> customFields;

    protected AccountUnifiedInputModel() {
    }

    /**
     * Creates a fully populated unified account input model.
     *
     * @param name              the account name
     * @param description       the free-form account description
     * @param industry          the industry the account operates in
     * @param numberOfEmployees the number of employees at the account
     * @param lifecycleStage    the lifecycle stage the account is in
     * @param lastActivityDate  the timestamp of the most recent activity on the account
     * @param website           the account website URL
     * @param ownerId           the identifier of the owning user
     * @param addresses         the postal addresses of the account
     * @param emails            the email addresses of the account
     * @param phones            the phone numbers of the account
     * @param customFields      the provider-specific custom fields keyed by field name
     */
    public AccountUnifiedInputModel(
        String name, String description, String industry, int numberOfEmployees, LifecycleStage lifecycleStage,
        OffsetDateTime lastActivityDate, String website, String ownerId, List<Address> addresses, List<Email> emails,
        List<Phone> phones, Map<String, ?> customFields) {

        this.name = name;
        this.description = description;
        this.industry = industry;
        this.numberOfEmployees = numberOfEmployees;
        this.lifecycleStage = lifecycleStage;
        this.lastActivityDate = lastActivityDate;
        this.website = website;
        this.ownerId = ownerId;
        this.addresses = addresses;
        this.emails = emails;
        this.phones = phones;
        this.customFields = customFields;
    }

    /**
     * Returns the account name.
     *
     * @return the account name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the free-form description of the account.
     *
     * @return the account description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the industry the account operates in.
     *
     * @return the account industry
     */
    public String getIndustry() {
        return industry;
    }

    /**
     * Returns the number of employees at the account.
     *
     * @return the employee count
     */
    public int getNumberOfEmployees() {
        return numberOfEmployees;
    }

    /**
     * Returns the lifecycle stage the account is currently in.
     *
     * @return the account lifecycle stage
     */
    public LifecycleStage getLifecycleStage() {
        return lifecycleStage;
    }

    /**
     * Returns the timestamp of the most recent activity recorded against the account.
     *
     * @return the last activity date
     */
    public OffsetDateTime getLastActivityDate() {
        return lastActivityDate;
    }

    /**
     * Returns the account's website URL.
     *
     * @return the account website
     */
    public String getWebsite() {
        return website;
    }

    /**
     * Returns the identifier of the user who owns the account.
     *
     * @return the owner identifier
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * Returns the postal addresses associated with the account.
     *
     * @return the account addresses
     */
    public List<Address> getAddresses() {
        return addresses;
    }

    /**
     * Returns the email addresses associated with the account.
     *
     * @return the account emails
     */
    public List<Email> getEmails() {
        return emails;
    }

    /**
     * Returns the phone numbers associated with the account.
     *
     * @return the account phone numbers
     */
    public List<Phone> getPhones() {
        return phones;
    }

    /**
     * Returns the provider-specific custom fields that do not map onto a normalized account property, keyed by field
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

        if (!(o instanceof AccountUnifiedInputModel that)) {
            return false;
        }

        return Objects.equals(name, that.name) && Objects.equals(description, that.description) &&
            Objects.equals(industry, that.industry) && numberOfEmployees == that.numberOfEmployees &&
            lifecycleStage == that.lifecycleStage && Objects.equals(lastActivityDate, that.lastActivityDate) &&
            Objects.equals(website, that.website) && Objects.equals(ownerId, that.ownerId) &&
            Objects.equals(addresses, that.addresses) && Objects.equals(emails, that.emails) &&
            Objects.equals(phones, that.phones) && Objects.equals(customFields, that.customFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            name, description, industry, numberOfEmployees, lifecycleStage, lastActivityDate, website, ownerId,
            addresses, emails, phones, customFields);
    }

    @Override
    public String toString() {
        return "AccountUnifiedInputModel{" +
            "name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", industry='" + industry + '\'' +
            ", numberOfEmployees=" + numberOfEmployees +
            ", lifecycleStage=" + lifecycleStage +
            ", lastActivityDate=" + lastActivityDate +
            ", website='" + website + '\'' +
            ", ownerId='" + ownerId + '\'' +
            ", addresses=" + addresses +
            ", emails=" + emails +
            ", phones=" + phones +
            ", customFields=" + customFields +
            '}';
    }
}
