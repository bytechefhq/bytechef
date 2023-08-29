
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

package com.bytechef.component.mailchimp.trigger;

import com.bytechef.component.mailchimp.util.MailchimpUtils;
import com.bytechef.hermes.component.definition.Context.Connection;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookDisableContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.EnableDynamicWebhookContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookRequestContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookOutput;
import com.bytechef.hermes.component.util.HttpClientUtils;
import com.bytechef.hermes.component.util.HttpClientUtils.Body;
import com.bytechef.hermes.component.util.MapUtils;

import java.util.Map;

import static com.bytechef.hermes.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.hermes.component.util.HttpClientUtils.responseType;
import static com.bytechef.hermes.definition.DefinitionDSL.dateTime;

import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class MailchimpSubscribeTrigger {

    private static final String LIST_ID = "listId";
    private static final String SUBSCRIBE = "subscribe";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = ComponentDSL.trigger(SUBSCRIBE)
        .title("Subscribe")
        .description("Triggers when an Audience subscriber is added to the list.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(LIST_ID)
                .options(MailchimpUtils.getListIdOptions())
                .label("List Id")
                .description("The list id of intended audience to which you would like to add the contact."))
        .outputSchema(
            object()
                .properties(
                    object("data").properties(
                        string("email"),
                        string("email_type"),
                        string("id"),
                        string("ip_opt"),
                        string("ip_signup"),
                        string("list_id"),
                        object("merges").properties(
                            string("EMAIL"),
                            string("FNAME"),
                            string("INTERESTS"),
                            string("LNAME"))),
                    dateTime("fired_at"),
                    string("type")))
        .dynamicWebhookDisable(MailchimpSubscribeTrigger::dynamicWebhookDisable)
        .dynamicWebhookEnable(MailchimpSubscribeTrigger::dynamicWebhookEnable)
        .dynamicWebhookRequest(MailchimpSubscribeTrigger::dynamicWebhookRequest);

    protected static void dynamicWebhookDisable(DynamicWebhookDisableContext context) {
        DynamicWebhookEnableOutput dynamicWebhookEnableOutput = context.dynamicWebhookEnableOutput();
        Connection connection = context.connection();

        Map<String, Object> connectionParameters = connection.getParameters();

        String server = MailchimpUtils.getMailChimpServer(
            MapUtils.getRequiredString(connectionParameters, ACCESS_TOKEN));

        Map<String, ?> inputParameters = context.inputParameters();

        HttpClientUtils
            .delete(
                "https://%s.api.mailchimp.com/3.0/lists/$%s/webhooks/$%s".formatted(
                    server, MapUtils.getString(inputParameters, LIST_ID),
                    dynamicWebhookEnableOutput.getParameter(LIST_ID)))
            .execute();
    }

    protected static WebhookOutput dynamicWebhookRequest(DynamicWebhookRequestContext context) {
        WebhookBody webhookBody = context.body();

        return WebhookOutput.map((Map<?, ?>) webhookBody.content());
    }

    protected static DynamicWebhookEnableOutput dynamicWebhookEnable(EnableDynamicWebhookContext context) {
        Connection connection = context.connection();

        String server = MailchimpUtils.getMailChimpServer(
            MapUtils.getRequiredString(connection.getParameters(), ACCESS_TOKEN));

        Map<String, ?> inputParameters = context.inputParameters();

        Map<?, ?> response = (Map<?, ?>) HttpClientUtils
            .post("https://%s.api.mailchimp.com/3.0/lists/$%s/webhooks".formatted(
                server, MapUtils.getString(inputParameters, LIST_ID)))
            .body(Body.of(
                Map.of(
                    "url", context.webhookUrl(),
                    "events", Map.of(SUBSCRIBE, true),
                    "sources", Map.of(
                        "user", true,
                        "admin", true,
                        "api", true))))
            .configuration(responseType(HttpClientUtils.ResponseType.JSON))
            .execute()
            .body();

        return new DynamicWebhookEnableOutput(Map.of("id", response.get("id")), null);
    }
}
