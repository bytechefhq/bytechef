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

package com.bytechef.component.google.search.console.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.google.search.console.property.GoogleSearchConsoleApiDimensionFilterGroupProperties;
import com.bytechef.component.google.search.console.property.GoogleSearchConsoleSearchAnalyticsQueryResponseProperties;
import com.bytechef.component.google.search.console.util.GoogleSearchConsoleUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class GoogleSearchConsoleSearchAnalyticsAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("searchAnalytics")
        .title("Search Analytics")
        .description("Query your data with filters and parameters that you define.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/sites/{siteUrl}/searchAnalytics/query", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(string("siteUrl").label("Site URL")
            .description("The site's URL, including protocol. For example: `http://www.example.com/`.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) GoogleSearchConsoleUtils::getSiteUrlOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            date("startDate").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Start Date")
                .description("Start date of the requested date range. This value is included in the range.")
                .required(true),
            date("endDate").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("End Date")
                .description("End date of the requested date range. This value is included in the range.")
                .required(true),
            array("dimensions").items(string().metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .description(
                    "Dimensions to group results by. Dimensions are the group-by values in the Search Analytics page.")
                .options(option("DATE", "DATE"), option("QUERY", "QUERY"), option("PAGE", "PAGE"),
                    option("COUNTRY", "COUNTRY"), option("DEVICE", "DEVICE"),
                    option("SEARCH_APPEARANCE", "SEARCH_APPEARANCE")))
                .placeholder("Add to Dimensions")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Dimensions")
                .description(
                    "Dimensions to group results by. Dimensions are the group-by values in the Search Analytics page.")
                .required(false),
            string("type").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Type")
                .description("Filter results to the following type.")
                .options(option("WEB", "WEB"), option("IMAGE", "IMAGE"), option("VIDEO", "VIDEO"),
                    option("NEWS", "NEWS"), option("DISCOVER", "DISCOVER"), option("GOOGLE_NEWS", "GOOGLE_NEWS"))
                .defaultValue("WEB")
                .required(false),
            array("dimensionFilterGroups")
                .items(object().properties(GoogleSearchConsoleApiDimensionFilterGroupProperties.PROPERTIES))
                .placeholder("Add to Dimension Filter Groups")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Filters")
                .description("Filters to apply to the dimension grouping values.")
                .required(false),
            string("searchType").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Search Type")
                .description("The search type to filter for.")
                .options(option("WEB", "WEB"), option("IMAGE", "IMAGE"), option("VIDEO", "VIDEO"),
                    option("NEWS", "NEWS"), option("DISCOVER", "DISCOVER"), option("GOOGLE_NEWS", "GOOGLE_NEWS"))
                .defaultValue("WEB")
                .required(false),
            string("aggregationType").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Aggregation Type")
                .description("How data is aggregated.")
                .options(option("AUTO", "AUTO"), option("BY_PROPERTY", "BY_PROPERTY"), option("BY_PAGE", "BY_PAGE"))
                .defaultValue("AUTO")
                .required(false),
            integer("rowLimit").minValue(1)
                .maxValue(25000)
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Row Limit")
                .description("The maximum number of rows to return.")
                .defaultValue(1000)
                .required(false))
        .output(outputSchema(object().properties(GoogleSearchConsoleSearchAnalyticsQueryResponseProperties.PROPERTIES)
            .description(
                "A list of rows, one per result, grouped by key. Metrics in each row are aggregated for all data grouped by that key either by page or property, as specified by the aggregation type parameter.")
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private GoogleSearchConsoleSearchAnalyticsAction() {
    }
}
