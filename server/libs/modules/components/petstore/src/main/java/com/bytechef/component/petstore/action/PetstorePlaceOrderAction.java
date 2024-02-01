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

package com.bytechef.component.petstore.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.petstore.property.PetstoreOrderProperties;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PetstorePlaceOrderAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("placeOrder")
        .title("Place an order for a pet")
        .description("Place a new order in the store")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/store/order", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("order").properties(PetstoreOrderProperties.PROPERTIES)
            .label("Order")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(PetstoreOrderProperties.PROPERTIES)
        .outputSchemaMetadata(Map.of(
            "responseType", ResponseType.JSON));
}
