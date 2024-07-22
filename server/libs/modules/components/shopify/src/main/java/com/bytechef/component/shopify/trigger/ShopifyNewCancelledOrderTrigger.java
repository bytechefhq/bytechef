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

package com.bytechef.component.shopify.trigger;

import static com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.trigger;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.NEW_CANCELLED_ORDER;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.shopify.property.ShopifyOrderProperties;
import com.bytechef.component.shopify.util.ShopifyUtils;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class ShopifyNewCancelledOrderTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger(NEW_CANCELLED_ORDER)
        .title("New Cancelled Order")
        .description("Triggers when order is cancelled.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .outputSchema(object().properties(ShopifyOrderProperties.PROPERTIES))
        .dynamicWebhookEnable(ShopifyNewCancelledOrderTrigger::dynamicWebhookEnable)
        .dynamicWebhookDisable(ShopifyNewCancelledOrderTrigger::dynamicWebhookDisable)
        .dynamicWebhookRequest(ShopifyNewCancelledOrderTrigger::dynamicWebhookRequest);

    private ShopifyNewCancelledOrderTrigger() {
    }

    protected static DynamicWebhookEnableOutput dynamicWebhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        return new DynamicWebhookEnableOutput(
            Map.of(ID, ShopifyUtils.subscribeWebhook(connectionParameters, webhookUrl, context, "orders/cancelled")),
            null);
    }

    protected static void dynamicWebhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        ShopifyUtils.unsubscribeWebhook(connectionParameters, outputParameters, context);
    }

    protected static Object dynamicWebhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, DynamicWebhookEnableOutput output, TriggerContext context) {

        return body.getContent();
    }

}
