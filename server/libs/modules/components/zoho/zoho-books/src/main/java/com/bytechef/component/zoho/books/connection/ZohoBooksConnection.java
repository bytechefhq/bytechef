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

package com.bytechef.component.zoho.books.connection;

import static com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

import com.bytechef.component.zoho.commons.ZohoConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class ZohoBooksConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = ZohoConnection.createConnection(
        "/books/v3", createScopesMap(), true);

    private ZohoBooksConnection() {
    }

    private static Map<String, Boolean> createScopesMap() {
        Map<String, Boolean> map = new HashMap<>();

        map.put("ZohoBooks.contacts.CREATE", true);
        map.put("ZohoBooks.contacts.UPDATE", false);
        map.put("ZohoBooks.contacts.READ", true);
        map.put("ZohoBooks.contacts.DELETE", false);
        map.put("ZohoBooks.contacts.ALL", false);
        map.put("ZohoBooks.settings.CREATE", false);
        map.put("ZohoBooks.settings.UPDATE", false);
        map.put("ZohoBooks.settings.READ", true);
        map.put("ZohoBooks.settings.DELETE", false);
        map.put("ZohoBooks.settings.ALL", false);
        map.put("ZohoBooks.estimates.CREATE", false);
        map.put("ZohoBooks.estimates.UPDATE", false);
        map.put("ZohoBooks.estimates.READ", false);
        map.put("ZohoBooks.estimates.DELETE", false);
        map.put("ZohoBooks.estimates.ALL", false);
        map.put("ZohoBooks.invoices.CREATE", true);
        map.put("ZohoBooks.invoices.UPDATE", false);
        map.put("ZohoBooks.invoices.READ", false);
        map.put("ZohoBooks.invoices.DELETE", false);
        map.put("ZohoBooks.invoices.ALL", false);
        map.put("ZohoBooks.customerpayments.CREATE", false);
        map.put("ZohoBooks.customerpayments.UPDATE", false);
        map.put("ZohoBooks.customerpayments.READ", false);
        map.put("ZohoBooks.customerpayments.DELETE", false);
        map.put("ZohoBooks.customerpayments.ALL", false);
        map.put("ZohoBooks.creditnotes.CREATE", false);
        map.put("ZohoBooks.creditnotes.UPDATE", false);
        map.put("ZohoBooks.creditnotes.READ", false);
        map.put("ZohoBooks.creditnotes.DELETE", false);
        map.put("ZohoBooks.creditnotes.ALL", false);
        map.put("ZohoBooks.projects.CREATE", false);
        map.put("ZohoBooks.projects.UPDATE", false);
        map.put("ZohoBooks.projects.READ", false);
        map.put("ZohoBooks.projects.DELETE", false);
        map.put("ZohoBooks.projects.ALL", false);
        map.put("ZohoBooks.expenses.CREATE", false);
        map.put("ZohoBooks.expenses.UPDATE", false);
        map.put("ZohoBooks.expenses.READ", false);
        map.put("ZohoBooks.expenses.DELETE", false);
        map.put("ZohoBooks.expenses.ALL", false);
        map.put("ZohoBooks.salesorders.CREATE", true);
        map.put("ZohoBooks.salesorders.UPDATE", false);
        map.put("ZohoBooks.salesorders.READ", false);
        map.put("ZohoBooks.salesorders.DELETE", false);
        map.put("ZohoBooks.salesorders.ALL", false);
        map.put("ZohoBooks.purchaseorders.CREATE", false);
        map.put("ZohoBooks.purchaseorders.UPDATE", false);
        map.put("ZohoBooks.purchaseorders.READ", false);
        map.put("ZohoBooks.purchaseorders.DELETE", false);
        map.put("ZohoBooks.purchaseorders.ALL", false);
        map.put("ZohoBooks.bills.CREATE", false);
        map.put("ZohoBooks.bills.UPDATE", false);
        map.put("ZohoBooks.bills.READ", false);
        map.put("ZohoBooks.bills.DELETE", false);
        map.put("ZohoBooks.bills.ALL", false);
        map.put("ZohoBooks.debitnotes.CREATE", false);
        map.put("ZohoBooks.debitnotes.UPDATE", false);
        map.put("ZohoBooks.debitnotes.READ", false);
        map.put("ZohoBooks.debitnotes.DELETE", false);
        map.put("ZohoBooks.debitnotes.ALL", false);
        map.put("ZohoBooks.vendorpayments.CREATE", false);
        map.put("ZohoBooks.vendorpayments.UPDATE", false);
        map.put("ZohoBooks.vendorpayments.READ", false);
        map.put("ZohoBooks.vendorpayments.DELETE", false);
        map.put("ZohoBooks.vendorpayments.ALL", false);
        map.put("ZohoBooks.banking.CREATE", false);
        map.put("ZohoBooks.banking.UPDATE", false);
        map.put("ZohoBooks.banking.READ", false);
        map.put("ZohoBooks.banking.DELETE", false);
        map.put("ZohoBooks.banking.ALL", false);
        map.put("ZohoBooks.accountants.CREATE", false);
        map.put("ZohoBooks.accountants.UPDATE", false);
        map.put("ZohoBooks.accountants.READ", false);
        map.put("ZohoBooks.accountants.DELETE", false);
        map.put("ZohoBooks.accountants.ALL", false);

        return map;
    }
}
