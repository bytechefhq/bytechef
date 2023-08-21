
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.hubspot.trigger;

import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookDisableContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.EnableDynamicWebhookContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookRequestContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookOutput;
import com.bytechef.hermes.component.util.HttpClientUtils;
import com.bytechef.hermes.component.util.MapUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.Map;

import static com.bytechef.hermes.component.util.HttpClientUtils.responseType;
import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.option;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class HubspotSubscribeTrigger {

    private static final String APP_ID = "appId";
    private static final String EVENT_TYPE = "eventType";
    private static final String PROPERTY_NAME = "propertyName";
    private static final String SUBSCRIBE = "subscribe";

    public static final ComponentDSL.ModifiableTriggerDefinition TRIGGER_DEFINITION = ComponentDSL.trigger(SUBSCRIBE)
        .title("Subscribe")
        .description("Triggers when an event of the subscribed type happens inside HubSpot.")
        .type(TriggerDefinition.TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(APP_ID)
                .label("App Id")
                .description(
                    "The id of a Hubspot app used to register this trigger to. See the <a href=\"https://legacydocs.hubspot.com/docs/faq/integration-platform-api-requirements\">prerequisites documentation</a> for more details about creating an app")
                .required(true),
            string(EVENT_TYPE)
                .options(
                    option("contact.creation", "Contact Creation"),
                    option("contact.deletion", "Contact Deletion"),
                    option("contact.merge", "Contact Merge"),
                    option("contact.associationChange", "Contact Association Change"),
                    option("contact.restore", "Contact Restore"),
                    option("contact.privacyDeletion", "Contact  Privacy Deletion"),
                    option("contact.propertyChange", "Contact Property Change"),
                    option("company.creation", "Contact Creation"),
                    option("company.deletion", "Company Deletion"),
                    option("company.propertyChange", "Company Property Change"),
                    option("company.associationChange", "Company Association Change"),
                    option("company.restore", "Company Restore"),
                    option("company.merge", "Company Merge"),
                    option("deal.creation", "Deal Creation"),
                    option("deal.deletion", "Deal Deletion"),
                    option("deal.associationChange", "Deal Association Change"),
                    option("deal.restore", "Deal Restore"),
                    option("deal.merge", "Deal Merge"),
                    option("deal.propertyChange", "Deal Property Change"),
                    option("ticket.creation", "Ticket Creation"),
                    option("ticket.deletion", "Ticket Deletion"),
                    option("ticket.propertyChange", "Ticket Property Change"),
                    option("ticket.associationChange", "Ticket Association Change"),
                    option("ticket.restore", "Ticket Restore"),
                    option("ticket.merge", "Ticket Merge"),
                    option("product.creation", "Product Creation"),
                    option("product.deletion", "Product Deletion"),
                    option("product.restore", "Product Restore"),
                    option("product.merge", "Product Merge"),
                    option("product.propertyChange", "Product Property Change"),
                    option("line_item.creation", "Line Item Creation"),
                    option("line_item.deletion", "Line Item Deletion"),
                    option("line_item.associationChange", "Line Item Association Change"),
                    option("line_item.restore", "Line Item Restore"),
                    option("line_item.merge", "Line Item Merge"),
                    option("line_item.propertyChange", "Line Item Property Change"),
                    option("conversation.creation", "Conversation Creation"),
                    option("conversation.deletion", "Conversation Deletion"),
                    option("conversation.privacyDeletion", "Conversation Privacy Deletion"),
                    option("conversation.propertyChange", "Conversation Property Change"),
                    option("conversation.newMessage", "Conversation New Message"))
                .label("Event Type")
                .description("The list of available event types for which you want to receive events.")
                .required(true),
            string(PROPERTY_NAME)
                .label("Property Name")
                .description("The name of property to listen for change events.")
                .displayCondition("eventType.includes('.propertyChange')")
                .required(true))
        .outputSchema(
            array().items(
                object()
                    .properties(
                        object("data").properties(
                            integer("objectId"),
                            string("propertyName"),
                            string("propertyValue"),
                            string("changeSource"),
                            integer("eventId"),
                            integer("subscriptionId"),
                            integer("portalId"),
                            integer("appId"),
                            integer("occurredAt"),
                            string("eventType"),
                            integer("attemptNumber"),
                            integer("messageId"),
                            string("messageType")))))
        .sampleOutput("""
            [
                {
                    "objectId": 1246965,
                    "propertyName": "lifecyclestage",
                    "propertyValue": "subscriber",
                    "changeSource": "ACADEMY",
                    "eventId": 3816279340,
                    "subscriptionId": 25,
                    "portalId": 33,
                    "appId": 1160452,
                    "occurredAt": 1462216307945,
                    "eventType":"contact.propertyChange",
                    "attemptNumber": 0
               }
            ]
            """)
        .dynamicWebhookDisable(HubspotSubscribeTrigger::dynamicWebhookDisable)
        .dynamicWebhookEnable(HubspotSubscribeTrigger::dynamicWebhookEnable)
        .dynamicWebhookRequest(HubspotSubscribeTrigger::dynamicWebhookRequest);

    protected static void dynamicWebhookDisable(DynamicWebhookDisableContext context) {
        HttpClientUtils
            .delete("/webhooks/v3/%s/settings".formatted(MapUtils.getString(context.inputParameters(), APP_ID)))
            .execute();
    }

    @SuppressWarnings("unchecked")
    protected static WebhookOutput dynamicWebhookRequest(DynamicWebhookRequestContext context) {
        WebhookBody webhookBody = context.body();

        return WebhookOutput.list((List<Map<?, ?>>) webhookBody.content());
    }

    @SuppressFBWarnings("RV")
    protected static DynamicWebhookEnableOutput dynamicWebhookEnable(EnableDynamicWebhookContext context) {
        HttpClientUtils
            .put("/webhooks/v3/%s/settings".formatted(MapUtils.getString(context.inputParameters(), APP_ID)))
            .body(HttpClientUtils.Body.of(
                Map.of(
                    "throttling", Map.of(
                        "period", "SECONDLY",
                        "maxConcurrentRequests", 10),
                    "targetUrl", context.webhookUrl())))
            .configuration(responseType(HttpClientUtils.ResponseType.JSON))
            .execute()
            .body();

        HttpClientUtils
            .put("/webhooks/v3/%s/subscriptions".formatted(MapUtils.getString(context.inputParameters(), APP_ID)))
            .body(HttpClientUtils.Body.of(
                Map.of(
                    "eventType", MapUtils.getString(context.inputParameters(), EVENT_TYPE),
                    "propertyName", MapUtils.getString(context.inputParameters(), PROPERTY_NAME, ""),
                    "active", true)))
            .configuration(responseType(HttpClientUtils.ResponseType.JSON))
            .execute()
            .body();

        return null;
    }
}
