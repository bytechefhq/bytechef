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

import static com.bytechef.component.shopify.constant.ShopifyConstants.LINE_ITEMS;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ORDER;
import static com.bytechef.component.shopify.constant.ShopifyConstants.PRODUCTS;
import static com.bytechef.component.shopify.constant.ShopifyConstants.PRODUCT_ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.QUANTITY;
import static com.bytechef.component.shopify.constant.ShopifyConstants.VARIANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.shopify.util.ShopifyOptionsUtils;
import com.bytechef.component.shopify.util.ShopifyUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class ShopifyCreateOrderActionTest extends AbstractShopifyActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(PRODUCTS, List.of(Map.of(PRODUCT_ID, "productId", QUANTITY, 2))));

    @Test
    void testPerform() {
        shopifyUtilsMockedStatic
            .when(() -> ShopifyUtils.executeGraphQlOperation(
                stringArgumentCaptor.capture(),
                contextArgumentCaptor.capture(),
                mapArgumentCaptor.capture(),
                stringArgumentCaptor.capture()))
            .thenReturn(Map.of());

        shopifyOptionsUtilsMockedStatic.when(
            () -> ShopifyOptionsUtils.getLineItemsList(
                contextArgumentCaptor.capture(), listArgumentCaptor.capture()))
            .thenReturn(List.of(Map.of(VARIANT_ID, "variantId", QUANTITY, 2)));

        Object result = ShopifyCreateOrderAction.perform(mockedParameters, null, mockedContext);

        assertEquals(Map.of(), result);

        String expectedQuery = """
            mutation orderCreate($order: OrderCreateOrderInput!, $options: OrderCreateOptionsInput) {
              orderCreate(order: $order, options: $options) {
                userErrors {
                  field
                  message
                }
                order {
                  id
                  lineItems(first: 5) {
                    nodes {
                      id
                      title
                      quantity
                      variant {
                        id
                      }
                    }
                  }
                }
              }
            }""";

        Map<String, Object> expectedVariables = Map.of(
            ORDER, Map.of(LINE_ITEMS, List.of(Map.of(VARIANT_ID, "variantId", QUANTITY, 2))));

        assertEquals(List.of(expectedQuery, "orderCreate"), stringArgumentCaptor.getAllValues());
        assertEquals(expectedVariables, mapArgumentCaptor.getValue());
        assertEquals(List.of(Map.of(PRODUCT_ID, "productId", QUANTITY, 2)), listArgumentCaptor.getValue());
        assertEquals(List.of(mockedContext, mockedContext), contextArgumentCaptor.getAllValues());
    }
}
