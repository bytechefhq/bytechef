/*
 * Copyright 2023-present ByteChef Inc.
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

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.shopify.property.ShopifyOrderProperties;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ShopifyCreateOrderAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createOrder")
        .title("Create an order")
        .description("Adds an order into a Shopify store.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/orders.json", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item")
            .properties(object("order")
                .properties(
                    array("line_items")
                        .items(object().properties(string("fulfillment_status").label("Fulfillment Status")
                            .description("How far along an order is in terms line items fulfilled.")
                            .options(option("Null", "null"), option("Fulfilled", "fulfilled"),
                                option("Partial", "partial"), option("Not_eligible", "not_eligible"))
                            .required(false),
                            string("grams").label("Grams")
                                .description("The weight of the item in grams.")
                                .required(false),
                            number("price").label("Price")
                                .description(
                                    "The price of the item before discounts have been applied in the shop currency.")
                                .required(false),
                            integer("product_id").label("Product Id")
                                .description("The ID of the product that the line item belongs to.")
                                .required(false),
                            integer("variant_id").label("Variant Id")
                                .description("The ID of the product variant.")
                                .required(false),
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
                .label("Order")
                .required(false))
            .label("Order")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object().properties(ShopifyOrderProperties.PROPERTIES)
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private ShopifyCreateOrderAction() {
    }
}
