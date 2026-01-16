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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
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
public class ShopifyDeleteOrderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteOrder")
        .title("Delete Order")
        .description("Deletes an order. Orders that interact with an online gateway can't be deleted.")
        .properties(
            string(ORDER_ID)
                .label("Order ID")
                .description("ID of the order to delete.")
                .required(true)
                .options((OptionsFunction<String>) ShopifyOptionsUtils::getOrderIdOptions))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("deletedId")
                            .description("ID of the deleted order."),
                        USER_ERRORS_PROPERTY)))
        .perform(ShopifyDeleteOrderAction::perform);

    private ShopifyDeleteOrderAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String query = """
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

        Map<String, Object> variables = Map.of(ORDER_ID, inputParameters.getRequiredString(ORDER_ID));

        return executeGraphQlOperation(query, context, variables, "orderDelete");
    }
}
