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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.shopify.constant.ShopifyConstants.NOTIFY_CUSTOMER;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ORDER_ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.QUERY;
import static com.bytechef.component.shopify.constant.ShopifyConstants.REASON;
import static com.bytechef.component.shopify.constant.ShopifyConstants.RESTOCK;
import static com.bytechef.component.shopify.constant.ShopifyConstants.STAFF_NOTE;
import static com.bytechef.component.shopify.constant.ShopifyConstants.VARIABLES;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.shopify.util.ShopifyUtils;

import java.util.Map;

public class ShopifyCancelOrderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("cancelOrder")
        .title("Cancel an order")
        .description(
            "Cancels an order, with options for refunding, restocking inventory, and customer notification." +
                "Order cancellation is irreversible.")
        .properties(
            integer(ORDER_ID)
                .label("Order ID")
                .description("ID of the order to cancel.")
                .required(true)
                .options((ActionDefinition.OptionsFunction<Long>) ShopifyUtils::getOrderIdOptions),
            string(REASON)
                .label("Reason")
                .description("The reason for canceling the order.")
                .required(true)
                .options(
                    option("The customer wanted to cancel the order.", "CUSTOMER"),
                    option("Payment was declined.", "DECLINED"),
                    option("The order was fraudulent.", "FRAUD"),
                    option("There was insufficient inventory.", "INVENTORY"),
                    option("The order was canceled for an unlisted reason.", "OTHER"),
                    option("Staff made an error.", "STAFF")),
            bool(RESTOCK)
                .label("Restock")
                .description("Whether to restock the inventory committed to the order.")
                .required(true),
            string(STAFF_NOTE)
                .label("Staff Note")
                .description("A staff-facing note about the order cancellation. This is not visible to the customer.")
                .maxLength(255)
                .required(false),
            bool(NOTIFY_CUSTOMER)
                .label("Notify Customer")
                .description("Whether to send a notification to the customer about the order cancellation.")
                .defaultValue(false)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("job")
                            .description("The job that asynchronously cancels the order.")
                            .properties(
                                bool("done")
                                    .description("This indicates if the job is still queued or has been run."),
                                string("id")
                                    .description("A globally-unique ID that's returned when running an asynchronous mutation."),
                                string("query")
                                    .description("This field will only resolve once the job is done. Can be used to ask for object(s) that have been changed by the job.")),
                                array("orderCancelUserErrors")
                                    .description("The list of errors that occurred from executing the mutation.")
                                    .items(
                                        object()
                                            .properties(
                                                string("code")
                                                    .description("The error code."),
                                                string("field")
                                                    .description("The path to the input field that caused the error."),
                                                string("message")
                                                    .description("The error message."))))))
        .perform(ShopifyCancelOrderAction::perform);

    private ShopifyCancelOrderAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.post(""))
            .body(
                Http.Body.of(
                    Map.of(
                        QUERY, "mutation OrderCancel($orderId: ID!, $notifyCustomer: Boolean, $restock: Boolean!, $reason: OrderCancelReason!, $staffNote: String) { orderCancel(orderId: $orderId, notifyCustomer: $notifyCustomer, refundMethod: $refundMethod, restock: $restock, reason: $reason, staffNote: $staffNote) { job { id done } orderCancelUserErrors { field message code } userErrors { field message } } }",
                        VARIABLES, Map.of(
                            ORDER_ID, inputParameters.getRequiredString(ORDER_ID), // "gid://shopify/Order/148977776"
                            NOTIFY_CUSTOMER, inputParameters.getBoolean(NOTIFY_CUSTOMER),
                            REASON, inputParameters.getRequiredString(REASON),
                            RESTOCK, inputParameters.getRequiredBoolean(RESTOCK),
                            STAFF_NOTE, inputParameters.getString(STAFF_NOTE)
                        )
                    )
//            "variables": {
//                "refundMethod": {
//                "originalPaymentMethodsRefund": true
//            },
//        }
                ))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
