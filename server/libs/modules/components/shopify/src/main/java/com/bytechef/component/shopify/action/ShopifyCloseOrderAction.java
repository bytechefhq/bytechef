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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.INPUT;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ORDER_ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.USER_ERRORS_PROPERTY;
import static com.bytechef.component.shopify.util.ShopifyUtils.executeGraphQlOperation;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.shopify.util.ShopifyOptionsUtils;
import java.util.Map;

/**
 * @author Monika Domiter
 * @author Nikolina Spehar
 */
public class ShopifyCloseOrderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("closeOrder")
        .title("Close Order")
        .description(
            "Marks an open Order as closed. A closed order is one where merchants fulfill or cancel all LineItem " +
                "objects and complete all financial transactions.")
        .properties(
            string(ORDER_ID)
                .label("Order ID")
                .description("ID of the order to close.")
                .required(true)
                .options((OptionsFunction<String>) ShopifyOptionsUtils::getOrderIdOptions))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("order")
                            .description("The closed order.")
                            .properties(
                                bool("canMarkAsPaid")
                                    .description("Whether an order can be manually marked as paid."),
                                object("cancelReason")
                                    .description("The reason provided for an order cancellation."),
                                dateTime("cancelledAt")
                                    .description("The date and time in ISO 8601 format when an order was canceled."),
                                string("clientIp")
                                    .description("The IP address of the customer who placed the order."),
                                bool("confirmed")
                                    .description("Whether inventory has been reserved for an order."),
                                array("discountCodes")
                                    .description(
                                        "The discount codes used for the order. Multiple codes can be applied to a " +
                                            "single order.")
                                    .items(
                                        string("discountCode")
                                            .description("The discount code used for an order."))),
                        USER_ERRORS_PROPERTY)))
        .help("", "https://docs.bytechef.io/reference/components/shopify_v1#close-order")
        .perform(ShopifyCloseOrderAction::perform);

    private ShopifyCloseOrderAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String query = """
            mutation OrderClose($input: OrderCloseInput!) {
              orderClose(input: $input) {
                order {
                  canMarkAsPaid
                  cancelReason
                  cancelledAt
                  clientIp
                  confirmed
                  customer {
                    displayName
                    email
                  }
                  discountCodes
                }
                userErrors {
                  field
                  message
                }
              }
            }""";

        Map<String, Object> variables = Map.of(INPUT, Map.of(ID, inputParameters.getRequiredString(ORDER_ID)));

        return executeGraphQlOperation(query, context, variables, "orderClose");
    }
}
