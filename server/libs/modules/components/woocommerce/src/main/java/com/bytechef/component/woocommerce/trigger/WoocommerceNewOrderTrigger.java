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

package com.bytechef.component.woocommerce.trigger;

import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.ID;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.TRIGGER_OUTPUT_PROPERTY;
import static com.bytechef.component.woocommerce.util.WoocommerceUtils.createWebhook;
import static com.bytechef.component.woocommerce.util.WoocommerceUtils.deleteWebhook;

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
 * @author Marija Horvat
 */
public class WoocommerceNewOrderTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newOrder")
        .title("New Order")
        .description("Triggers when any order is created.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .output(outputSchema(TRIGGER_OUTPUT_PROPERTY))
        .webhookEnable(WoocommerceNewOrderTrigger::webhookEnable)
        .webhookDisable(WoocommerceNewOrderTrigger::webhookDisable)
        .webhookRequest(WoocommerceNewOrderTrigger::webhookRequest);

    private WoocommerceNewOrderTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
        TriggerContext context) {

        return createWebhook(webhookUrl, context, "order.created");
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        deleteWebhook(outputParameters.getRequiredInteger(ID), context);
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        return body.getContent();
    }
}
