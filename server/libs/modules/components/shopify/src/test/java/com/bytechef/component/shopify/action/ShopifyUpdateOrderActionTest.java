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

import static com.bytechef.component.shopify.constant.ShopifyConstants.EMAIL;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.INPUT;
import static com.bytechef.component.shopify.constant.ShopifyConstants.NOTE;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ORDER_ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.TAGS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ActionDefinition.SingleConnectionPerformFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.shopify.util.ShopifyUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class ShopifyUpdateOrderActionTest extends AbstractShopifyActionTest {

    private final Object mockedObject = Map.of("orderUpdate", Map.of());
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            ORDER_ID, "orderId",
            NOTE, "note",
            EMAIL, "email",
            TAGS, List.of("tag1", "tag2")));

    @Test
    void testPerform() throws Exception {
        shopifyUtilsMockedStatic
            .when(() -> ShopifyUtils.sendGraphQlQuery(
                stringArgumentCaptor.capture(),
                actionContextArgumentCaptor.capture(),
                (Map<String, Object>) objectArgumentCaptor.capture()))
            .thenReturn(mockedObject);

        Optional<PerformFunction> performFunction = ShopifyUpdateOrderAction.ACTION_DEFINITION.getPerform();

        assertTrue(performFunction.isPresent());

        SingleConnectionPerformFunction singleConnectionPerformFunction =
            (SingleConnectionPerformFunction) performFunction.get();

        Object result = singleConnectionPerformFunction.apply(
            mockedParameters, null, mockedActionContext);

        assertEquals(Map.of(), result);

        String expectedQuery = """
            mutation OrderUpdate($input: OrderInput!) {
              orderUpdate(input: $input) {
                order {
                  id
                  note
                  email
                  tags
                }
                userErrors {
                  field
                  message
                }
              }
            }""";

        Map<String, Object> expectedVariables = Map.of(
            INPUT, Map.of(
                ID, "orderId",
                NOTE, "note",
                EMAIL, "email",
                TAGS, List.of("tag1", "tag2")));

        assertEquals(expectedQuery, stringArgumentCaptor.getValue());
        assertEquals(expectedVariables, objectArgumentCaptor.getValue());
        assertEquals(mockedActionContext, actionContextArgumentCaptor.getValue());
    }
}
