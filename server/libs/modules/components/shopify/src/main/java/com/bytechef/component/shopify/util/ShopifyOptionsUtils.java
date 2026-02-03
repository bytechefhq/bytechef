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
import static com.bytechef.component.shopify.util.ShopifyUtils.sendGraphQlQuery;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class ShopifyOptionsUtils {

    private ShopifyOptionsUtils() {
    }

    public static List<Option<String>> getOrderIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        String query = """
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

        Map<String, Object> variables = new HashMap<>();
        variables.put("first", 250);

        Object body = sendGraphQlQuery(query, context, variables);

        Map<?, ?> orders = Map.of();
        if (body instanceof Map<?, ?> bodyMap &&
            bodyMap.get("orders") instanceof Map<?, ?> ordersMap) {

            orders = ordersMap;
        }

        getOrderOptionsFromEdges(options, orders.get("edges"));

        String endCursor = getNextPageEndCursor(orders.get("pageInfo"));

        while (!endCursor.isEmpty()) {
            variables.put("after", endCursor);

            body = sendGraphQlQuery(query, context, variables);

            if (body instanceof Map<?, ?> bodyMap &&
                bodyMap.get("orders") instanceof Map<?, ?> ordersMap) {

                orders = ordersMap;
            }

            getOrderOptionsFromEdges(options, orders.get("edges"));

            endCursor = getNextPageEndCursor(orders.get("pageInfo"));
        }

        return options.reversed();
    }

    public static List<Object> getLineItemsList(Context context, List<Object> lineItems) {
        String query = """
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

        List<Object> lineItemsList = new ArrayList<>();

        for (Object lineItem : lineItems) {
            if (lineItem instanceof Map<?, ?> product) {
                Map<String, Object> variables = Map.of(PRODUCT_ID, product.get(PRODUCT_ID));

                Object body = sendGraphQlQuery(query, context, variables);

                if (body instanceof Map<?, ?> bodyMap &&
                    bodyMap.get(PRODUCT) instanceof Map<?, ?> variant &&
                    variant.get("variants") instanceof Map<?, ?> variants &&
                    variants.get("edges") instanceof List<?> edges &&
                    edges.getFirst() instanceof Map<?, ?> edgeMap &&
                    edgeMap.get("node") instanceof Map<?, ?> node) {

                    lineItemsList.add(Map.of("variantId", node.get(ID), QUANTITY, product.get(QUANTITY)));
                }
            }
        }

        return lineItemsList;
    }

    private static String getNextPageEndCursor(Object pageInfoObject) {
        String endCursor = "";

        if (pageInfoObject instanceof Map<?, ?> pageInfoMap && (Boolean) pageInfoMap.get("hasNextPage")) {
            endCursor = (String) pageInfoMap.get("endCursor");
        }

        return endCursor;
    }

    private static void getOrderOptionsFromEdges(List<Option<String>> options, Object edgesObject) {
        if (edgesObject instanceof List<?> edgesList) {
            for (Object edgeObject : edgesList) {
                if (edgeObject instanceof Map<?, ?> edgeMap && edgeMap.get("node") instanceof Map<?, ?> node) {
                    options.add(option((String) node.get("name"), (String) node.get("id")));
                }
            }
        }
    }

    public static List<Option<String>> getProductIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        String query = """
            query GetProducts($first: Int!) {
              products(first: $first) {
                nodes {
                  id
                  title
                }
              }
            }
            """;

        Map<String, Object> variables = Map.of("first", 250);

        Object body = sendGraphQlQuery(query, context, variables);

        if (body instanceof Map<?, ?> bodyMap &&
            bodyMap.get(PRODUCTS) instanceof Map<?, ?> productsMap &&
            productsMap.get("nodes") instanceof List<?> nodes) {

            for (Object node : nodes) {
                if (node instanceof Map<?, ?> nodeMap) {
                    options.add(option((String) nodeMap.get("title"), (String) nodeMap.get(ID)));
                }
            }
        }

        return options;
    }
}
