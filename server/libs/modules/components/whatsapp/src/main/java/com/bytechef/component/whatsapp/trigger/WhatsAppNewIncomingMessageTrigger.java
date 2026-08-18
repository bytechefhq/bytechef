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
import static com.bytechef.component.definition.ComponentDsl.agentRequest;
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
 * The request half of the {@code whatsapp} agent channel. Its descriptor reproduces, unchanged, the expression paths
 * the channel has always read.
 * <p>
 * Two known limitations ride along with those paths, both inherited rather than introduced here, and both pinned by
 * {@code WhatsAppNewIncomingMessageTriggerAgentChannelTest}:
 * <ol>
 * <li>The paths descend through {@code value}, matching Meta's documented payload, while the output schema declared
 * below places {@code messages} as a SIBLING of {@code value} under {@code changes}. One of the two is wrong and this
 * repository holds no fixture that says which.</li>
 * <li>Meta documents {@code entry}, {@code changes} and {@code messages} as arrays; the schema below declares each a
 * single object, and the paths address them as such. The evaluator is SpEL, which indexes explicitly but does not
 * auto-flatten a list, so if the payload really is an array these paths resolve to nothing.</li>
 * </ol>
 * The path segments are therefore spelled as literals rather than assembled from the constants the schema uses: sharing
 * constants would assert a correspondence between path and schema that does not currently hold.
 * <p>
 * The first limitation is also why this is the one channel declaring {@code agentRequest().unverifiedPaths(...)}. Every
 * other trigger with a declared output schema has each segment of each path checked against it; here that check would
 * have to be satisfied by editing one of the two disagreeing halves, and neither may be edited until a live webhook
 * says which is right. Opting out states that in the declaration rather than silently weakening the rule for everyone,
 * and it still leaves each path's first segment checked.
 * <p>
 * No {@code attachments} path is bound. The trigger's message output declares no attachments field, today's envelope
 * maps the literal {@code []}, and an absent path is how a channel states that it carries none — leaving the generator
 * to wire that same {@code []}.
 *
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
        .agentRequest(
            agentRequest()
                .conversationId("entry.changes.value.messages.from")
                .message("entry.changes.value.messages.text.body")
                .unverifiedPaths(
                    "the declared output schema places 'messages' as a sibling of 'value' under 'changes', while "
                        + "these paths descend through 'value' as Meta's documented payload does; one of the two is "
                        + "wrong and this repository holds no fixture that says which, so neither may be changed to "
                        + "make the other validate"))
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
