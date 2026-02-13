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

import static com.bytechef.component.shopify.constant.ShopifyConstants.ORDER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.shopify.util.ShopifyUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class ShopifyDeleteOrderActionTest extends AbstractShopifyActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(ORDER_ID, "testOrderId"));

    @Test
    void testPerform() {
        shopifyUtilsMockedStatic
            .when(() -> ShopifyUtils.executeGraphQlOperation(
                stringArgumentCaptor.capture(),
                contextArgumentCaptor.capture(),
                mapArgumentCaptor.capture(),
                stringArgumentCaptor.capture()))
            .thenReturn(Map.of());

        Object result = ShopifyDeleteOrderAction.perform(mockedParameters, null, mockedContext);

        assertEquals(Map.of(), result);

        String expectedQuery = """
            mutation OrderDelete($orderId: ID!) {
              orderDelete(orderId: $orderId) {
                deletedId
                userErrors {
                  field
                  message
                  code
                }
              }
            }""";

        assertEquals(List.of(expectedQuery, "orderDelete"), stringArgumentCaptor.getAllValues());
        assertEquals(Map.of(ORDER_ID, "testOrderId"), mapArgumentCaptor.getValue());
        assertEquals(mockedContext, contextArgumentCaptor.getValue());
    }
}
