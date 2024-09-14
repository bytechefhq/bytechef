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

package com.bytechef.component.salesflare;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.salesflare.action.SalesflareCreateAccountAction;
import com.bytechef.component.salesflare.action.SalesflareCreateContactsAction;
import com.bytechef.component.salesflare.action.SalesflareCreateTasksAction;
import com.bytechef.component.salesflare.connection.SalesflareConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractSalesflareComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("salesflare")
            .title("Salesflare")
            .description(
                "Salesflare is a CRM software designed to help small businesses and startups manage their customer relationships efficiently."))
                    .actions(modifyActions(SalesflareCreateAccountAction.ACTION_DEFINITION,
                        SalesflareCreateContactsAction.ACTION_DEFINITION,
                        SalesflareCreateTasksAction.ACTION_DEFINITION))
                    .connection(modifyConnection(SalesflareConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
