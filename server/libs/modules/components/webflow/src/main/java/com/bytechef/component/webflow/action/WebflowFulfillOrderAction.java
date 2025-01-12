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

package com.bytechef.component.webflow.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class WebflowFulfillOrderAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("fulfillOrder")
        .title("Fulfill Order")
        .description("Updates an order's status to fulfilled.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/sites/{siteId}/orders/{orderId}/fulfill"

            ))
        .properties(string("siteId").label("Site")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("orderId").label("Order")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)))
        .output(outputSchema(object()
            .properties(object("body").properties(string("orderId").required(false), string("status").required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private WebflowFulfillOrderAction() {
    }
}
