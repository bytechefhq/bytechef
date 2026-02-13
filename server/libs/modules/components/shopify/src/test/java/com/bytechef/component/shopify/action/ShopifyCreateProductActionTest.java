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

import static com.bytechef.component.shopify.constant.ShopifyConstants.NAME;
import static com.bytechef.component.shopify.constant.ShopifyConstants.PRODUCT_OPTIONS;
import static com.bytechef.component.shopify.constant.ShopifyConstants.STATUS;
import static com.bytechef.component.shopify.constant.ShopifyConstants.TITLE;
import static com.bytechef.component.shopify.constant.ShopifyConstants.VALUES;
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
class ShopifyCreateProductActionTest extends AbstractShopifyActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(TITLE, "title", PRODUCT_OPTIONS, List.of(Map.of(NAME, "name", VALUES, List.of("value")))));

    @Test
    void testPerform() {
        shopifyUtilsMockedStatic
            .when(() -> ShopifyUtils.executeGraphQlOperation(
                stringArgumentCaptor.capture(),
                contextArgumentCaptor.capture(),
                mapArgumentCaptor.capture(),
                stringArgumentCaptor.capture()))
            .thenReturn(Map.of());

        Object result = ShopifyCreateProductAction.perform(mockedParameters, null, mockedContext);

        assertEquals(Map.of(), result);

        String expectedQuery = """
            mutation CreateProduct($product: ProductCreateInput!) {
              productCreate(product: $product) {
                product {
                  id
                  title
                  options {
                    id
                    name
                    position
                    optionValues {
                      id
                      name
                      hasVariants
                    }
                  }
                }
                userErrors {
                  field
                  message
                }
              }
            }
            """;

        Map<String, Object> expectedVariables = Map.of(
            "product", Map.of(
                TITLE, "title",
                STATUS, "ACTIVE",
                PRODUCT_OPTIONS, List.of(Map.of(NAME, "name", VALUES, List.of(Map.of(NAME, "value"))))));

        assertEquals(List.of(expectedQuery, "productCreate"), stringArgumentCaptor.getAllValues());
        assertEquals(expectedVariables, mapArgumentCaptor.getValue());
        assertEquals(mockedContext, contextArgumentCaptor.getValue());
    }
}
