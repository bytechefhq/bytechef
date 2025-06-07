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

package com.bytechef.component.productboard.property;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class ProductboardFeatureProperties {
    public static final List<ComponentDsl.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(string("id").label("Id")
        .description("ID of the feature.")
        .required(false),
        string("name").label("Name")
            .description("Name of the feature.")
            .required(false),
        string("description").label("Description")
            .description("HTML-encoded description of the feature.")
            .required(false),
        string("type").label("Type")
            .description("Type of the feature.")
            .required(false),
        object("status").properties(string("id").label("Id")
            .description("ID of the status.")
            .required(false),
            string("name").label("Name")
                .description("Name of the status.")
                .required(false))
            .label("Status")
            .required(false),
        object("parent").properties(object("component").properties(string("id").label("Id")
            .description("ID of the parent component.")
            .required(false),
            object("links").properties(string("self").label("Self")
                .description("API link to the component.")
                .required(false))
                .label("Links")
                .required(false))
            .label("Component")
            .required(false))
            .label("Parent")
            .required(false),
        object("links").properties(string("self").label("Self")
            .description("API link to the feature.")
            .required(false),
            string("html").label("Html")
                .description("HTML link to the feature in Productboard.")
                .required(false))
            .label("Links")
            .required(false),
        bool("archived").label("Archived")
            .description("Whether the feature is archived.")
            .required(false),
        object("timeframe").properties(string("startDate").label("Start Date")
            .description("Start date of the timeframe.")
            .required(false),
            string("endDate").label("End Date")
                .description("End date of the timeframe.")
                .required(false),
            string("granularity").label("Granularity")
                .description("Granularity of the timeframe.")
                .required(false))
            .label("Timeframe")
            .required(false),
        object("owner").properties(ProductboardOwnerProperties.PROPERTIES)
            .label("Owner")
            .required(false),
        dateTime("createdAt").label("Created At")
            .description("Date and time when the feature was created.")
            .required(false),
        dateTime("updatedAt").label("Updated At")
            .description("Date and time when the feature was last updated.")
            .required(false),
        dateTime("lastHealthUpdate").label("Last Health Update")
            .description("Date and time of the last health update.")
            .required(false));

    private ProductboardFeatureProperties() {
    }
}
