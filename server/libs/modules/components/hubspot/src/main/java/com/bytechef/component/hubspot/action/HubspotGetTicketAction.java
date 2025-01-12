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

package com.bytechef.component.hubspot.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class HubspotGetTicketAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getTicket")
        .title("Get Ticket")
        .description("Gets ticket details.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/crm/v3/objects/tickets/{ticketId}"

            ))
        .properties(string("ticketId").label("Ticket ID")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .output(outputSchema(object()
            .properties(object("body")
                .properties(string("id").required(false),
                    object("properties")
                        .properties(string("content").required(false), string("hs_object_id").required(false),
                            string("hs_pipeline").required(false), string("hs_pipeline_stage").required(false),
                            string("hs_ticket_priority").required(false), string("subject").required(false))
                        .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private HubspotGetTicketAction() {
    }
}
