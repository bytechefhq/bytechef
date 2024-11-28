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
import com.bytechef.component.definition.unified.crm.model.common.LifecycleStage;
import com.bytechef.component.definition.unified.crm.model.common.Phone;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Account unified input model.
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIndustry() {
        return industry;
    }

    public int getNumberOfEmployees() {
        return numberOfEmployees;
    }

    public LifecycleStage getLifecycleStage() {
        return lifecycleStage;
    }

    public OffsetDateTime getLastActivityDate() {
        return lastActivityDate;
    }

    public String getWebsite() {
        return website;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public List<Email> getEmails() {
        return emails;
    }

    public List<Phone> getPhones() {
        return phones;
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
