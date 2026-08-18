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

package com.bytechef.component.twilio.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.agentRequest;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.twilio.constant.TwilioConstants.BODY;
import static com.bytechef.component.twilio.constant.TwilioConstants.FROM;
import static com.bytechef.component.twilio.constant.TwilioConstants.NUMBER;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;

/**
 * The request half of the {@code twilio} agent channel.
 * <p>
 * {@code number} is the WhatsApp-enabled Twilio number this channel listens on. It is the reply's sender too — the
 * paired {@code sendWhatsAppMessage} action takes it from the channel row through
 * {@code channelParameter(NUMBER, FROM)} — which is why it is declared here even though the webhook itself needs no
 * configuration. It is optional because the trigger already exists in saved workflows and requiring it would invalidate
 * them.
 * <p>
 * The request paths are Twilio's own capitalised form-field names. The trigger declares no output schema, returning the
 * form-encoded webhook body verbatim, so nothing validates them at construction time; they are pinned instead by
 * {@code TwilioNewWhatsappMessageTriggerAgentChannelTest}. No {@code attachments} path is bound: there is no declared
 * output to bind one in, and an absent path is how a channel states that it carries none.
 *
 * @author Monika Kušter
 */
public class TwilioNewWhatsappMessageTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newWhatsappMessage")
        .title("New WhatsApp Message")
        .description("Triggers when a new WhatsApp message is received.")
        .type(TriggerType.STATIC_WEBHOOK)
        .properties(
            string(NUMBER)
                .label("Number")
                .description("The WhatsApp-enabled Twilio number this channel listens on.")
                .exampleValue("whatsapp:+15554449999")
                .required(false))
        .output()
        .agentRequest(
            agentRequest()
                .conversationId(FROM)
                .message(BODY))
        .webhookRequest(TwilioNewWhatsappMessageTrigger::webhookRequest);

    private TwilioNewWhatsappMessageTrigger() {
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters webhookEnableOutput, TriggerContext context) {

        return body.getContent();
    }
}
