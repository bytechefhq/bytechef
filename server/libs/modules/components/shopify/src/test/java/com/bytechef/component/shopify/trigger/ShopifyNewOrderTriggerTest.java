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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.shopify.util.ShopifyUtils;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class ShopifyNewOrderTriggerTest extends AbstractShopifyTriggerTest {

    @Test
    void testWebhookEnable() {
        String webhookUrl = "testWebhookUrl";

        shopifyUtilsMockedStatic.when(
            () -> ShopifyUtils.subscribeWebhook(webhookUrl, "orders/create", mockedTriggerContext))
            .thenReturn(123L);
        WebhookEnableOutput webhookEnableOutput = ShopifyNewOrderTrigger.webhookEnable(
            mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

        Map<String, ?> parameters = webhookEnableOutput.parameters();
        Instant webhookExpirationDate = webhookEnableOutput.webhookExpirationDate();

        Map<String, Object> expectedParameters = Map.of(ID, 123L);

        assertEquals(expectedParameters, parameters);
        assertNull(webhookExpirationDate);
    }

    @Test
    void testWebhookDisable() {

        ShopifyNewOrderTrigger.webhookDisable(
            mockedParameters, mockedParameters, mockedParameters, workflowExecutionId, mockedTriggerContext);

        shopifyUtilsMockedStatic
            .verify(() -> ShopifyUtils.unsubscribeWebhook(mockedParameters, mockedTriggerContext));

    }

    @Test
    void testWebhookRequest() {
        when(mockedWebhookBody.getContent())
            .thenReturn(mockedObject);

        Object result = ShopifyNewOrderTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertEquals(mockedObject, result);

    }
}
