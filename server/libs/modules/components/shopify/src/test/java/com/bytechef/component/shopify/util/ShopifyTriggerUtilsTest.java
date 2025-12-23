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

package com.bytechef.component.shopify.util;

import static com.bytechef.component.shopify.constant.ShopifyConstants.ID;
import static com.bytechef.component.shopify.util.ShopifyTriggerUtils.subscribeWebhook;
import static com.bytechef.component.shopify.util.ShopifyTriggerUtils.unsubscribeWebhook;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class ShopifyTriggerUtilsTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final Context mockedContext = mock(Context.class);
    private final ArgumentCaptor<Object> objectArgumentCaptor = forClass(Object.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testSubscribeWebhook() {
        Map<String, Object> mockedObject = Map.of(
            "webhookSubscriptionCreate", Map.of("webhookSubscription", Map.of(ID, "id")));
        String mockedTopic = "topic";
        String mockedWebhookUrl = "webhookUrl";

        try (MockedStatic<ShopifyUtils> shopifyUtilsMockedStatic = mockStatic(ShopifyUtils.class)) {
            shopifyUtilsMockedStatic
                .when(() -> ShopifyUtils.sendGraphQlQuery(
                    stringArgumentCaptor.capture(),
                    contextArgumentCaptor.capture(),
                    (Map<String, Object>) objectArgumentCaptor.capture()))
                .thenReturn(mockedObject);

            String result = subscribeWebhook(mockedWebhookUrl, mockedTopic, mockedContext);

            assertEquals("id", result);

            String expectedQuery = """
                mutation WebhookSubscriptionCreate(
                  $topic: WebhookSubscriptionTopic!
                  $webhookSubscription: WebhookSubscriptionInput!
                ) {
                  webhookSubscriptionCreate(
                    topic: $topic
                    webhookSubscription: $webhookSubscription
                  ) {
                    webhookSubscription {
                      id
                      topic
                      filter
                      uri
                    }
                    userErrors {
                      field
                      message
                    }
                  }
                }
                """;

            Map<String, Object> expectedVariables = Map.of(
                "topic", "topic",
                "webhookSubscription", Map.of(
                    "uri", "webhookUrl"));

            assertEquals(expectedQuery, stringArgumentCaptor.getValue());
            assertEquals(expectedVariables, objectArgumentCaptor.getValue());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
        }
    }

    @Test
    void testUnsubscribeWebhook() {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(ID, "id"));

        try (MockedStatic<ShopifyUtils> shopifyUtilsMockedStatic = mockStatic(ShopifyUtils.class)) {
            shopifyUtilsMockedStatic
                .when(() -> ShopifyUtils.sendGraphQlQuery(
                    stringArgumentCaptor.capture(),
                    contextArgumentCaptor.capture(),
                    (Map<String, Object>) objectArgumentCaptor.capture()))
                .thenReturn(null);

            unsubscribeWebhook(mockedParameters, mockedContext);

            String expectedQuery = """
                mutation WebhookSubscriptionDelete($id: ID!) {
                  webhookSubscriptionDelete(id: $id) {
                    deletedWebhookSubscriptionId
                    userErrors {
                      field
                      message
                    }
                  }
                }
                """;

            Map<String, Object> expectedVariables = Map.of(ID, "id");

            assertEquals(expectedQuery, stringArgumentCaptor.getValue());
            assertEquals(expectedVariables, objectArgumentCaptor.getValue());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
        }
    }
}
