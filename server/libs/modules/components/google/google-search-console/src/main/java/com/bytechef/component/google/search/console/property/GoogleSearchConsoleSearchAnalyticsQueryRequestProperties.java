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
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.integer;
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
public class GoogleSearchConsoleSearchAnalyticsQueryRequestProperties {
    public static final List<ComponentDsl.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        date("startDate").label("Start Date")
            .description("Start date of the requested date range. This value is included in the range.")
            .required(true),
        date("endDate").label("End Date")
            .description("End date of the requested date range. This value is included in the range.")
            .required(true),
        array("dimensions")
            .items(string()
                .description(
                    "Dimensions to group results by. Dimensions are the group-by values in the Search Analytics page.")
                .options(option("DATE", "DATE"), option("QUERY", "QUERY"), option("PAGE", "PAGE"),
                    option("COUNTRY", "COUNTRY"), option("DEVICE", "DEVICE"),
                    option("SEARCH_APPEARANCE", "SEARCH_APPEARANCE")))
            .placeholder("Add to Dimensions")
            .label("Dimensions")
            .description(
                "Dimensions to group results by. Dimensions are the group-by values in the Search Analytics page.")
            .required(false),
        string("type").label("Type")
            .description("Filter results to the following type.")
            .options(option("WEB", "WEB"), option("IMAGE", "IMAGE"), option("VIDEO", "VIDEO"), option("NEWS", "NEWS"),
                option("DISCOVER", "DISCOVER"), option("GOOGLE_NEWS", "GOOGLE_NEWS"))
            .defaultValue("WEB")
            .required(false),
        array("dimensionFilterGroups")
            .items(object().properties(GoogleSearchConsoleApiDimensionFilterGroupProperties.PROPERTIES))
            .placeholder("Add to Dimension Filter Groups")
            .label("Filters")
            .description("Filters to apply to the dimension grouping values.")
            .required(false),
        string("searchType").label("Search Type")
            .description("The search type to filter for.")
            .options(option("WEB", "WEB"), option("IMAGE", "IMAGE"), option("VIDEO", "VIDEO"), option("NEWS", "NEWS"),
                option("DISCOVER", "DISCOVER"), option("GOOGLE_NEWS", "GOOGLE_NEWS"))
            .defaultValue("WEB")
            .required(false),
        string("aggregationType").label("Aggregation Type")
            .description("How data is aggregated.")
            .options(option("AUTO", "AUTO"), option("BY_PROPERTY", "BY_PROPERTY"), option("BY_PAGE", "BY_PAGE"))
            .defaultValue("AUTO")
            .required(false),
        integer("rowLimit").minValue(1)
            .maxValue(25000)
            .label("Row Limit")
            .description("The maximum number of rows to return.")
            .defaultValue(1000)
            .required(false));

    private GoogleSearchConsoleSearchAnalyticsQueryRequestProperties() {
    }
}
