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

import com.bytechef.component.definition.unified.base.model.UnifiedOutputModel;
import com.bytechef.component.definition.unified.crm.model.common.Address;
import com.bytechef.component.definition.unified.crm.model.common.Email;
import com.bytechef.component.definition.unified.crm.model.common.Phone;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Normalized, provider-agnostic representation of a CRM contact as returned on read operations. It extends
 * {@link ContactUnifiedInputModel} with the read-only metadata assigned by the platform and the provider — the ByteChef
 * {@link #getId() id}, the provider's own {@link #getRemoteId() remote id}, the untouched {@link #getRemoteData()
 * remote data} payload, and the {@link #getCreatedDate() created} and {@link #getLastModifiedDate() last modified}
 * timestamps. Each provider's native contact output model is translated into this unified shape so that contact reads
 * look identical across CRM providers.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ContactUnifiedOutputModel extends ContactUnifiedInputModel implements UnifiedOutputModel {

    private String id;
    private String remoteId;
    private Map<String, ?> remoteData;
    private OffsetDateTime createdDate;
    private OffsetDateTime lastModifiedDate;

    private ContactUnifiedOutputModel() {
    }

    /**
     * Creates a fully populated unified contact output model, combining the unified contact attributes with the
     * platform- and provider-assigned read-only metadata.
     *
     * @param firstName        the contact's first name
     * @param lastName         the contact's last name
     * @param userId           the identifier of the user associated with the contact
     * @param addresses        the postal addresses of the contact
     * @param emails           the email addresses of the contact
     * @param phoneNumbers     the phone numbers of the contact
     * @param customFields     the provider-specific custom fields keyed by field name
     * @param id               the ByteChef unified identifier of the contact
     * @param remoteId         the provider's native identifier of the contact
     * @param remoteData       the raw, provider-native contact payload as returned by the provider
     * @param createdDate      the timestamp at which the contact was created
     * @param lastModifiedDate the timestamp at which the contact was last modified
     */
    public ContactUnifiedOutputModel(
        String firstName, String lastName, String userId, List<Address> addresses, List<Email> emails,
        List<Phone> phoneNumbers, Map<String, ?> customFields, String id, String remoteId, Map<String, ?> remoteData,
        OffsetDateTime createdDate, OffsetDateTime lastModifiedDate) {

        super(firstName, lastName, userId, addresses, emails, phoneNumbers, customFields);
        this.id = id;
        this.remoteId = remoteId;
        this.remoteData = remoteData;
        this.createdDate = createdDate;
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * Returns the ByteChef unified identifier of the contact.
     *
     * @return the unified identifier
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Returns the provider's native identifier of the contact.
     *
     * @return the remote identifier
     */
    @Override
    public String getRemoteId() {
        return remoteId;
    }

    /**
     * Returns the raw, provider-native contact payload preserved alongside the unified fields.
     *
     * @return the remote data map
     */
    @Override
    public Map<String, ?> getRemoteData() {
        return remoteData;
    }

    /**
     * Returns the timestamp at which the contact was created.
     *
     * @return the created date
     */
    @Override
    public OffsetDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Returns the timestamp at which the contact was last modified.
     *
     * @return the last modified date
     */
    @Override
    public OffsetDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ContactUnifiedOutputModel that)) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        return Objects.equals(id, that.id) && Objects.equals(remoteId, that.remoteId) &&
            Objects.equals(remoteData, that.remoteData) && Objects.equals(createdDate, that.createdDate) &&
            Objects.equals(lastModifiedDate, that.lastModifiedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, remoteId, remoteData, createdDate, lastModifiedDate);
    }

    @Override
    public String toString() {
        return "ContactUnifiedOutputModel{" +
            "id='" + id + '\'' +
            ", remoteId='" + remoteId + '\'' +
            ", remoteData=" + remoteData +
            ", createdDate=" + createdDate +
            ", lastModifiedDate=" + lastModifiedDate +
            "} " + super.toString();
    }
}
