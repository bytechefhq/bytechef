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
import static com.bytechef.component.shopify.constant.ShopifyConstants.ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ORDER_ID;
import static com.bytechef.component.shopify.util.ShopifyUtils.executeGraphQlOperation;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.shopify.util.ShopifyOptionsUtils;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class ShopifyGetOrderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getOrder")
        .title("Get Order")
        .description("Get order by id.")
        .properties(
            string(ORDER_ID)
                .label("Order ID")
                .description("ID of the order you want to fetch.")
                .options((OptionsFunction<String>) ShopifyOptionsUtils::getOrderIdOptions)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("id")
                            .description("Order ID"),
                        string("name")
                            .description("Order name"),
                        object("totalPriceSet")
                            .description(
                                "The total price of the order, before returns, in shop and presentment currencies.")
                            .properties(
                                object("presentmentMoney")
                                    .description("Amount in presentment currency.")
                                    .properties(
                                        string("amount")
                                            .description("Amount of money."))),
                        string("displayFulfillmentStatus")
                            .description("Fulfillment status of the order."),
                        object("customer")
                            .description("Customer information.")
                            .properties(
                                string("email")
                                    .description("Customer email."),
                                string("phone")
                                    .description("Customer phone.")),
                        object("lineItems")
                            .description("A list of the order's line items.")
                            .properties(
                                array("nodes")
                                    .description("")
                                    .items(
                                        object()
                                            .properties(
                                                string("id")
                                                    .description("ID of the lineItem."),
                                                string("name")
                                                    .description("Name of the lineItem.")))))))
        .perform(ShopifyGetOrderAction::perform);

    private ShopifyGetOrderAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String query = """
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

        Map<String, Object> variables = Map.of(ID, inputParameters.getRequiredString(ORDER_ID));

        return executeGraphQlOperation(query, context, variables, "order");
    }
}
