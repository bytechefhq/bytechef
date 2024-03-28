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

package com.bytechef.component.xero;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.xero.constant.XeroConstants.XERO;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.xero.action.XeroCreateContactAction;
import com.bytechef.component.xero.action.XeroCreateInvoiceAction;
import com.bytechef.component.xero.connection.XeroConnection;
import com.google.auto.service.AutoService;

/**
 * @author Mario Cvjetojevic
 */
@AutoService(ComponentHandler.class)
public class XeroComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(XERO)
        .title("Xero")
        .description(
            "Xero is an online accounting software platform designed for small businesses and accountants to manage " +
                "finances efficiently.")
        .icon("path:assets/xero.svg")
        .connection(XeroConnection.CONNECTION_DEFINITION)
        .actions(
            XeroCreateContactAction.ACTION_DEFINITION,
            XeroCreateInvoiceAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
