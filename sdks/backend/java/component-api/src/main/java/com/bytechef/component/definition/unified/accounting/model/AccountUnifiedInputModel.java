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

import com.bytechef.component.definition.unified.base.model.UnifiedInputModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the normalized, provider-agnostic input shape for an accounting account. Instances carry the fields that
 * ByteChef exposes uniformly across accounting providers when creating or updating an account, together with a map of
 * provider-specific custom fields.
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

    /**
     * Creates a fully populated unified account input model.
     *
     * @param accountNumber   the account number assigned within the accounting system
     * @param companyInfoId   the identifier of the company the account belongs to
     * @param classification  the account classification (for example asset, liability, income)
     * @param currentBalance  the current balance of the account
     * @param currency        the ISO currency code of the account
     * @param description     a human-readable description of the account
     * @param name            the display name of the account
     * @param parentAccountId the identifier of the parent account, when the account is nested
     * @param status          the account status (for example active or archived)
     * @param type            the account type as defined by the accounting system
     * @param customFields    provider-specific custom fields keyed by field name
     */
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

    /**
     * Returns the account number assigned within the accounting system.
     *
     * @return the account number
     */
    public String getAccountNumber() {
        return accountNumber;
    }

    /**
     * Returns the identifier of the company the account belongs to.
     *
     * @return the company info identifier
     */
    public String getCompanyInfoId() {
        return companyInfoId;
    }

    /**
     * Returns the account classification (for example asset, liability, income).
     *
     * @return the account classification
     */
    public String getClassification() {
        return classification;
    }

    /**
     * Returns the current balance of the account.
     *
     * @return the current balance
     */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    /**
     * Returns the ISO currency code of the account.
     *
     * @return the currency code
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Returns the human-readable description of the account.
     *
     * @return the account description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the display name of the account.
     *
     * @return the account name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the identifier of the parent account, when the account is nested.
     *
     * @return the parent account identifier
     */
    public String getParentAccountId() {
        return parentAccountId;
    }

    /**
     * Returns the account status (for example active or archived).
     *
     * @return the account status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Returns the account type as defined by the accounting system.
     *
     * @return the account type
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the provider-specific custom fields keyed by field name.
     *
     * @return the custom fields
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
