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

package com.bytechef.component.dhl.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.dhl.property.DhlTrackingShipmentsProperties;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class DhlTrackShipmentAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("trackShipment")
        .title("Track Shipment")
        .description("Retrieves the tracking information for shipments.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/track/shipments"

            ))
        .properties(string("trackingNumber").label("Tracking Number")
            .description("The tracking number of the shipment for which to return the information.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)))
        .output(outputSchema(object().properties(DhlTrackingShipmentsProperties.PROPERTIES)
            .description("Unified tracking response object.")
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private DhlTrackShipmentAction() {
    }
}
