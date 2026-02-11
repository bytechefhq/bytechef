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

package com.bytechef.component.hubspot;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.hubspot.action.HubspotCreateContactAction;
import com.bytechef.component.hubspot.action.HubspotCreateDealAction;
import com.bytechef.component.hubspot.action.HubspotDeleteContactAction;
import com.bytechef.component.hubspot.action.HubspotGetContactAction;
import com.bytechef.component.hubspot.action.HubspotGetTicketAction;
import com.bytechef.component.hubspot.action.HubspotUpdateContactAction;
import com.bytechef.component.hubspot.connection.HubspotConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractHubspotComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("hubspot")
            .title("Hubspot")
            .description(
                "HubSpot is a CRM platform with all the software, integrations, and resources you need to connect marketing, sales, content management, and customer service."))
                    .actions(modifyActions(HubspotGetContactAction.ACTION_DEFINITION,
                        HubspotCreateContactAction.ACTION_DEFINITION, HubspotDeleteContactAction.ACTION_DEFINITION,
                        HubspotUpdateContactAction.ACTION_DEFINITION, HubspotCreateDealAction.ACTION_DEFINITION,
                        HubspotGetTicketAction.ACTION_DEFINITION))
                    .connection(modifyConnection(HubspotConnection.CONNECTION_DEFINITION))
                    .clusterElements(modifyClusterElements(tool(HubspotGetContactAction.ACTION_DEFINITION),
                        tool(HubspotCreateContactAction.ACTION_DEFINITION),
                        tool(HubspotDeleteContactAction.ACTION_DEFINITION),
                        tool(HubspotUpdateContactAction.ACTION_DEFINITION),
                        tool(HubspotCreateDealAction.ACTION_DEFINITION),
                        tool(HubspotGetTicketAction.ACTION_DEFINITION)))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
