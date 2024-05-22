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

package com.bytechef.component.quickbooks;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.QUICKBOOKS;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.quickbooks.action.QuickbooksCreateCustomerAction;
import com.bytechef.component.quickbooks.action.QuickbooksCreateItemAction;
import com.bytechef.component.quickbooks.action.QuickbooksDownloadCustomerPdfAction;
import com.bytechef.component.quickbooks.connection.QuickbooksConnection;
import com.google.auto.service.AutoService;

/**
 * @author Mario Cvjetojevic
 */
@AutoService(ComponentHandler.class)
public class QuickbooksComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(QUICKBOOKS)
        .title("QuickBooks")
        .description(
            "QuickBooks is an accounting software package developed and marketed by Intuit. It is geared mainly " +
                "toward small and medium-sized businesses and offers on-premises accounting applications as well as " +
                "cloud-based versions that accept business payments, manage and pay bills, and payroll functions.")
        .icon("path:assets/quickbooks.svg")
        .connection(QuickbooksConnection.CONNECTION_DEFINITION)
        .categories(ComponentCategory.ACCOUNTING)
        .actions(
            QuickbooksCreateCustomerAction.ACTION_DEFINITION,
            QuickbooksCreateItemAction.ACTION_DEFINITION,
            QuickbooksDownloadCustomerPdfAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
