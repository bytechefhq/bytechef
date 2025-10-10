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

package com.bytechef.component.pagerduty.trigger;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.ID;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.SERVICE;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.pagerduty.util.PagerDutyUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class PagerDutyNewOrUpdatedIncidentTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newOrUpdatedIncident")
        .title("New or Updated Incident Trigger")
        .description("Triggers incident is created or updated.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(SERVICE)
                .label("Service")
                .description("The service that will be watched for the trigger event.")
                .options((OptionsFunction<String>) PagerDutyUtils::getServiceIdOptions)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("id")
                            .description("ID of the incident that triggered the trigger."),
                        string("event_type")
                            .description("Type of the event that triggered the trigger."),
                        string("resource_type")
                            .description("Type of the resource that triggered the trigger."),
                        string("occurred_at")
                            .description("When did the event occurred."),
                        object("agent")
                            .description("Agent that triggered the event.")
                            .properties(
                                string("type")
                                    .description("A string that determines the schema of the object."),
                                string("id")
                                    .description("ID of the channel."),
                                string("summary")
                                    .description("A short description about the channel."),
                                string("self")
                                    .description("The API show URL at which the object is accessible."),
                                string("html_url")
                                    .description(
                                        "A URL at which the entity is uniquely displayed in the Web app.")),
                        object("client")
                            .description("Client on which event occurred."),
                        object("data")
                            .description("Data of the object that triggered the trigger.")
                            .properties(
                                string("id")
                                    .description("ID of the object."),
                                string("type")
                                    .description("A string that determines the schema of the object."),
                                string("self")
                                    .description("The API show URL at which the object is accessible."),
                                string("html_url")
                                    .description("A URL at which the entity is uniquely displayed in the Web app."),
                                string("number")
                                    .description("Number of the incident."),
                                string("status")
                                    .description("Status of the incident."),
                                string("incident_key")
                                    .description("Incident key."),
                                string("created_at")
                                    .description("Date when the incident was created."),
                                string("title")
                                    .description("Title of the incident."),
                                object("service")
                                    .description("The user who created an incident note.")
                                    .properties(
                                        string("id")
                                            .description("ID of the object."),
                                        string("type")
                                            .description("A string that determines the schema of the object."),
                                        string("summary")
                                            .description("A short description about the service."),
                                        string("self")
                                            .description("The API show URL at which the object is accessible."),
                                        string("html_url")
                                            .description(
                                                "A URL at which the entity is uniquely displayed in the Web app.")),
                                array("assignees")
                                    .description("Assignees of the incident.")
                                    .items(
                                        object()
                                            .properties(
                                                string("at")
                                                    .description(
                                                        "Date when the incident was assigned to the assignee."),
                                                object("assignee")
                                                    .description("Assignee of the incident.")
                                                    .properties(
                                                        string("id")
                                                            .description("ID of the object."),
                                                        string("type")
                                                            .description(
                                                                "A string that determines the schema of the object."),
                                                        string("summary")
                                                            .description("A short description about the assignee."),
                                                        string("self")
                                                            .description(
                                                                "The API show URL at which the object is accessible."),
                                                        string("html_url")
                                                            .description(
                                                                "A URL at which the entity is uniquely displayed in the Web app.")))),
                                object("escalation_policy")
                                    .description("Escalation Policy.")
                                    .properties(
                                        string("id")
                                            .description("ID of the object."),
                                        string("type")
                                            .description("A string that determines the schema of the object."),
                                        string("summary")
                                            .description("A short description about the escalation policy."),
                                        string("self")
                                            .description("The API show URL at which the object is accessible."),
                                        string("html_url")
                                            .description(
                                                "A URL at which the entity is uniquely displayed in the Web app.")),
                                array("teams")
                                    .description("Teams that are associated with the incident."),
                                string("priority")
                                    .description("Priority of the incident."),
                                string("urgency")
                                    .description("Urgency of the incident."),
                                string("conference_bridge")
                                    .description("Conference bridge of the incident."),
                                string("resolve_reason")
                                    .description("Reason the incident was resolved."),
                                object("incident_type")
                                    .description("Incident type.")
                                    .properties(
                                        string("name")
                                            .description("Name of the incident type."))))))
        .webhookEnable(PagerDutyNewOrUpdatedIncidentTrigger::webhookEnable)
        .webhookDisable(PagerDutyNewOrUpdatedIncidentTrigger::webhookDisable)
        .webhookRequest(PagerDutyNewOrUpdatedIncidentTrigger::webhookRequest);

    private PagerDutyNewOrUpdatedIncidentTrigger() {
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        context.http(http -> http.delete("/webhook_subscriptions/%s".formatted(outputParameters.get(ID))))
            .execute();
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        Map<String, Map<String, Object>> body = context.http(http -> http.post("/webhook_subscriptions"))
            .body(
                Http.Body.of(
                    "webhook_subscription", Map.of(
                        "delivery_method", Map.of("type", "http_delivery_method", "url", webhookUrl),
                        "events", List.of(
                            "incident.acknowledged",
                            "incident.annotated",
                            "incident.delegated",
                            "incident.escalated",
                            "incident.priority_updated",
                            "incident.reassigned",
                            "incident.reopened",
                            "incident.resolved",
                            "incident.service_updated",
                            "incident.triggered",
                            "incident.unacknowledged"),
                        "filter", Map.of(
                            "id", inputParameters.getRequiredString(SERVICE),
                            "type", "service_reference"),
                        "type", "webhook_subscription")))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        Map<String, Object> webhookSubscription = body.get("webhook_subscription");

        return new WebhookEnableOutput(Map.of(ID, (String) webhookSubscription.get("id")), null);
    }

    protected static Map<String, Object> webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, WebhookEnableOutput output, TriggerContext context) {

        Map<String, Map<String, Object>> content = body.getContent(new TypeReference<>() {});

        return content.get("event");
    }
}
