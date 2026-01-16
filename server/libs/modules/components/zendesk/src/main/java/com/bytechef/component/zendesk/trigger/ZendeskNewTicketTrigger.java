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

package com.bytechef.component.zendesk.trigger;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.ID;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.NAME;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.WEBHOOK;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class ZendeskNewTicketTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newTicket")
        .title("New Ticket")
        .description("Triggers when a new ticket is submitted.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(NAME)
                .label("Webhook Name")
                .description("Name of the webhook.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("actor_id")
                            .description("ID of the actor that triggered the webhook."),
                        string("assignee_id")
                            .description("ID of the agent assigned to the ticket."),
                        string("brand_id")
                            .description("Brand ID."),
                        string("created_at")
                            .description("Timestamp when the ticket was created."),
                        integer("custom_status")
                            .description("Custom status of the ticket."),
                        string("description")
                            .description("Detailed description of the ticket issue."),
                        string("external_id")
                            .description("External ID of the ticket."),
                        string("form_id")
                            .description("Form ID."),
                        string("group_id")
                            .description("ID of the group associated with the ticket."),
                        string("id")
                            .description("Ticket ID."),
                        bool("is_public")
                            .description("Whether the ticket is public."),
                        string("organization_id")
                            .description("ID of the organization associated with the requester."),
                        string("priority")
                            .description("Priority of the ticket."),
                        string("requester_id")
                            .description("ID of the user who requested the ticket."),
                        string("status")
                            .description("Current status of the ticket."),
                        string("subject")
                            .description("Subject line of the ticket."),
                        string("submitter_id")
                            .description("ID of the user who submitted the ticket."),
                        array("tags")
                            .description("List of tags associated with the ticket.")
                            .items(
                                string()
                                    .description("List of tags associated with the ticket.")),
                        string("type")
                            .description("Type of the ticket."),
                        string("updated_at")
                            .description("Timestamp of the last update to the ticket."),
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
                                        object("rel"))))))
        .webhookEnable(ZendeskNewTicketTrigger::webhookEnable)
        .webhookDisable(ZendeskNewTicketTrigger::webhookDisable)
        .webhookRequest(ZendeskNewTicketTrigger::webhookRequest);

    private ZendeskNewTicketTrigger() {
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        context.http(http -> http.delete("/webhooks/%s".formatted(outputParameters.getRequiredString(ID))))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute();
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
        TriggerContext context) {

        Map<String, Map<String, Object>> response = context.http(http -> http.post("/webhooks"))
            .configuration(responseType(ResponseType.JSON))
            .body(
                Body.of(
                    WEBHOOK, Map.of(
                        NAME, inputParameters.getRequiredString(NAME),
                        "status", "active",
                        "endpoint", webhookUrl,
                        "http_method", "POST",
                        "request_format", "json",
                        "subscriptions", List.of("zen:event-type:ticket.created"))))
            .execute()
            .getBody(new TypeReference<>() {});

        Map<String, Object> webhook = response.get(WEBHOOK);
        String webhookId = (String) webhook.get(ID);

        return new WebhookEnableOutput(Map.of(ID, webhookId), null);
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        Map<String, Object> content = body.getContent(new TypeReference<>() {});

        return content.get("detail");
    }
}
