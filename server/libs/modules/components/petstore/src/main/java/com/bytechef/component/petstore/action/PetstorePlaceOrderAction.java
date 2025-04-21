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

package com.bytechef.component.petstore.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.petstore.property.PetstoreOrderProperties;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PetstorePlaceOrderAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("placeOrder")
        .title("Place an order for a pet")
        .description("Place a new order in the store")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/store/order", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(integer("id").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Id")
            .required(false)
            .exampleValue(10),
            integer("petId").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Pet Id")
                .required(false)
                .exampleValue(198772),
            integer("quantity").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Quantity")
                .required(false)
                .exampleValue(7),
            dateTime("shipDate").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Ship Date")
                .required(false),
            string("status").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Status")
                .description("Order Status")
                .options(option("Placed", "placed"), option("Approved", "approved"), option("Delivered", "delivered"))
                .required(false)
                .exampleValue("approved"),
            bool("complete").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Complete")
                .required(false))
        .output(outputSchema(object().properties(PetstoreOrderProperties.PROPERTIES)
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private PetstorePlaceOrderAction() {
    }
}
