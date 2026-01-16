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

package com.bytechef.component.hubspot.trigger;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.hubspot.constant.HubspotConstants.APP_ID;
import static com.bytechef.component.hubspot.constant.HubspotConstants.HAPIKEY;
import static com.bytechef.component.hubspot.constant.HubspotConstants.ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.hubspot.util.HubspotUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class HubspotNewDealTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newDeal")
        .title("New Deal")
        .description("Triggers when a new deal is added.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(APP_ID)
                .label("App Id")
                .description("The id of a Hubspot app used to register this trigger to.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("eventId")
                            .description("ID of the event that triggered the workflow."),
                        string("subscriptionId")
                            .description("ID of the subscription associated with this webhook event."),
                        string("subscriptionType")
                            .description("Type of the subscription, indicating the nature of event."),
                        string("objectId")
                            .description("ID for the newly created deal."))))
        .webhookDisable(HubspotNewDealTrigger::webhookDisable)
        .webhookEnable(HubspotNewDealTrigger::webhookEnable)
        .webhookRequest(HubspotNewDealTrigger::webhookRequest);

    private HubspotNewDealTrigger() {
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext triggerContext) {

        HubspotUtils.unsubscribeWebhook(
            inputParameters.getString(APP_ID), outputParameters.getRequiredString(ID),
            connectionParameters.getRequiredString(HAPIKEY), triggerContext);
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
        TriggerContext triggerContext) {

        return new WebhookEnableOutput(
            Map.of(ID,
                HubspotUtils.subscribeWebhook(
                    "deal.creation", inputParameters.getRequiredString(APP_ID),
                    connectionParameters.getRequiredString(HAPIKEY), webhookUrl, triggerContext)),
            null);
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers,
        HttpParameters parameters, WebhookBody body, WebhookMethod method, Parameters output,
        TriggerContext triggerContext) {

        return HubspotUtils.extractFirstContentMap(body);
    }
}
