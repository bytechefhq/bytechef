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

package com.bytechef.component.stripe.trigger;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.stripe.constant.StripeConstants.ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.stripe.util.StripeUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class StripeNewCustomerTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newCustomer")
        .title("New Customer")
        .description("Triggers when a new customer is created.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID),
                        string("object"),
                        string("description"),
                        string("email"),
                        string("name"),
                        string("phone"),
                        object("address")
                            .properties(
                                string("city"),
                                string("country"),
                                string("line1"),
                                string("line2"),
                                string("postal_code"),
                                string("state")))))
        .webhookEnable(StripeNewCustomerTrigger::webhookEnable)
        .webhookDisable(StripeNewCustomerTrigger::webhookDisable)
        .webhookRequest(StripeNewCustomerTrigger::webhookRequest);

    private StripeNewCustomerTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
        TriggerContext context) {

        return new WebhookEnableOutput(
            Map.of(ID, StripeUtils.subscribeWebhook(webhookUrl, context, "customer.created")), null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        StripeUtils.unsubscribeWebhook(outputParameters.getString(ID), context);
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, WebhookEnableOutput output, TriggerContext context) {

        return StripeUtils.getNewObject(body);
    }
}
