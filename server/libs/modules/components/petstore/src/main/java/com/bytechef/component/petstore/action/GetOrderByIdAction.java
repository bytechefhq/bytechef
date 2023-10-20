
/*
 * Copyright 2021 <your company/name>.
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

import static com.bytechef.hermes.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;

import com.bytechef.component.petstore.property.OrderProperties;
import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class GetOrderByIdAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("getOrderById")
        .display(
            display("Find purchase order by ID")
                .description(
                    "For valid response try integer IDs with value <= 5 or > 10. Other values will generate exceptions."))
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/store/order/{orderId}"

            ))
        .properties(integer("orderId").label("OrderId")
            .description("ID of order that needs to be fetched")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .outputSchema(object().properties(OrderProperties.PROPERTIES)
            .metadata(
                Map.of(
                    "responseFormat", ResponseFormat.JSON)));
}
