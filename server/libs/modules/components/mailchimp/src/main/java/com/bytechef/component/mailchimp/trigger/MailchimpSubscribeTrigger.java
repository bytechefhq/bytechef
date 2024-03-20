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

package com.bytechef.component.mailchimp.trigger;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.mailchimp.util.MailchimpUtils;
import java.util.List;
import java.util.Map;

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
                .description("The list id of intended audience to which you would like to add the contact.")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    object("data")
                        .properties(
                            string("email"),
                            string("email_type"),
                            string("id"),
                            string("ip_opt"),
                            string("ip_signup"),
                            string("list_id"),
                            object("merges")
                                .properties(
                                    string("EMAIL"),
                                    string("FNAME"),
                                    string("INTERESTS"),
                                    string("LNAME"))),
                    dateTime("fired_at"),
                    string("type")))
        .dynamicWebhookDisable(MailchimpSubscribeTrigger::dynamicWebhookDisable)
        .dynamicWebhookEnable(MailchimpSubscribeTrigger::dynamicWebhookEnable)
        .dynamicWebhookRequest(MailchimpSubscribeTrigger::dynamicWebhookRequest);

    protected static void dynamicWebhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, Context context) {

        String server = MailchimpUtils.getMailChimpServer(
            connectionParameters.getRequiredString(ACCESS_TOKEN), context);

        context.http(http -> http.delete(
            "https://%s.api.mailchimp.com/3.0/lists/%s/webhooks/%s".formatted(
                server, inputParameters.getRequiredString(LIST_ID), outputParameters.get("id"))))
            .execute();
    }

    protected static DynamicWebhookEnableOutput dynamicWebhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, Context context) {

        String server = MailchimpUtils.getMailChimpServer(
            connectionParameters.getRequiredString(ACCESS_TOKEN), context);

        Map<?, ?> response = context
            .http(http -> http.post(
                "https://%s.api.mailchimp.com/3.0/lists/%s/webhooks".formatted(
                    server, inputParameters.getRequiredString(LIST_ID))))
            .body(
                Body.of(
                    Map.of(
                        "url", webhookUrl,
                        "events", Map.of(SUBSCRIBE, true),
                        "sources", Map.of(
                            "user", true,
                            "admin", true,
                            "api", true))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new Context.TypeReference<>() {});

        if (response.containsKey("errors")) {
            List<?> errors = (List<?>) response.get("errors");

            Map<?, ?> firstError = (Map<?, ?>) errors.getFirst();

            throw new IllegalStateException((String) firstError.get("message"));
        }

        return new DynamicWebhookEnableOutput(Map.of("id", response.get("id")), null);
    }

    protected static Object dynamicWebhookRequest(
        Map<String, ?> inputParameters, Parameters connectionParameters, HttpHeaders headers,
        HttpParameters parameters, WebhookBody body, WebhookMethod method, DynamicWebhookEnableOutput output,
        TriggerContext context) {

        if (body == null) {
            return null;
        }

        return body.getContent();
    }
}
