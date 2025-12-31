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

package com.bytechef.component.shopify.trigger;

import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ID;
import static com.bytechef.component.shopify.util.ShopifyTriggerUtils.subscribeWebhook;
import static com.bytechef.component.shopify.util.ShopifyTriggerUtils.unsubscribeWebhook;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import java.util.Map;

/**
 * @author Monika Domiter
 * @author Nikolina Spehar
 */
public class ShopifyNewPaidOrderTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newPaidOrder")
        .title("New Paid Order")
        .description("Triggers when paid order is created.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .output()
        .webhookEnable(ShopifyNewPaidOrderTrigger::webhookEnable)
        .webhookDisable(ShopifyNewPaidOrderTrigger::webhookDisable)
        .webhookRequest(ShopifyNewPaidOrderTrigger::webhookRequest);

    private ShopifyNewPaidOrderTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        return new WebhookEnableOutput(Map.of(ID, subscribeWebhook(webhookUrl, "ORDERS_PAID", context)), null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        unsubscribeWebhook(outputParameters, context);
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        return body.getContent();
    }
}
