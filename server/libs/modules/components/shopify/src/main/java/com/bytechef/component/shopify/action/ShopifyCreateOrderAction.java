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

import com.bytechef.component.OpenApiComponentHandler.PropertyType;
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
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.shopify.constant.ShopifyConstants.NOTIFY_CUSTOMER;
import static com.bytechef.component.shopify.constant.ShopifyConstants.OPTIONS;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ORDER_ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.QUERY;
import static com.bytechef.component.shopify.constant.ShopifyConstants.REASON;
import static com.bytechef.component.shopify.constant.ShopifyConstants.RESTOCK;
import static com.bytechef.component.shopify.constant.ShopifyConstants.STAFF_NOTE;
import static com.bytechef.component.shopify.constant.ShopifyConstants.VARIABLES;

public class ShopifyCreateOrderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createOrder")
        .title("Create Order")
        .description(
            "Creates an order with attributes such as customer information, line items, and shipping and billing " +
                "addresses.")
        .properties(
//            buyerAcceptsMarketing
//            currency
            object("order")
                .properties(
                    array("line_items").items(object()
                            .properties(string("fulfillment_status").label("Fulfillment Status")
                                    .description("How far along an order is in terms line items fulfilled.")
                                    .options(option("Null", "null"), option("Fulfilled", "fulfilled"), option("Partial",
                                        "partial"), option("Not_eligible", "not_eligible"))
                                    .required(false),
                                string("grams").label("Grams")
                                    .description("The weight of the item in grams.")
                                    .required(false),
                                number("price").label("Price")
                                    .description(
                                        "The price of the item before discounts have been applied in the shop currency.")
                                    .required(false),
                                integer("product_id").label("Product ID")
                                    .description("The ID of the product that the line item belongs to.")
                                    .required(false)
                                    .options((ActionDefinition.OptionsFunction<Long>) ShopifyUtils::getProductIdOptions),
                                integer("variant_id").label("Variant ID")
                                    .description("The ID of the product variant.")
                                    .required(false)
                                    .options((ActionDefinition.OptionsFunction<Long>) ShopifyUtils::getVariantIdOptions)
                                    .optionsLookupDependsOn("order.line_items[index].product_id"),
                                integer("quantity").label("Quantity")
                                    .description("The number of items that were purchased.")
                                    .required(false),
                                string("title").label("Title")
                                    .description("The title of the product.")
                                    .required(false))
                            .description(
                                "The list of line item objects, each containing information about an item in the order."))
                        .placeholder("Add to Line Items")
                        .label("Line Items")
                        .description(
                            "The list of line item objects, each containing information about an item in the order.")
                        .required(false),
                    string("total_tax").label("Total Tax")
                        .description(
                            "The sum of all the taxes applied to the order in the shop currency. Must be positive.")
                        .required(false),
                    string("currency").label("Currency")
                        .description("The three-letter code (ISO 4217 format) for the shop currency")
                        .required(false))
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Order")
                .required(false)
        )
        .output(outputSchema(string()))
        .perform(ShopifyCreateOrderAction::perform);

    private ShopifyCreateOrderAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        // TODO

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
                        ))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
//            "query": "mutation orderCreate($order: OrderCreateOrderInput!, $options: OrderCreateOptionsInput) { orderCreate(order: $order, options: $options) { userErrors { field message } order { id totalTaxSet { shopMoney { amount currencyCode } } lineItems(first: 5) { nodes { variant { id } id title quantity taxLines { title rate priceSet { shopMoney { amount currencyCode } } } } } } } }",
//            "variables": {
//            "order": {
//                "currency": "EUR",
//                    "lineItems": [
//                {
//                    "title": "Big Brown Bear Boots",
//                    "priceSet": {
//                    "shopMoney": {
//                        "amount": 74.99,
//                            "currencyCode": "EUR"
//                    }
//                },
//                    "quantity": 3,
//                    "taxLines": [
//                    {
//                        "priceSet": {
//                        "shopMoney": {
//                            "amount": 13.5,
//                                "currencyCode": "EUR"
//                        }
//                    },
//                        "rate": 0.06,
//                        "title": "State tax"
//                    }
//          ]
//                }
//      ],
//                "transactions": [
//                {
//                    "kind": "SALE",
//                    "status": "SUCCESS",
//                    "amountSet": {
//                    "shopMoney": {
//                        "amount": 238.47,
//                            "currencyCode": "EUR"
    }
}
