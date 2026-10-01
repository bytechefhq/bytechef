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

package com.bytechef.component.infobip.trigger;

import static com.bytechef.component.definition.ComponentDsl.agentRequest;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.infobip.constant.InfobipConstants.CONFIGURATION_KEY;
import static com.bytechef.component.infobip.constant.InfobipConstants.KEYWORD;
import static com.bytechef.component.infobip.constant.InfobipConstants.KEYWORD_PROPERTY;
import static com.bytechef.component.infobip.constant.InfobipConstants.NUMBER;
import static com.bytechef.component.infobip.util.InfobipUtils.getWebhookEnableOutput;
import static com.bytechef.component.infobip.util.InfobipUtils.unsubscribeWebhook;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;

/**
 * The request half of the {@code infobip} agent channel.
 * <p>
 * {@code number} is the Infobip number this channel listens on. It is the reply's sender too — the paired
 * {@code sendWhatsappTextMessage} action takes it from the channel row through {@code channelParameter(NUMBER, FROM)} —
 * and it is required here already, so a channel row cannot leave the reply's sender unset.
 * <p>
 * The request paths reproduce the expressions the agent channel generates today, minus their {@code ${__NODE__.}}
 * wrapper. The trigger declares no output schema, returning Infobip's webhook body verbatim, so nothing validates them
 * at construction time; they are pinned instead by {@code InfobipNewWhatsAppMessageTriggerAgentChannelTest}, which
 * names the payload documentation they assume. No {@code attachments} path is bound: there is no declared output to
 * bind one in, and an absent path is how a channel states that it carries none.
 *
 * @author Monika Kušter
 */
public class InfobipNewWhatsAppMessageTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newWhatsappMessage")
        .title("New WhatsApp Message")
        .description("Triggers when a new WhatsApp message is received.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(NUMBER)
                .label("Number")
                .description("Number to monitor for new WhatsApp messages.")
                .required(true),
            KEYWORD_PROPERTY)
        .output()
        .agentRequest(
            agentRequest()
                .conversationId("results.from")
                .message("results.message.text"))
        .help("", "https://docs.bytechef.io/reference/components/infobip_v1#new-whatsapp-message")
        .webhookEnable(InfobipNewWhatsAppMessageTrigger::webhookEnable)
        .webhookDisable(InfobipNewWhatsAppMessageTrigger::webhookDisable)
        .webhookRequest(InfobipNewWhatsAppMessageTrigger::dynamicWebhookRequest);

    private InfobipNewWhatsAppMessageTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext triggerContext) {

        return getWebhookEnableOutput(
            inputParameters.getRequiredString(NUMBER), "WHATSAPP", inputParameters.getString(KEYWORD), webhookUrl,
            triggerContext);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext triggerContext) {

        unsubscribeWebhook(outputParameters.getRequiredString(CONFIGURATION_KEY), triggerContext);
    }

    protected static Object dynamicWebhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext triggerContext) {

        return body.getContent();
    }
}
