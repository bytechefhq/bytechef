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

package com.bytechef.component.freshdesk.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class FreshdeskCreateTicketAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createTicket")
        .title("Create ticket")
        .description("Creates a new ticket")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/tickets", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item").properties(string("subject").label("Subject")
            .description("Subject of the ticket.")
            .required(true),
            string("email").label("Email")
                .description(
                    "Email address of the requester. If no contact exists with this email address in Freshdesk, it will be added as a new contact.")
                .required(true),
            string("description").label("Description")
                .description("HTML content of the ticket.")
                .required(true),
            integer("priority").label("Priority")
                .description("Priority of the ticket.")
                .options(option("1", 1), option("2", 2), option("3", 3), option("4", 4))
                .required(false),
            integer("status").label("Status")
                .description("Status of the ticket.")
                .options(option("2", 2), option("3", 3), option("4", 4), option("5", 5))
                .required(false))
            .label("Ticket")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object()
            .properties(object("body")
                .properties(string("subject").required(false), string("email").required(false),
                    string("description").required(false), integer("priority").description("Priority of the ticket.")
                        .required(false),
                    integer("status").required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));
}
