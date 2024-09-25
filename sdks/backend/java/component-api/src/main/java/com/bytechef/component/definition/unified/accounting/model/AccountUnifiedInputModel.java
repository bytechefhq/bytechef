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

import com.bytechef.component.definition.unified.base.model.UnifiedInputModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * Account unified input model.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class AccountUnifiedInputModel implements UnifiedInputModel {

    private String accountNumber;
    private String companyInfoId;
    private String classification;
    private BigDecimal currentBalance;
    private String currency;
    private String description;
    private String name;
    private String parentAccountId;
    private String status;
    private String type;
    private Map<String, ?> customFields;

    protected AccountUnifiedInputModel() {
    }

    public AccountUnifiedInputModel(
        String accountNumber, String companyInfoId, String classification, BigDecimal currentBalance, String currency,
        String description, String name, String parentAccountId, String status, String type,
        Map<String, ?> customFields) {

        this.accountNumber = accountNumber;
        this.companyInfoId = companyInfoId;
        this.classification = classification;
        this.currentBalance = currentBalance;
        this.currency = currency;
        this.description = description;
        this.name = name;
        this.parentAccountId = parentAccountId;
        this.status = status;
        this.type = type;
        this.customFields = customFields;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCompanyInfoId() {
        return companyInfoId;
    }

    public String getClassification() {
        return classification;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getParentAccountId() {
        return parentAccountId;
    }

    public String getStatus() {
        return status;
    }

    public String getType() {
        return type;
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

        return Objects.equals(accountNumber, that.accountNumber) &&
            Objects.equals(companyInfoId, that.companyInfoId) && Objects.equals(classification, that.classification) &&
            Objects.equals(currentBalance, that.currentBalance) && Objects.equals(currency, that.currency) &&
            Objects.equals(description, that.description) && Objects.equals(name, that.name) &&
            Objects.equals(parentAccountId, that.parentAccountId) && Objects.equals(status, that.status) &&
            Objects.equals(type, that.type) && Objects.equals(customFields, that.customFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            accountNumber, companyInfoId, classification, currentBalance, currency, description, name, parentAccountId,
            status, type, customFields);
    }

    @Override
    public String toString() {
        return "AccountUnifiedInputModel{" +
            "accountNumber='" + accountNumber + '\'' +
            ", companyInfoId='" + companyInfoId + '\'' +
            ", classification='" + classification + '\'' +
            ", currentBalance=" + currentBalance +
            ", currency='" + currency + '\'' +
            ", description='" + description + '\'' +
            ", name='" + name + '\'' +
            ", parentAccountId='" + parentAccountId + '\'' +
            ", status='" + status + '\'' +
            ", type='" + type + '\'' +
            ", customFields=" + customFields +
            '}';
    }
}
