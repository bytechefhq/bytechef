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

package com.bytechef.component.freshdesk;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.freshdesk.action.FreshdeskCreateCompanyAction;
import com.bytechef.component.freshdesk.action.FreshdeskCreateContactAction;
import com.bytechef.component.freshdesk.action.FreshdeskCreateTicketAction;
import com.bytechef.component.freshdesk.connection.FreshdeskConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractFreshdeskComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("freshdesk")
            .title("Freshdesk")
            .description(
                "Freshdesk is a cloud-based customer support software that helps businesses manage customer queries and tickets efficiently."))
                    .actions(modifyActions(FreshdeskCreateCompanyAction.ACTION_DEFINITION,
                        FreshdeskCreateContactAction.ACTION_DEFINITION, FreshdeskCreateTicketAction.ACTION_DEFINITION))
                    .connection(modifyConnection(FreshdeskConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
