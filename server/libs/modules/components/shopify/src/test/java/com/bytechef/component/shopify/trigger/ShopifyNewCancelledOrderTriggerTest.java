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

import static com.bytechef.component.shopify.constant.ShopifyConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.shopify.util.ShopifyTriggerUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 * @author Nikolina Spehar
 */
class ShopifyNewCancelledOrderTriggerTest extends AbstractShopifyTriggerTest {

    @Test
    void testWebhookEnable() {
        String webhookUrl = "testWebhookUrl";

        shopifyTriggerUtilsMockedStatic
            .when(() -> ShopifyTriggerUtils.subscribeWebhook(
                stringArgumentCaptor.capture(), stringArgumentCaptor.capture(), contextArgumentCaptor.capture()))
            .thenReturn("webhookId");

        WebhookEnableOutput webhookEnableOutput = ShopifyNewCancelledOrderTrigger.webhookEnable(
            mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

        WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(
            Map.of(ID, "webhookId"), null);

        assertEquals(expectedWebhookEnableOutput, webhookEnableOutput);
        assertEquals(List.of(webhookUrl, "ORDERS_CANCELLED"), stringArgumentCaptor.getAllValues());
        assertEquals(mockedTriggerContext, contextArgumentCaptor.getValue());
    }

    @Test
    void testWebhookDisable() {
        mockedParameters = MockParametersFactory.create(Map.of(ID, "webhookId"));

        ShopifyNewCancelledOrderTrigger.webhookDisable(
            mockedParameters, mockedParameters, mockedParameters, workflowExecutionId, mockedTriggerContext);

        shopifyTriggerUtilsMockedStatic
            .verify(() -> ShopifyTriggerUtils.unsubscribeWebhook(mockedParameters, mockedTriggerContext));
    }

    @Test
    void testWebhookRequest() {
        when(mockedWebhookBody.getContent())
            .thenReturn(mockedObject);

        Object result = ShopifyNewCancelledOrderTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedParameters, mockedTriggerContext);

        assertEquals(mockedObject, result);
    }
}
