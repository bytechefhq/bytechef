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

package com.bytechef.component.zoho.invoice.connection;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.zoho.commons.ZohoConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class ZohoInvoiceConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = ZohoConnection.createConnection(
        "/invoice/v3", createScopesMap(), true);

    private static Map<String, Boolean> createScopesMap() {
        Map<String, Boolean> map = new HashMap<>();

        map.put("ZohoInvoice.contacts.Create", true);
        map.put("ZohoInvoice.contacts.UPDATE", false);
        map.put("ZohoInvoice.contacts.READ", true);
        map.put("ZohoInvoice.contacts.DELETE", false);
        map.put("ZohoInvoice.settings.Create", true);
        map.put("ZohoInvoice.settings.UPDATE", false);
        map.put("ZohoInvoice.settings.READ", true);
        map.put("ZohoInvoice.settings.DELETE", false);
        map.put("ZohoInvoice.estimates.UPDATE", false);
        map.put("ZohoInvoice.estimates.READ", false);
        map.put("ZohoInvoice.estimates.DELETE", false);
        map.put("ZohoInvoice.invoices.Create", true);
        map.put("ZohoInvoice.invoices.UPDATE", false);
        map.put("ZohoInvoice.invoices.READ", false);
        map.put("ZohoInvoice.invoices.DELETE", false);
        map.put("ZohoInvoice.customerpayments.UPDATE", false);
        map.put("ZohoInvoice.customerpayments.READ", false);
        map.put("ZohoInvoice.customerpayments.DELETE", false);
        map.put("ZohoInvoice.creditnotes.Create", false);
        map.put("ZohoInvoice.creditnotes.UPDATE", false);
        map.put("ZohoInvoice.creditnotes.READ", false);
        map.put("ZohoInvoice.creditnotes.DELETE", false);
        map.put("ZohoInvoice.projects.Create", false);
        map.put("ZohoInvoice.projects.UPDATE", false);
        map.put("ZohoInvoice.projects.READ", false);
        map.put("ZohoInvoice.projects.DELETE", false);
        map.put("ZohoInvoice.expenses.Create", false);
        map.put("ZohoInvoice.expenses.UPDATE", false);
        map.put("ZohoInvoice.expenses.READ", false);
        map.put("ZohoInvoice.expenses.DELETE", false);

        return map;
    }

    private ZohoInvoiceConnection() {
    }
}
