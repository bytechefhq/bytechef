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

package com.bytechef.component.calendly.trigger;

import static com.bytechef.component.calendly.constant.CalendlyConstants.SCOPE;
import static com.bytechef.component.calendly.constant.CalendlyConstants.UUID;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.calendly.util.CalendlyUtils;
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

/**
 * @author Monika Kušter
 */
public class CalendlyInviteeCanceledTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("inviteeCanceled")
        .title("Invitee Canceled")
        .description("Triggers when an invitee cancels a scheduled event.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(SCOPE)
                .label("Scope")
                .options(
                    option("User", "user"),
                    option("Organization", "organization"))
                .required(true))
        .output()
        .webhookDisable(CalendlyInviteeCanceledTrigger::webhookDisable)
        .webhookEnable(CalendlyInviteeCanceledTrigger::webhookEnable)
        .webhookRequest(CalendlyInviteeCanceledTrigger::webhookRequest);

    private CalendlyInviteeCanceledTrigger() {
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        CalendlyUtils.unsubscribeWebhook(context, outputParameters.getString(UUID));
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
        TriggerContext context) {

        return CalendlyUtils.subscribeWebhook(
            context, webhookUrl, inputParameters.getRequiredString(SCOPE), "invitee.canceled");
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders httpHeaders,
        HttpParameters httpParameters, WebhookBody webhookBody, WebhookMethod webhookMethod,
        Parameters webhookEnableOutput, TriggerContext triggerContext) {

        return webhookBody.getContent(new TypeReference<>() {});
    }

}
