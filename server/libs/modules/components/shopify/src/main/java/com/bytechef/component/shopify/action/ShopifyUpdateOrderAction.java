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
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
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
public class ShopifyUpdateOrderAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("updateOrder")
        .title("Update an order")
        .description("Update an existing order.")
        .metadata(
            Map.of(
                "method", "PUT",
                "path", "/orders/{orderId}.json", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(integer("orderId").label("Order Id")
            .description("The order id.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            object("__item").properties(object("order").properties(string("note").label("Note")
                .description("An optional note that a shop owner can attach to the order.")
                .required(false),
                string("email").label("Email")
                    .description("The customer's email address.")
                    .required(false),
                string("phone").label("Phone")
                    .description("The customer's phone number for receiving SMS notifications.")
                    .required(false),
                string("tags").label("Tags")
                    .description("Tags attached to the order, formatted as a string of comma-separated values.")
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

    private ShopifyUpdateOrderAction() {
    }
}
