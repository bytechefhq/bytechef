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

import static com.bytechef.component.shopify.constant.ShopifyConstants.ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ORDER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
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
class ShopifyGetOrderActionTest extends AbstractShopifyActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(ORDER_ID, "testOrderId"));

    @Test
    void testPerform() throws Exception {
        shopifyUtilsMockedStatic
            .when(() -> ShopifyUtils.executeGraphQlOperation(
                stringArgumentCaptor.capture(),
                actionContextArgumentCaptor.capture(),
                mapArgumentCaptor.capture(),
                stringArgumentCaptor.capture()))
            .thenReturn(Map.of());

        Optional<? extends ActionDefinition.BasePerformFunction> performFunction =
            ShopifyGetOrderAction.ACTION_DEFINITION.getPerform();

        assertTrue(performFunction.isPresent());

        PerformFunction singleConnectionPerformFunction = (PerformFunction) performFunction.get();

        Object result = singleConnectionPerformFunction.apply(
            mockedParameters, null, mockedActionContext);

        assertEquals(Map.of(), result);

        String expectedQuery = """
            query GetOrder($id: ID!) {
              order(id: $id) {
                id
                name
                totalPriceSet {
                  presentmentMoney {
                    amount
                  }
                }
                displayFulfillmentStatus
                customer {
                    email
                    phone
                }
                lineItems(first: 10) {
                  nodes {
                    id
                    name
                  }
                }
              }
            }
            """;

        Map<String, Object> expectedVariables = Map.of(ID, "testOrderId");

        assertEquals(List.of(expectedQuery, "order"), stringArgumentCaptor.getAllValues());
        assertEquals(expectedVariables, mapArgumentCaptor.getValue());
        assertEquals(mockedActionContext, actionContextArgumentCaptor.getValue());
    }
}
