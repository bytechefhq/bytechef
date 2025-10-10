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

package com.bytechef.component.hubspot.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.hubspot.util.HubspotUtils;
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
            .options((ActionDefinition.OptionsFunction<String>) HubspotUtils::getTicketIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .output(outputSchema(object().properties(string("id").description("ID of the ticket")
            .required(false),
            object("properties").properties(
                string("content").description("The main content or body of the ticket, detailing the issue or request.")
                    .required(false),
                string("hs_object_id").description("ID for the HubSpot object associated with the ticket.")
                    .required(false),
                string("hs_pipeline")
                    .description("The pipeline to which the ticket belongs, indicating its workflow stage.")
                    .required(false),
                string("hs_pipeline_stage")
                    .description("The current stage of the ticket within its pipeline, reflecting its progress.")
                    .required(false),
                string("hs_ticket_priority")
                    .description("The priority level assigned to the ticket, such as high, medium, or low.")
                    .required(false),
                string("subject").description("The subject or title of the ticket, summarizing the issue or request.")
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private HubspotGetTicketAction() {
    }
}
