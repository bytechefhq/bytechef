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

package com.bytechef.component.zendesk.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Nikolina Spehar
 */
public class ZendeskConstants {

    public static final String BODY = "body";
    public static final String COMMENT = "comment";
    public static final String DETAILS = "details";
    public static final String DOMAIN_NAMES = "domain_names";
    public static final String EMAIL = "email";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String NOTES = "notes";
    public static final String ORGANIZATION = "organization";
    public static final String PRIORITY = "priority";
    public static final String STATUS = "status";
    public static final String SUBDOMAIN = "subdomain";
    public static final String SUBJECT = "subject";
    public static final String TICKET = "ticket";
    public static final String TICKET_ID = "ticketId";
    public static final String TYPE = "type";
    public static final String WEBHOOK = "webhook";

    public static final ModifiableObjectProperty TICKET_OBJECT_PROPERTY =
        object()
            .properties(
                string("url")
                    .description("API URL of the ticket resource."),
                integer("id")
                    .description("Ticket ID."),
                integer("external_id")
                    .description("External ID of the ticket."),
                object("via")
                    .description("Information for how the ticket was created.")
                    .properties(
                        string("channel")
                            .description(
                                "Name of the channel through which the ticket was created (e.g., web, email)."),
                        object("source")
                            .description("Source from where the ticket was created.")
                            .properties(
                                object("from")
                                    .description("Email address of the sender."),
                                object("to")
                                    .description("Email address of the recipient."),
                                object("rel"))),
                string("created_at")
                    .description("Timestamp when the ticket was created."),
                string("updated_at")
                    .description("Timestamp of the last update to the ticket."),
                integer("generated_timestamp")
                    .description("UNIX timestamp when the ticket was generated."),
                string("type")
                    .description("Type of the ticket.")
                    .options(
                        option("Problem", "problem"),
                        option("Incident", "incident"),
                        option("Question", "question"),
                        option("Task", "task")),
                string("subject")
                    .description("Subject line of the ticket."),
                string("raw_subject")
                    .description("Unprocessed subject line of the ticket."),
                string("description")
                    .description("Detailed description of the ticket issue."),
                string("priority")
                    .description("Priority of the ticket.")
                    .options(
                        option("Low", "low"),
                        option("Normal", "normal"),
                        option("High", "high"),
                        option("Urgent", "urgent")),
                string("status")
                    .description("Current status of the ticket.")
                    .options(
                        option("New", "new"),
                        option("Open", "open"),
                        option("Pending", "pending"),
                        option("Hold", "hold"),
                        option("Solved", "solved"),
                        option("Closed", "closed")),
                string("recipient")
                    .description("Email address of the ticket recipient."),
                integer("requester_id")
                    .description("ID of the user who requested the ticket."),
                integer("submitter_id")
                    .description("ID of the user who submitted the ticket."),
                integer("assignee_id")
                    .description("ID of the agent assigned to the ticket."),
                integer("organization_id")
                    .description("ID of the organization associated with the requester."),
                integer("group_id")
                    .description("ID of the group associated with the ticket."),
                array("collaborator_ids")
                    .description("List of user IDs who are collaborators on the ticket.")
                    .items(
                        integer("id")
                            .description("List of user IDs who are collaborators on the ticket.")),
                array("follower_ids")
                    .description("List of user IDs following the ticket.")
                    .items(
                        integer("id")
                            .description("List of user IDs following the ticket.")),
                array("email_cc_ids")
                    .description("List of ticket CCs user IDs."),
                array("forum_topic_ids")
                    .description("List of forum ID topics."),
                integer("problem_id")
                    .description("ID of the problem the ticket is linked to."),
                bool("has_incidents")
                    .description("Whether the ticket has related incidents."),
                bool("is_public")
                    .description("Whether the ticket is public."),
                string("due_at")
                    .description("Due date for the ticket, if any."),
                array("tags")
                    .description("List of tags associated with the ticket.")
                    .items(
                        string()
                            .description("List of tags associated with the ticket.")),
                array("custom_fields")
                    .description("Custom field values associated with the ticket.")
                    .items(
                        object()
                            .properties(
                                integer("id")
                                    .description("ID of the custom field."),
                                string("value")
                                    .description("Value for the custom field."))
                            .description("Custom field values associated with the ticket.")),
                object("satisfaction_rating")
                    .properties(
                        string("comment")
                            .description("Comment left with the satisfaction rating."),
                        integer("id")
                            .description("ID of the satisfaction rating."),
                        string("score")
                            .description("Score given in the satisfaction rating.")
                            .options(
                                option("Good", "good"),
                                option("Bad", "bad")))
                    .description("Customer satisfaction rating for the ticket."),
                array("sharing_agreement_ids")
                    .items(
                        integer("id")
                            .description("List of sharing agreement IDs for the ticket."))
                    .description("List of sharing agreement IDs for the ticket."),
                integer("custom_status_id")
                    .description("Custom status ID for the ticket."),
                string("encoded_id")
                    .description("Encoded ticket ID."),
                array("fields")
                    .description("Ticket fields.")
                    .items(
                        object("item")
                            .properties(
                                integer("id")
                                    .description("Field ID."),
                                string("value")
                                    .description("Field value."))),
                array("followup_ids")
                    .description("Array of follow up IDs."),
                integer("ticket_form_id")
                    .description("Ticket form ID."),
                integer("brand_id")
                    .description("Brand ID."),
                bool("allow_channelback")
                    .description("Whether channelback is allowed for the ticket."),
                bool("allow_attachments")
                    .description("Whether attachments are allowed for the ticket."),
                bool("from_messaging_channel")
                    .description("Indicates if the ticket originated from a messaging channel."));

    private ZendeskConstants() {
    }
}
