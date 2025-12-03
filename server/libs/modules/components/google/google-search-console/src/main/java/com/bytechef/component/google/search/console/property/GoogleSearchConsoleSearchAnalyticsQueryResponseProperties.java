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

package com.bytechef.component.google.search.console.property;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class GoogleSearchConsoleSearchAnalyticsQueryResponseProperties {
    public static final List<ComponentDsl.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        string("responseAggregationType").label("Response Aggregation Type")
            .description("How the results were aggregated.")
            .options(option("AUTO", "AUTO"), option("BY_PROPERTY", "BY_PROPERTY"), option("BY_PAGE", "BY_PAGE"))
            .required(false),
        array("rows").items(object().properties(GoogleSearchConsoleApiDataRowProperties.PROPERTIES))
            .placeholder("Add to Rows")
            .label("Rows")
            .description("A list of rows grouped by the key values in the order given in the query.")
            .required(false));

    private GoogleSearchConsoleSearchAnalyticsQueryResponseProperties() {
    }
}
