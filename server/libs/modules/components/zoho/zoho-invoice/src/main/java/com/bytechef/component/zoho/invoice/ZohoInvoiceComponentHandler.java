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

package com.bytechef.component.zoho.invoice;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.zoho.invoice.action.ZohoInvoiceCreateContactAction;
import com.bytechef.component.zoho.invoice.action.ZohoInvoiceCreateInvoiceAction;
import com.bytechef.component.zoho.invoice.action.ZohoInvoiceCreateItemAction;
import com.bytechef.component.zoho.invoice.connection.ZohoInvoiceConnection;
import com.google.auto.service.AutoService;

/**
 * @author Marija Horvat
 */
@AutoService(ComponentHandler.class)
public class ZohoInvoiceComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("zohoInvoice")
        .title("Zoho Invoice")
        .description(
            "Zoho Invoice is an online invoicing software used to create, send, and manage professional invoices, " +
                "along with tracking payments and automating billing workflows.")
        .icon("path:assets/zoho-invoice.svg")
        .categories(ComponentCategory.ACCOUNTING)
        .connection(ZohoInvoiceConnection.CONNECTION_DEFINITION)
        .actions(
            ZohoInvoiceCreateContactAction.ACTION_DEFINITION,
            ZohoInvoiceCreateInvoiceAction.ACTION_DEFINITION,
            ZohoInvoiceCreateItemAction.ACTION_DEFINITION)
        .clusterElements(
            tool(ZohoInvoiceCreateContactAction.ACTION_DEFINITION),
            tool(ZohoInvoiceCreateInvoiceAction.ACTION_DEFINITION),
            tool(ZohoInvoiceCreateItemAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
