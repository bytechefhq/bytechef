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

package com.bytechef.component.whatsapp.trigger;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import static com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import static com.bytechef.component.definition.TriggerDefinition.TriggerType;
import static com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import static com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.GET_MESSAGE;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.RECEIVE_USER;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.SENDER_NUMBER;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.whatsapp.util.WhatsAppUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Luka Ljubić
 */
public class WhatsAppNewIncomingMessageTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("messageReceived")
        .title("Message Received")
        .description("Triggers when you get a new message from certain number.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(SENDER_NUMBER)
                .label("Sender Number")
                .description("Type in the number from whom you want to trigger")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("object"),
                        object("entry")
                            .properties(
                                string("id"),
                                object("changes")
                                    .properties(
                                        object("value")
                                            .properties(
                                                string("messaging_product"),
                                                object("metadata")
                                                    .properties(
                                                        string("display_phone_number"),
                                                        string("phone_number_id"))),
                                        object("contacts")
                                            .properties(
                                                object("profile")
                                                    .properties(
                                                        string("name")),
                                                string("wa_id")),
                                        object("messages")
                                            .properties(
                                                string("from"),
                                                string("id"),
                                                string("timestamp"),
                                                object("text")
                                                    .properties(
                                                        string("body"))))))))
        .webhookDisable(WhatsAppNewIncomingMessageTrigger::webhookDisable)
        .webhookEnable(WhatsAppNewIncomingMessageTrigger::webhookEnable)
        .webhookRequest(WhatsAppNewIncomingMessageTrigger::webhookRequest);

    private WhatsAppNewIncomingMessageTrigger() {
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders httpHeaders,
        HttpParameters httpParameters, WebhookBody body, WebhookMethod webhookMethod,
        Parameters webhookEnableOutput, TriggerContext context) {

        if (body == null) {
            return null;
        }

        return body.getContent();
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
        TriggerContext context) {

        String server = WhatsAppUtils.getWhatsappServer(connectionParameters.getRequiredString(ACCESS_TOKEN), context);
        String url = "/webhooks";

        Map<?, ?> response = context
            .http(http -> http.post(url.formatted(server, inputParameters.getRequiredString(RECEIVE_USER))))
            .body(
                Http.Body.of(
                    Map.of(
                        "url", webhookUrl,
                        "events", Map.of(GET_MESSAGE, true),
                        "sources", Map.of("api", true))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (response.containsKey("errors")) {
            List<?> errors = (List<?>) response.get("errors");

            Map<?, ?> firstError = (Map<?, ?>) errors.getFirst();

            throw new IllegalStateException((String) firstError.get("message"));
        }

        return new WebhookEnableOutput(Map.of("id", response.get("id")), null);
    }

    public static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters, String s,
        TriggerContext context) {

        String url = "/webhooks";

        String server = WhatsAppUtils.getWhatsappServer(connectionParameters.getRequiredString(ACCESS_TOKEN), context);

        context.http(http -> http
            .delete(
                url.formatted(server, inputParameters.getRequiredString(SENDER_NUMBER), outputParameters.get("id"))))
            .execute();
    }
}
