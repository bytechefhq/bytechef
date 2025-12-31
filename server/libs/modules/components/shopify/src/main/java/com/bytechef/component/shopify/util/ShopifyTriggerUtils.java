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
import static com.bytechef.component.shopify.util.ShopifyUtils.sendGraphQlQuery;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class ShopifyTriggerUtils {

    private ShopifyTriggerUtils() {
    }

    public static String subscribeWebhook(String webhookUrl, String topic, Context context) {
        String query = """
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

        Map<String, Object> variables = Map.of(
            "topic", topic, "webhookSubscription", Map.of("uri", webhookUrl));

        Map<String, Object> body = sendGraphQlQuery(query, context, variables);

        if (body.get("webhookSubscriptionCreate") instanceof Map<?, ?> webhookSubscriptionCreate &&
            webhookSubscriptionCreate.get("webhookSubscription") instanceof Map<?, ?> webhookSubscription) {

            return (String) webhookSubscription.get(ID);
        }

        throw new RuntimeException("Webhook not created successfully.");
    }

    public static void unsubscribeWebhook(Parameters outputParameters, Context context) {
        String query = """
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

        Map<String, Object> variables = Map.of(ID, outputParameters.getString(ID));

        sendGraphQlQuery(query, context, variables);
    }
}
