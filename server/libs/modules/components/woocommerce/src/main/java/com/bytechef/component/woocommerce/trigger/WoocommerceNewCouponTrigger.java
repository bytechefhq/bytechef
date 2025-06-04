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

import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.ID;
import static com.bytechef.component.woocommerce.util.WoocommerceUtils.deleteWebhook;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class WoocommerceNewCouponTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newCoupon")
        .title("New Coupon")
        .description("Triggers when any coupon is created.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
//        .output(outputSchema(TRIGGER_OUTPUT_PROPERTY))
        .webhookEnable(WoocommerceNewCouponTrigger::webhookEnable)
        .webhookDisable(WoocommerceNewCouponTrigger::webhookDisable)
        .webhookRequest(WoocommerceNewCouponTrigger::webhookRequest);

    private WoocommerceNewCouponTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        Map<String, ?> body = context.http(http -> http.post("/webhooks"))
            .body(
                Body.of("delivery_url", webhookUrl, "name", "New Coupon Webhook", "topic", "coupon.created"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return new WebhookEnableOutput(Map.of(ID, body.get(ID)), null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        deleteWebhook(outputParameters.getRequiredInteger(ID), context);
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, WebhookEnableOutput output, TriggerContext context) {

        return body.getContent();
    }
}
