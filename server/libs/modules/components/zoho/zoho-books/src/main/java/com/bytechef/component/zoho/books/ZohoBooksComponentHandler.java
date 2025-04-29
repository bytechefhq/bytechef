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

package com.bytechef.component.zoho.books;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.zoho.books.action.ZohoBooksCreateContactAction;
import com.bytechef.component.zoho.books.action.ZohoBooksCreateInvoiceAction;
import com.bytechef.component.zoho.books.action.ZohoBooksCreateSalesOrderAction;
import com.bytechef.component.zoho.books.connection.ZohoBooksConnection;
import com.google.auto.service.AutoService;

/**
 * @author Marija Horvat
 */
@AutoService(ComponentHandler.class)
public class ZohoBooksComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("zohoBooks")
        .title("Zoho Books")
        .description(
            "Zoho Books is cloud-based accounting software for managing your accounting tasks and organizing " +
                "your transactions.")
        .icon("path:assets/zoho-books.svg")
        .categories(ComponentCategory.ACCOUNTING)
        .connection(ZohoBooksConnection.CONNECTION_DEFINITION)
        .actions(
            ZohoBooksCreateContactAction.ACTION_DEFINITION,
            ZohoBooksCreateInvoiceAction.ACTION_DEFINITION,
            ZohoBooksCreateSalesOrderAction.ACTION_DEFINITION)
        .clusterElements(
            tool(ZohoBooksCreateContactAction.ACTION_DEFINITION),
            tool(ZohoBooksCreateInvoiceAction.ACTION_DEFINITION),
            tool(ZohoBooksCreateSalesOrderAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
