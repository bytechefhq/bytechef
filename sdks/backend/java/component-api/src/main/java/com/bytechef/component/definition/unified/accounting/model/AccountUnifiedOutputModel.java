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

package com.bytechef.component.definition.unified.accounting.model;

import com.bytechef.component.definition.unified.base.model.UnifiedOutputModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the normalized, provider-agnostic output shape for an accounting account. In addition to the account
 * attributes inherited from {@link AccountUnifiedInputModel}, it exposes the ByteChef identifier, the remote provider
 * identifier, the raw provider payload, and the created and last-modified timestamps returned when reading accounts
 * from a provider.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class AccountUnifiedOutputModel extends AccountUnifiedInputModel implements UnifiedOutputModel {

    private String id;
    private String remoteId;
    private Map<String, ?> remoteData;
    private Instant createdDate;
    private Instant lastModifiedDate;

    private AccountUnifiedOutputModel() {
    }

    /**
     * Creates a fully populated unified account output model.
     *
     * @param accountNumber    the account number assigned within the accounting system
     * @param companyInfoId    the identifier of the company the account belongs to
     * @param classification   the account classification (for example asset, liability, income)
     * @param currentBalance   the current balance of the account
     * @param currency         the ISO currency code of the account
     * @param description      a human-readable description of the account
     * @param name             the display name of the account
     * @param parentAccountId  the identifier of the parent account, when the account is nested
     * @param status           the account status (for example active or archived)
     * @param typ              the account type as defined by the accounting system
     * @param customFields     provider-specific custom fields keyed by field name
     * @param id               the ByteChef unified identifier of the account
     * @param remoteId         the identifier of the account in the remote provider system
     * @param remoteData       the raw provider payload for the account
     * @param createdDate      the timestamp at which the account was created
     * @param lastModifiedDate the timestamp at which the account was last modified
     */
    public AccountUnifiedOutputModel(
        String accountNumber, String companyInfoId, String classification, BigDecimal currentBalance, String currency,
        String description, String name, String parentAccountId, String status, String typ, Map<String, ?> customFields,
        String id, String remoteId, Map<String, ?> remoteData, Instant createdDate, Instant lastModifiedDate) {

        super(
            accountNumber, companyInfoId, classification, currentBalance, currency, description, name, parentAccountId,
            status, typ, customFields);

        this.id = id;
        this.remoteId = remoteId;
        this.remoteData = remoteData;
        this.createdDate = createdDate;
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * Returns the ByteChef unified identifier of the account.
     *
     * @return the unified account identifier
     */
    @Override
    public String getId() {
        return "";
    }

    /**
     * Returns the identifier of the account in the remote provider system.
     *
     * @return the remote account identifier
     */
    @Override
    public String getRemoteId() {
        return "";
    }

    /**
     * Returns the raw provider payload for the account.
     *
     * @return the remote data map
     */
    @Override
    public Map<String, ?> getRemoteData() {
        return Map.of();
    }

    /**
     * Returns the timestamp at which the account was created.
     *
     * @return the creation timestamp
     */
    @Override
    public OffsetDateTime getCreatedDate() {
        return null;
    }

    /**
     * Returns the timestamp at which the account was last modified.
     *
     * @return the last-modified timestamp
     */
    @Override
    public OffsetDateTime getLastModifiedDate() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AccountUnifiedOutputModel that)) {
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
        return "AccountUnifiedOutputModel{" +
            "id='" + id + '\'' +
            ", remoteId='" + remoteId + '\'' +
            ", remoteData=" + remoteData +
            ", createdDate=" + createdDate +
            ", lastModifiedDate=" + lastModifiedDate +
            "} " + super.toString();
    }
}
