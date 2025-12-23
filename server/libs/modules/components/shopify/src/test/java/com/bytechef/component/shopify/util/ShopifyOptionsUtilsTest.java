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

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.PRODUCT;
import static com.bytechef.component.shopify.constant.ShopifyConstants.PRODUCTS;
import static com.bytechef.component.shopify.constant.ShopifyConstants.PRODUCT_ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.QUANTITY;
import static com.bytechef.component.shopify.util.ShopifyOptionsUtils.getLineItemsList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class ShopifyOptionsUtilsTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final Context mockedContext = mock(Context.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final ArgumentCaptor<Object> objectArgumentCaptor = forClass(Object.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testGetOrderIdOptions() {
        Map<String, Object> mockedObject = Map.of(
            "orders", Map.of("edges", List.of(Map.of("node", Map.of("name", "name", ID, "id")))),
            "pageInfo", Map.of("hasNextPage", false));

        try (MockedStatic<ShopifyUtils> shopifyUtilsMockedStatic = mockStatic(ShopifyUtils.class)) {
            shopifyUtilsMockedStatic
                .when(() -> ShopifyUtils.sendGraphQlQuery(
                    stringArgumentCaptor.capture(),
                    contextArgumentCaptor.capture(),
                    (Map<String, Object>) objectArgumentCaptor.capture()))
                .thenReturn(mockedObject);

            List<Option<String>> result = ShopifyOptionsUtils.getOrderIdOptions(
                mockedParameters, mockedParameters, Map.of(), "", mockedContext);

            List<Option<String>> expected = List.of(option("name", "id"));

            assertEquals(expected, result);

            String expectedQuery = """
                query GetOrders($first: Int!, $after: String) {
                  orders(first: $first, after: $after) {
                    edges {
                      cursor
                      node {
                        id
                        name
                      }
                    }
                    pageInfo {
                      hasNextPage
                      hasPreviousPage
                      startCursor
                      endCursor
                    }
                  }
                }
                """;

            Map<String, Object> expectedVariables = Map.of("first", 250);

            assertEquals(expectedQuery, stringArgumentCaptor.getValue());
            assertEquals(expectedVariables, objectArgumentCaptor.getValue());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
        }
    }

    @Test
    void testGetLineItemsList() {
        List<Object> mockedLineItems = List.of(Map.of(PRODUCT_ID, "productId", QUANTITY, 2));
        Map<String, Object> mockedObject = Map.of(
            PRODUCT, Map.of("variants", Map.of("edges", List.of(Map.of("node", Map.of(ID, "id"))))));

        try (MockedStatic<ShopifyUtils> shopifyUtilsMockedStatic = mockStatic(ShopifyUtils.class)) {
            shopifyUtilsMockedStatic
                .when(() -> ShopifyUtils.sendGraphQlQuery(
                    stringArgumentCaptor.capture(),
                    contextArgumentCaptor.capture(),
                    (Map<String, Object>) objectArgumentCaptor.capture()))
                .thenReturn(mockedObject);

            List<Object> result = getLineItemsList(mockedContext, mockedLineItems);

            List<Object> expected = List.of(Map.of("variantId", "id", QUANTITY, 2));

            assertEquals(expected, result);

            String expectedQuery = """
                query ProductFirstVariant($productId: ID!) {
                  product(id: $productId) {
                    id
                    title
                    variants(first: 1) {
                      edges {
                        node {
                          id
                          title
                          sku
                          price
                        }
                      }
                    }
                  }
                }""";

            Map<String, Object> expectedVariables = Map.of(PRODUCT_ID, "productId");

            assertEquals(expectedQuery, stringArgumentCaptor.getValue());
            assertEquals(expectedVariables, objectArgumentCaptor.getValue());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
        }
    }

    @Test
    void testProductIdOptions() {
        Map<String, Object> mockedObject = Map.of(
            PRODUCTS, Map.of("nodes", List.of(Map.of("title", "title", ID, "id"))));

        try (MockedStatic<ShopifyUtils> shopifyUtilsMockedStatic = mockStatic(ShopifyUtils.class)) {
            shopifyUtilsMockedStatic
                .when(() -> ShopifyUtils.sendGraphQlQuery(
                    stringArgumentCaptor.capture(),
                    contextArgumentCaptor.capture(),
                    (Map<String, Object>) objectArgumentCaptor.capture()))
                .thenReturn(mockedObject);

            List<Option<String>> result = ShopifyOptionsUtils.getProductIdOptions(
                mockedParameters, mockedParameters, Map.of(), "", mockedContext);

            List<Option<String>> expected = List.of(option("title", "id"));

            assertEquals(expected, result);

            String expectedQuery = """
                query GetProducts($first: Int!) {
                  products(first: $first) {
                    nodes {
                      id
                      title
                    }
                  }
                }
                """;

            Map<String, Object> expectedVariables = Map.of("first", 250);

            assertEquals(expectedQuery, stringArgumentCaptor.getValue());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
            assertEquals(expectedVariables, objectArgumentCaptor.getValue());
        }
    }
}
