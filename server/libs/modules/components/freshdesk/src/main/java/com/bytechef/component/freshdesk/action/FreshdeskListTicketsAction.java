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

package com.bytechef.component.freshdesk.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
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
public class FreshdeskListTicketsAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("listTickets")
        .title("List Tickets")
        .description(
            "Lists tickets. By default, only tickets created within the past 30 days are returned; use Updated Since to return older tickets.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/tickets"

            ))
        .properties(string("filter").label("Filter")
            .description("Predefined filter to apply to the tickets.")
            .options(option("New_and_my_open", "new_and_my_open"), option("Watching", "watching"),
                option("Spam", "spam"), option("Deleted", "deleted"))
            .required(false)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)),
            string("updated_since").label("Updated Since")
                .description(
                    "Only return tickets updated since this date or timestamp, in UTC, for example 2015-01-19T02:00:00Z or 2015-08-17.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("order_by").label("Order By")
                .description("Ticket field to sort the tickets by.")
                .options(option("Created_at", "created_at"), option("Due_by", "due_by"),
                    option("Updated_at", "updated_at"), option("Status", "status"))
                .defaultValue("created_at")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("order_type").label("Order Type")
                .description("Sort direction of the tickets.")
                .options(option("Asc", "asc"), option("Desc", "desc"))
                .defaultValue("desc")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .output(
            outputSchema(
                array()
                    .items(object()
                        .properties(
                            array("cc_emails")
                                .items(string()
                                    .description("List of email addresses added in the cc field of the ticket."))
                                .description("List of email addresses added in the cc field of the ticket.")
                                .required(false),
                            array("fwd_emails")
                                .items(string().description("List of email addresses added while forwarding a ticket."))
                                .description("List of email addresses added while forwarding a ticket.")
                                .required(false),
                            array("reply_cc_emails").items(
                                string().description("List of email addresses added while replying to a ticket."))
                                .description("List of email addresses added while replying to a ticket.")
                                .required(false),
                            integer("email_config_id").description("ID of the email config used for the ticket.")
                                .required(false),
                            integer("group_id").description("ID of the group the ticket is assigned to.")
                                .required(false),
                            integer("priority").description("Priority of the ticket.")
                                .required(false),
                            integer("requester_id").description("ID of the requester of the ticket.")
                                .required(false),
                            integer("responder_id").description("ID of the agent the ticket is assigned to.")
                                .required(false),
                            integer("source").description("Channel through which the ticket was created.")
                                .required(false),
                            integer("status").description("Status of the ticket.")
                                .required(false),
                            string("subject").description("Subject of the ticket.")
                                .required(false),
                            integer("company_id").description("ID of the company the ticket belongs to.")
                                .required(false),
                            integer("id").description("ID of the ticket.")
                                .required(false),
                            string("type").description("Type of the ticket.")
                                .required(false),
                            array("to_emails")
                                .items(string().description("List of email addresses the ticket was sent to."))
                                .description("List of email addresses the ticket was sent to.")
                                .required(false),
                            integer("product_id").description("ID of the product the ticket belongs to.")
                                .required(false),
                            bool("fr_escalated").description(
                                "Whether the ticket has been escalated as the result of first response time being breached.")
                                .required(false),
                            bool("spam").description("Whether the ticket has been marked as spam.")
                                .required(false),
                            bool("urgent").description("Whether the ticket is marked as urgent.")
                                .required(false),
                            bool("is_escalated").description("Whether the ticket has been escalated.")
                                .required(false),
                            string("created_at").description("Timestamp when the ticket was created.")
                                .required(false),
                            string("updated_at").description("Timestamp when the ticket was last updated.")
                                .required(false),
                            string("due_by").description("Timestamp when the ticket is due to be resolved.")
                                .required(false),
                            string("fr_due_by").description("Timestamp when the first response is due.")
                                .required(false),
                            string("description_text").description("Plain text version of the ticket description.")
                                .required(false),
                            string("description").description("HTML content of the ticket description.")
                                .required(false),
                            array("tags").items(string().description("List of tags associated with the ticket."))
                                .description("List of tags associated with the ticket.")
                                .required(false),
                            array("attachments")
                                .items(object().description("List of attachments associated with the ticket."))
                                .description("List of attachments associated with the ticket.")
                                .required(false))
                        .description("List of tickets."))
                    .description("List of tickets.")
                    .metadata(
                        Map.of(
                            "responseType", ResponseType.JSON))))
        .help("", "https://docs.bytechef.io/reference/components/freshdesk_v1#list-tickets");

    private FreshdeskListTicketsAction() {
    }
}
