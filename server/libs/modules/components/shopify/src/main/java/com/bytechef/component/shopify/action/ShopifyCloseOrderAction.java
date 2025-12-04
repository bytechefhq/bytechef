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

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.shopify.util.ShopifyUtils;

import java.util.Map;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.shopify.constant.ShopifyConstants.NOTIFY_CUSTOMER;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ORDER_ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.QUERY;
import static com.bytechef.component.shopify.constant.ShopifyConstants.REASON;
import static com.bytechef.component.shopify.constant.ShopifyConstants.RESTOCK;
import static com.bytechef.component.shopify.constant.ShopifyConstants.STAFF_NOTE;
import static com.bytechef.component.shopify.constant.ShopifyConstants.VARIABLES;

public class ShopifyCloseOrderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("closeOrder")
        .title("Close Order")
        .description("Closes an open order.")
        .properties(
            integer(ORDER_ID)
                .label("Order ID")
                .description("ID of the order to close.")
                .required(true)
                .options((ActionDefinition.OptionsFunction<Long>) ShopifyUtils::getOrderIdOptions))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("order")
                            .description("The closed order.")
                            .properties(
                                array("additionalFees")
                                    .description("A list of additional fees applied to an order, such as duties, import fees, or tax lines.")
                            ),
                        array("userErrors")
                            .description("The list of errors that occurred from executing the mutation.")
                            .items(
                                object()
                                    .properties(
                                        string("field")
                                            .description("The path to the input field that caused the error."),
                                        string("message")
                                            .description("The error message.")))
                    )))
        .perform(ShopifyCloseOrderAction::perform);

    private ShopifyCloseOrderAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.post(""))
            .body(
                Http.Body.of(
                    Map.of(
                        QUERY, "mutation OrderClose($input: OrderCloseInput!) { orderClose(input: $input) { order { canMarkAsPaid cancelReason cancelledAt clientIp confirmed customer { displayName email } discountCodes } userErrors { field message } } }",
                        VARIABLES, Map.of(
                            "input", Map.of(
                            ORDER_ID, inputParameters.getRequiredString(ORDER_ID) // "gid://shopify/Order/148977776"
                            )))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
