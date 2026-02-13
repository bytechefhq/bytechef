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

package com.bytechef.component.shopify.action;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.shopify.util.ShopifyUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class ShopifyGetAbandonedCartsActionTest extends AbstractShopifyActionTest {

    @Test
    void testPerform() {
        String expectedQuery = """
            query ListAbandonedCheckouts($first: Int, $after: String) {
              abandonedCheckouts(first: $first, after: $after) {
                nodes {
                  id
                  abandonedCheckoutUrl
                  createdAt
                  customer {
                    id
                    firstName
                    lastName
                    email
                  }
                }
                pageInfo {
                  hasNextPage
                  endCursor
                }
              }
            }""";

        shopifyUtilsMockedStatic
            .when(() -> ShopifyUtils.executeGraphQlOperation(
                expectedQuery,
                mockedContext,
                Map.of("first", 100),
                "abandonedCheckouts"))
            .thenReturn(Map.of(
                "nodes", List.of(Map.of("id", "cart1")),
                "pageInfo", Map.of("hasNextPage", true, "endCursor", "cursor1")));

        shopifyUtilsMockedStatic
            .when(() -> ShopifyUtils.executeGraphQlOperation(
                expectedQuery,
                mockedContext,
                Map.of("first", 100, "after", "cursor1"),
                "abandonedCheckouts"))
            .thenReturn(Map.of(
                "nodes", List.of(Map.of("id", "cart2")),
                "pageInfo", Map.of("hasNextPage", false)));

        Object result = ShopifyGetAbandonedCartsAction.perform(null, null, mockedContext);

        assertEquals(List.of(Map.of("id", "cart1"), Map.of("id", "cart2")), result);
    }
}
