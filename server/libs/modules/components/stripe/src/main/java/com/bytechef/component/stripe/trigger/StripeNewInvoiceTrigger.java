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
public class StripeNewInvoiceTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newInvoice")
        .title("New Invoice")
        .description("Triggers on a new invoice.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID)
                            .description("ID of the invoice."),
                        string("object")
                            .description("Value is 'invoice'."),
                        string("currency")
                            .description("Currency of the invoice."),
                        string("customer")
                            .description("ID of the customer who will be billed."),
                        string("customer_name")
                            .description("Name of the customer who will be billed."),
                        string("description")
                            .description("Description of the invoice."))))
        .webhookEnable(StripeNewInvoiceTrigger::webhookEnable)
        .webhookDisable(StripeNewInvoiceTrigger::webhookDisable)
        .webhookRequest(StripeNewInvoiceTrigger::webhookRequest);

    private StripeNewInvoiceTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
        TriggerContext context) {

        return new WebhookEnableOutput(
            Map.of(ID, StripeUtils.subscribeWebhook(webhookUrl, context, "invoice.created")), null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        StripeUtils.unsubscribeWebhook(outputParameters.getString(ID), context);
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        return StripeUtils.getNewObject(body);
    }
}
