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

package com.bytechef.component.firecrawl.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.CATEGORIES;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.COUNTRY;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.IGNORE_INVALID_URLS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.LIMIT;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.LOCATION;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.QUERY;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.SCRAPE_OPTIONS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.SOURCES;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.TBS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.TIMEOUT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marko Krišković
 */
public class FirecrawlSearchAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("firecrawlSearch")
        .title("Search")
        .description("Search the web and optionally scrape search results using Firecrawl.")
        .properties(
            string(QUERY)
                .label("Search Query")
                .description("The search query string.")
                .minLength(1)
                .maxLength(400)
                .required(true),
            integer(LIMIT)
                .label("Limit")
                .description("Maximum number of results to return (1-100).")
                .minValue(1)
                .maxValue(100)
                .required(false),
            array(SOURCES)
                .label("Sources")
                .description("Sources to search. Determines the arrays available in the response.")
                .items(
                    object()
                        .properties(
                            string("type")
                                .label("Type")
                                .options(
                                    option("Web", "web"),
                                    option("Images", "images"),
                                    option("News", "news"))
                                .required(true)))
                .advancedOption(true)
                .required(false),
            array(CATEGORIES)
                .label("Categories")
                .description("Categories to filter results by (github, research, pdf).")
                .items(
                    object()
                        .properties(
                            string("type")
                                .label("Type")
                                .options(
                                    option("GitHub", "github"),
                                    option("Research", "research"),
                                    option("PDF", "pdf"))
                                .required(true)))
                .advancedOption(true)
                .required(false),
            string(TBS)
                .label("Time-Based Search")
                .description(
                    "Filter results by time periods.")
                .options(
                    option("last hour", "qdr:h"),
                    option("last day", "qdr:d"),
                    option("last week", "qdr:w"),
                    option("last month", "qdr:m"),
                    option("last year", "qdr:y"))
                .advancedOption(true)
                .required(false),
            string(LOCATION)
                .label("Location")
                .description(
                    "Location parameter for geo-targeted search results (e.g., 'San Francisco,California,United States').")
                .advancedOption(true)
                .required(false),
            string(COUNTRY)
                .label("Country")
                .description("ISO country code for geo-targeting search results (e.g., 'US', 'DE', 'FR', 'JP').")
                .advancedOption(true)
                .required(false),
            integer(TIMEOUT)
                .label("Timeout")
                .description("Timeout in milliseconds.")
                .advancedOption(true)
                .required(false),
            bool(IGNORE_INVALID_URLS)
                .label("Ignore Invalid URLs")
                .description(
                    "Excludes URLs from search results that are invalid for other Firecrawl endpoints. Useful when piping data to other Firecrawl API endpoints.")
                .advancedOption(true)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        bool("success"),
                        object("data")
                            .properties(
                                array("web")
                                    .items(
                                        object()
                                            .properties(
                                                string("title"),
                                                string("description"),
                                                string("url"),
                                                string("markdown"),
                                                string("html"),
                                                string("rawHtml"),
                                                array("links")
                                                    .items(string()),
                                                string("screenshot"),
                                                object("metadata")
                                                    .properties(
                                                        string("title"),
                                                        string("description"),
                                                        string("sourceURL"),
                                                        integer("statusCode"),
                                                        string("error")))),
                                array("images")
                                    .items(
                                        object()
                                            .properties(
                                                string("title"),
                                                string("imageUrl"),
                                                integer("imageWidth"),
                                                integer("imageHeight"),
                                                string("url"),
                                                integer("position"))),
                                array("news")
                                    .items(
                                        object()
                                            .properties(
                                                string("title"),
                                                string("snippet"),
                                                string("url"),
                                                string("date"),
                                                string("imageUrl"),
                                                integer("position"),
                                                string("markdown"),
                                                string("html"),
                                                string("rawHtml"),
                                                array("links")
                                                    .items(string()),
                                                string("screenshot"),
                                                object("metadata")
                                                    .properties(
                                                        string("title"),
                                                        string("description"),
                                                        string("sourceURL"),
                                                        integer("statusCode"),
                                                        string("error"))))),
                        string("warning"),
                        string("id"),
                        integer("creditsUsed"))))
        .perform(FirecrawlSearchAction::perform);

    private FirecrawlSearchAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, Object> body = createBody(inputParameters);

        return context
            .http(http -> http.post("/search"))
            .body(Context.Http.Body.of(body))
            .configuration(Context.Http.responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static Map<String, Object> createBody(Parameters inputParameters) {
        Map<String, Object> body = new HashMap<>();

        body.put(QUERY, inputParameters.getRequiredString(QUERY));
        body.put(LIMIT, inputParameters.getInteger(LIMIT));
        body.put(SOURCES, inputParameters.getList(SOURCES));
        body.put(CATEGORIES, inputParameters.getList(CATEGORIES));
        body.put(TBS, inputParameters.getString(TBS));
        body.put(LOCATION, inputParameters.getString(LOCATION));
        body.put(COUNTRY, inputParameters.getString(COUNTRY));
        body.put(TIMEOUT, inputParameters.getInteger(TIMEOUT));
        body.put(IGNORE_INVALID_URLS, inputParameters.getBoolean(IGNORE_INVALID_URLS));
        body.put(SCRAPE_OPTIONS, inputParameters.get(SCRAPE_OPTIONS));

        return body;
    }

}
