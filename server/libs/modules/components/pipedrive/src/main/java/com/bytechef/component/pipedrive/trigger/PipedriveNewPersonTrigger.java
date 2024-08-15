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

package com.bytechef.component.pipedrive.trigger;

import static com.bytechef.component.definition.ComponentDSL.trigger;
import static com.bytechef.component.pipedrive.constant.PipedriveConstants.ADDED;
import static com.bytechef.component.pipedrive.constant.PipedriveConstants.CURRENT;
import static com.bytechef.component.pipedrive.constant.PipedriveConstants.ID;
import static com.bytechef.component.pipedrive.constant.PipedriveConstants.PERSON_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.pipedrive.util.PipedriveUtils;
import java.util.Map;

/**
 * @author Ivica Cardic
 * @author Monika Kušter
 */
public class PipedriveNewPersonTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newPerson")
        .title("New Person")
        .description("Trigger off whenever a new person is added.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .outputSchema(PERSON_OUTPUT_PROPERTY)
        .webhookDisable(PipedriveNewPersonTrigger::webhookDisable)
        .webhookEnable(PipedriveNewPersonTrigger::webhookEnable)
        .webhookRequest(PipedriveNewPersonTrigger::webhookRequest);

    private PipedriveNewPersonTrigger() {
    }

    protected static void webhookDisable(
        Map<String, ?> inputParameters, Parameters connectionParameters, Map<String, ?> outputParameters,
        String workflowExecutionId, TriggerContext context) {

        PipedriveUtils.unsubscribeWebhook((Integer) outputParameters.get(ID), context);
    }

    protected static WebhookEnableOutput webhookEnable(
        Map<String, ?> inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        return new WebhookEnableOutput(
            Map.of("id", PipedriveUtils.subscribeWebhook("person", ADDED, webhookUrl, context)), null);
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, WebhookEnableOutput output, TriggerContext context) {

        return body.getContent(new TypeReference<Map<String, ?>>() {})
            .get(CURRENT);
    }
}
