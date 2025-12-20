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

package com.bytechef.component.mailerlite.trigger;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.DATA;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.EMAIL;
import static com.bytechef.component.mailerlite.constant.MailerLiteConstants.ID;
import static com.bytechef.component.mailerlite.util.MailerLiteUtils.getContent;
import static com.bytechef.component.mailerlite.util.MailerLiteUtils.subscribeWebhook;
import static com.bytechef.component.mailerlite.util.MailerLiteUtils.unsubscribeWebhook;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class MailerLiteSubscriberUnsubscribedTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("subscriberUnsubscribed")
        .title("Subscriber Unsubscribed")
        .description("Triggers when a subscriber unsubscribes.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties()
        .output(
            outputSchema(
                object()
                    .properties(
                        object(DATA)
                            .properties(
                                string("account_id")
                                    .description("ID of the account that subscriber subscribed to."),
                                string("id")
                                    .description("ID of the user that was created or updated."),
                                string(EMAIL)
                                    .description("The email address of the subscriber."),
                                string("status"),
                                string("source"),
                                integer("sent")
                                    .description("The number of messages that were sent to the subscriber."),
                                integer("opens_count")
                                    .description("Number of email messages the user has opened."),
                                integer("clicks_count"),
                                integer("open_rate"),
                                integer("click_rate"),
                                string("subscribed_at"),
                                string("created_at"),
                                string("updated_at"),
                                array("fields"),
                                array("groups")))))
        .webhookEnable(MailerLiteSubscriberUnsubscribedTrigger::webhookEnable)
        .webhookDisable(MailerLiteSubscriberUnsubscribedTrigger::webhookDisable)
        .webhookRequest(MailerLiteSubscriberUnsubscribedTrigger::webhookRequest);

    private MailerLiteSubscriberUnsubscribedTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        return new WebhookEnableOutput(
            Map.of(ID, subscribeWebhook(TRIGGER_DEFINITION.getName(), "subscriber.unsubscribed", webhookUrl, context)),
            null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        unsubscribeWebhook(outputParameters.getString(ID), context);
    }

    protected static Map<String, Object> webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        return getContent(body);
    }
}
