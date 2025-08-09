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

package com.bytechef.component.zendesk;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.zendesk.action.ZendeskCommentTicketAction;
import com.bytechef.component.zendesk.action.ZendeskCreateOrganizationAction;
import com.bytechef.component.zendesk.action.ZendeskCreateTicketAction;
import com.bytechef.component.zendesk.connection.ZendeskConnection;
import com.bytechef.component.zendesk.trigger.ZendeskNewTicketTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class ZendeskComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("zendesk")
        .title("Zendesk")
        .description(
            "Zendesk is a customer service and sales platform that helps businesses manage customer interactions" +
                " across various channels.")
        .icon("path:assets/zendesk.svg")
        .categories(ComponentCategory.SURVEYS_AND_FEEDBACK)
        .connection(ZendeskConnection.CONNECTION_DEFINITION)
        .customAction(true)
        .actions(
            ZendeskCommentTicketAction.ACTION_DEFINITION,
            ZendeskCreateOrganizationAction.ACTION_DEFINITION,
            ZendeskCreateTicketAction.ACTION_DEFINITION)
        .clusterElements(
            tool(ZendeskCommentTicketAction.ACTION_DEFINITION),
            tool(ZendeskCreateOrganizationAction.ACTION_DEFINITION),
            tool(ZendeskCreateTicketAction.ACTION_DEFINITION))
        .triggers(ZendeskNewTicketTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
