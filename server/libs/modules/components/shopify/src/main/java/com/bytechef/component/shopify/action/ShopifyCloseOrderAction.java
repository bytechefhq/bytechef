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

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.shopify.property.ShopifyOrderProperties;
import com.bytechef.component.shopify.util.ShopifyUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ShopifyCloseOrderAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("closeOrder")
        .title("Close Order")
        .description(
            "Closes an order. A closed order is one that has no more work to be done. All items have been fulfilled or refunded.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/orders/{orderId}/close.json"

            ))
        .properties(integer("orderId").label("Order ID")
            .description("ID of the order to close.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<Long>) ShopifyUtils::getOrderIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .output(outputSchema(object().properties(ShopifyOrderProperties.PROPERTIES)
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ShopifyCloseOrderAction() {
    }
}
