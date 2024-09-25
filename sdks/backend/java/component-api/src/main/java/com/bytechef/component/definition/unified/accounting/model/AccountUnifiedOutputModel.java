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

package com.bytechef.component.definition.unified.accounting.model;

import com.bytechef.component.definition.unified.base.model.UnifiedOutputModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Account unified input model.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class AccountUnifiedOutputModel extends AccountUnifiedInputModel implements UnifiedOutputModel {

    private String id;
    private String remoteId;
    private Map<String, ?> remoteData;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;

    private AccountUnifiedOutputModel() {
    }

    public AccountUnifiedOutputModel(
        String accountNumber, String companyInfoId, String classification, BigDecimal currentBalance, String currency,
        String description, String name, String parentAccountId, String status, String typ, Map<String, ?> customFields,
        String id, String remoteId, Map<String, ?> remoteData, LocalDateTime createdDate,
        LocalDateTime lastModifiedDate) {

        super(
            accountNumber, companyInfoId, classification, currentBalance, currency, description, name, parentAccountId,
            status, typ, customFields);

        this.id = id;
        this.remoteId = remoteId;
        this.remoteData = remoteData;
        this.createdDate = createdDate;
        this.lastModifiedDate = lastModifiedDate;
    }

    @Override
    public String getId() {
        return "";
    }

    @Override
    public String getRemoteId() {
        return "";
    }

    @Override
    public Map<String, ?> getRemoteData() {
        return Map.of();
    }

    @Override
    public LocalDateTime getCreatedDate() {
        return null;
    }

    @Override
    public LocalDateTime getLastModifiedDate() {
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
