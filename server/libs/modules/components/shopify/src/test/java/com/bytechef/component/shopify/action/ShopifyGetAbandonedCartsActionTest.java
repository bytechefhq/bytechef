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
        shopifyUtilsMockedStatic
            .when(() -> ShopifyUtils.executeGraphQlOperation(
                stringArgumentCaptor.capture(),
                contextArgumentCaptor.capture(),
                mapArgumentCaptor.capture(),
                stringArgumentCaptor.capture()))
            .thenReturn(Map.of());

        Object result = ShopifyGetAbandonedCartsAction.perform(
            null, null, mockedContext);

        assertEquals(List.of(), result);

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

        Map<String, Object> expectedVariables = Map.of("first", 100);

        assertEquals(List.of(expectedQuery, "abandonedCheckouts"), stringArgumentCaptor.getAllValues());
        assertEquals(expectedVariables, mapArgumentCaptor.getValue());
        assertEquals(mockedContext, contextArgumentCaptor.getValue());
    }
}
