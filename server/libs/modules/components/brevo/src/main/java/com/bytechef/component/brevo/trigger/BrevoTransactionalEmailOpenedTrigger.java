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

package com.bytechef.component.brevo.trigger;

import static com.bytechef.component.brevo.constant.BrevoConstants.ID;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.brevo.util.BrevoUtils;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class BrevoTransactionalEmailOpenedTrigger {

    public static final ComponentDsl.ModifiableTriggerDefinition TRIGGER_DEFINITION =
        trigger("transactionalEmailOpened")
            .title("Transactional Email Opened")
            .description("Triggers when transactional email is opened.")
            .type(TriggerDefinition.TriggerType.DYNAMIC_WEBHOOK)
            .properties()
            .output()
            .webhookEnable(BrevoTransactionalEmailOpenedTrigger::webhookEnable)
            .webhookDisable(BrevoTransactionalEmailOpenedTrigger::webhookDisable)
            .webhookRequest(BrevoTransactionalEmailOpenedTrigger::webhookRequest);

    private BrevoTransactionalEmailOpenedTrigger() {
    }

    protected static TriggerDefinition.WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        return new TriggerDefinition.WebhookEnableOutput(
            Map.of(ID,
                BrevoUtils.createWebhook(webhookUrl, context)),
            null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        BrevoUtils.deleteWebhook(outputParameters, context);
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, TriggerDefinition.HttpHeaders headers,
        TriggerDefinition.HttpParameters parameters,
        TriggerDefinition.WebhookBody body, TriggerDefinition.WebhookMethod method,
        TriggerDefinition.WebhookEnableOutput output, TriggerContext context) {

        return body.getContent();
    }
}
