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

package com.bytechef.component.definition.unified.accounting;

import com.bytechef.component.definition.UnifiedApiDefinition;

/**
 * Enumerates the unified model types exposed by the accounting category of the unified API, spanning ledger entities,
 * financial statements, and transactional records that accounting providers have in common.
 *
 * @author Ivica Cardic
 */
public enum AccountingModelType implements UnifiedApiDefinition.ModelType {

    /** A ledger account in the chart of accounts. */
    ACCOUNT,
    /** A postal address associated with a contact or company. */
    ADDRESS,
    /** A file attached to another accounting record. */
    ATTACHMENT,
    /** A balance sheet financial statement. */
    BALANCE_SHEET,
    /** A cash flow financial statement. */
    CASH_FLOW_STATEMENT,
    /** General information about the accounting company or organization. */
    COMPANY_INFO,
    /** A customer or vendor contact. */
    CONTACT,
    /** A credit note issued to a customer. */
    CREDIT_NOTE,
    /** A recorded business expense. */
    EXPENSE,
    /** An income (profit and loss) financial statement. */
    INCOME_STATEMENT,
    /** An invoice issued to a customer. */
    INVOICE,
    /** A product or service line item. */
    ITEM,
    /** A double-entry journal entry. */
    JOURNAL_ENTRY,
    /** A payment received or made. */
    PAYMENT,
    /** A telephone number associated with a contact or company. */
    PHONE_NUMBER,
    /** A purchase order issued to a vendor. */
    PURCHASE_ORDER,
    /** A tax rate applied to transactions. */
    TAX_RATE,
    /** A tracking category used to classify transactions (e.g., department or location). */
    TRACKING_CATEGORY,
    /** A generic financial transaction. */
    TRANSACTION,
    /** A credit issued by a vendor. */
    VENDOR_CREDIT
}
