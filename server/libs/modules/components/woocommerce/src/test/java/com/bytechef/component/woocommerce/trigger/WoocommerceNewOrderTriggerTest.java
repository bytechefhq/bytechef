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

import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.TriggerDefinition;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class WoocommerceNewOrderTriggerTest extends AbstractWoocommerceTriggerTest {

    @Test
    void testWebhookEnable() {
        String webhookUrl = "testWebhookUrl";

        TriggerDefinition.WebhookEnableOutput webhookEnableOutput = WoocommerceNewOrderTrigger.webhookEnable(
            mockedParameters, mockedParameters, webhookUrl, "testWorkflowExecutionId", mockedTriggerContext);

        assertEquals(new TriggerDefinition.WebhookEnableOutput(Map.of(ID, "3"), null), webhookEnableOutput);

        Context.Http.Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(
            "delivery_url", webhookUrl, "name", "New Order Webhook", "topic", "order.created");

        assertEquals(expectedBody, body.getContent());
    }

    @Test
    void testWebhookRequest() {
        Map<String, Object> mockWebhookContent = Map.of("test", "value");

        when(mockedWebhookBody.getContent())
            .thenReturn(mockWebhookContent);

        Object result = WoocommerceNewOrderTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertEquals(mockWebhookContent, result);
    }

}
