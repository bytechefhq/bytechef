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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.shopify.util.ShopifyUtils.executeGraphQlOperation;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class ShopifyGetAbandonedCartsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getAbandonedCarts")
        .title("Get Abandoned Carts")
        .description("Retrieves abandoned carts.")
        .output(
            outputSchema(
                array()
                    .description("Array of abandoned carts.")
                    .items(
                        object()
                            .properties(
                                string("id")
                                    .description("ID of the abandoned cart."),
                                string("abandonedCheckoutUrl")
                                    .description("URL that leads to the abandoned checkout."),
                                string("createdAt")
                                    .description("DateTime when the cart was created."),
                                object("customer")
                                    .description("Customer info.")
                                    .properties(
                                        string("id")
                                            .description("ID of the customer that created the abandoned cart."),
                                        string("firstName")
                                            .description("First name of the customer."),
                                        string("lastName")
                                            .description("Last name of the customer."),
                                        string("email")
                                            .description("Email address of the customer"))))))
        .help("", "https://docs.bytechef.io/reference/components/shopify_v1#get-abandoned-carts")
        .perform(ShopifyGetAbandonedCartsAction::perform);

    private ShopifyGetAbandonedCartsAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String query = """
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

        Map<String, Object> variables = new HashMap<>();

        variables.put("first", 100);

        List<Object> abandonedCarts = new ArrayList<>();
        Map<?, ?> queryResultMap;

        do {
            Object queryResult = executeGraphQlOperation(query, context, variables, "abandonedCheckouts");

            if (queryResult instanceof Map<?, ?> resultMap && !resultMap.isEmpty()) {
                queryResultMap = resultMap;
                if (resultMap.get("nodes") instanceof Collection<?> nodes) {
                    abandonedCarts.addAll(nodes);
                }

                if (resultMap.get("pageInfo") instanceof Map<?, ?> pageInfo &&
                    Boolean.TRUE.equals(pageInfo.get("hasNextPage"))) {

                    variables.put("after", pageInfo.get("endCursor"));
                } else {
                    queryResultMap = null;
                }
            } else {
                queryResultMap = null;
            }
        } while (queryResultMap != null);

        return abandonedCarts;
    }
}
