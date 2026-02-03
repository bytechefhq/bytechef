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

package com.bytechef.component.dhl.property;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class DhlTrackingShipmentsProperties {
    public static final List<ComponentDsl.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(string("url").label("Url")
        .description("A link to current page.")
        .required(false),
        string("prevUrl").label("Prev Url")
            .description("A link to the previous page.")
            .required(false),
        string("nextUrl").label("Next Url")
            .description("A link to the next page.")
            .required(false),
        string("firstUrl").label("First Url")
            .description("A link to the first page.")
            .required(false),
        string("lastUrl").label("Last Url")
            .description("A link to the last page.")
            .required(false),
        array("shipments").items(object().properties(DhlTrackingShipmentsProperties.PROPERTIES)
            .description("Unified tracking response object."))
            .placeholder("Add to Shipments")
            .label("Shipments")
            .description("An array of unified tracking shipments.")
            .required(false),
        array("possibleAdditionalShipmentsUrl")
            .items(string().description("An array of business services, where should be potentially shipment found."))
            .placeholder("Add to Possible Additional Shipments Url")
            .label("Possible Additional Shipments Url")
            .description("An array of business services, where should be potentially shipment found.")
            .required(false));

    private DhlTrackingShipmentsProperties() {
    }
}
