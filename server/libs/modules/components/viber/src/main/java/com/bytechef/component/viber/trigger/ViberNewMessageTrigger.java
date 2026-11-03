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

package com.bytechef.component.viber.trigger;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.viber.constant.ViberConstants.EVENT_OUTPUT_PROPERTIES;
import static com.bytechef.component.viber.constant.ViberConstants.MESSAGE;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.viber.util.ViberEventUtils;
import com.bytechef.component.viber.util.ViberWebhookUtils;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Ivica Cardic
 */
public class ViberNewMessageTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newMessage")
        .title("New Message")
        .description("Triggers when a Viber user sends a message to your bot.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .output(outputSchema(object().properties(EVENT_OUTPUT_PROPERTIES)))
        .webhookValidateWithConnection(ViberWebhookUtils::validate)
        .webhookValidateOnEnable(ViberWebhookUtils::validateOnEnable)
        .webhookEnable(ViberNewMessageTrigger::webhookEnable)
        .webhookDisable(ViberNewMessageTrigger::webhookDisable)
        .webhookRequest(ViberNewMessageTrigger::webhookRequest)
        .help("", "https://docs.bytechef.io/reference/components/viber_v1#new-message");

    private ViberNewMessageTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        return ViberWebhookUtils.enableWebhook(webhookUrl, List.of(), context);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        ViberWebhookUtils.disableWebhook(outputParameters.getString("webhookUrl"), context);
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        Map<String, Object> event = body.getContent(new TypeReference<Map<String, Object>>() {});

        return ViberEventUtils.toOutput(event, Set.of(MESSAGE), context);
    }
}
