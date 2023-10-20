
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

import com.bytechef.hermes.component.Context.Connection;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookDisableContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookRequestContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookOutput;
import com.bytechef.hermes.component.util.HttpClientUtils;
import com.bytechef.hermes.component.util.HttpClientUtils.Body;
import com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;

import java.util.List;
import java.util.Map;

import static com.bytechef.hermes.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.hermes.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.hermes.component.util.HttpClientUtils.responseFormat;
import static com.bytechef.hermes.definition.DefinitionDSL.dateTime;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class SubscribeTrigger {

    private static final String LIST_ID = "listId";
    private static final String SUBSCRIBE = "subscribe";

    public static final TriggerDefinition TRIGGER_DEFINITION = ComponentDSL.trigger(SUBSCRIBE)
        .display(
            display("Subscribe")
                .description("Triggers when an Audience subscriber is added to the list."))
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(LIST_ID)
// TODO
//                .optionsDataSource()
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
        .dynamicWebhookEnable(SubscribeTrigger::dynamicWebhookEnable)
        .dynamicWebhookDisable(SubscribeTrigger::dynamicWebhookDisable)
        .dynamicWebhookRequest(SubscribeTrigger::dynamicWebhookRequest);

    protected static void dynamicWebhookDisable(DynamicWebhookDisableContext context) {
        Connection connection = context.connection();
        DynamicWebhookEnableOutput dynamicWebhookEnableOutput = context.dynamicWebhookEnableOutput();
        InputParameters inputParameters = context.inputParameters();

        String server = getMailChimpServer(connection.getString(ACCESS_TOKEN));

        HttpClientUtils
            .delete(
                "https://%s.api.mailchimp.com/3.0/lists/$%s/webhooks/$%s".formatted(
                    server, inputParameters.getString(LIST_ID), dynamicWebhookEnableOutput.getParameter(LIST_ID)))
            .execute();
    }

    protected static DynamicWebhookEnableOutput dynamicWebhookEnable(DynamicWebhookEnableContext context) {
        Connection connection = context.connection();
        InputParameters inputParameters = context.inputParameters();

        String server = getMailChimpServer(connection.getString(ACCESS_TOKEN));

        Map<?, ?> response = (Map<?, ?>) HttpClientUtils
            .post(
                "https://%s.api.mailchimp.com/3.0/lists/$%s/webhooks".formatted(
                    server, inputParameters.getString(LIST_ID)))
            .body(Body.of(
                Map.of(
                    "url", context.webhookUrl(),
                    "events", Map.of(SUBSCRIBE, true),
                    "sources", Map.of(
                        "user", true,
                        "admin", true,
                        "api", true))))
            .configuration(responseFormat(ResponseFormat.JSON))
            .execute()
            .getBody();

        String id = (String) response.get("id");

        return new DynamicWebhookEnableOutput(Map.of("id", id), null);
    }

    protected static WebhookOutput dynamicWebhookRequest(DynamicWebhookRequestContext context) {
        WebhookBody webhookBody = context.body();

        return WebhookOutput.map((Map<?, ?>) webhookBody.getContent());
    }

    private static String getMailChimpServer(String accessToken) {
        Map<?, ?> response = (Map<?, ?>) HttpClientUtils
            .get("https://login.mailchimp.com/oauth2/metadata")
            .configuration(responseFormat(ResponseFormat.JSON))
            .headers(Map.of(AUTHORIZATION, List.of("OAuth " + accessToken)))
            .execute()
            .getBody();

        return (String) response.get("dc");
    }
}
