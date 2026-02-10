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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.shopify.constant.ShopifyConstants.LINE_ITEMS;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ORDER;
import static com.bytechef.component.shopify.constant.ShopifyConstants.PRODUCT;
import static com.bytechef.component.shopify.constant.ShopifyConstants.PRODUCTS;
import static com.bytechef.component.shopify.constant.ShopifyConstants.PRODUCT_ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.QUANTITY;
import static com.bytechef.component.shopify.constant.ShopifyConstants.USER_ERRORS_PROPERTY;
import static com.bytechef.component.shopify.util.ShopifyOptionsUtils.getLineItemsList;
import static com.bytechef.component.shopify.util.ShopifyUtils.executeGraphQlOperation;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.shopify.util.ShopifyOptionsUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 * @author Nikolina Spehar
 */
public class ShopifyCreateOrderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createOrder")
        .title("Create Order")
        .description(
            "Creates an order with attributes such as customer information, line items, and shipping and billing " +
                "addresses.")
        .properties(
            array(PRODUCTS)
                .label("Products")
                .description("List of products you want to order.")
                .minItems(1)
                .required(false)
                .items(
                    object(PRODUCT)
                        .label("Product")
                        .description("Product you want to order.")
                        .required(true)
                        .properties(
                            string(PRODUCT_ID)
                                .label("Product ID")
                                .description("ID of the product you want to order.")
                                .options((OptionsFunction<String>) ShopifyOptionsUtils::getProductIdOptions)
                                .required(true),
                            integer(QUANTITY)
                                .label("Quantity")
                                .description("How many products you want to order")
                                .required(true))))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("order")
                            .description("The created order.")
                            .properties(
                                string("id")
                                    .description("ID of the created order."),
                                object("lineItems")
                                    .properties(
                                        array("nodes")
                                            .items(
                                                object()
                                                    .properties(
                                                        string("id")
                                                            .description("ID of the line item."),
                                                        string("title")
                                                            .description("Title of the line item."),
                                                        integer("quantity")
                                                            .description("Quantity of the line item."),
                                                        object("variant")
                                                            .description("The product variant.")
                                                            .properties(
                                                                string("id")
                                                                    .description("ID of the product variant.")))))),
                        USER_ERRORS_PROPERTY)))
        .help("", "https://docs.bytechef.io/reference/components/shopify_v1#create-order")
        .perform(ShopifyCreateOrderAction::perform);

    private ShopifyCreateOrderAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String query = """
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

        List<Object> lineItems = getLineItemsList(context, inputParameters.getList(PRODUCTS, Object.class));

        Map<String, Object> variables = Map.of(ORDER, Map.of(LINE_ITEMS, lineItems));

        return executeGraphQlOperation(query, context, variables, "orderCreate");
    }
}
