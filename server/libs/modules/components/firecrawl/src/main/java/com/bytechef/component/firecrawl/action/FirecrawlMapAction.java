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
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.COUNTRY;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.IGNORE_CACHE;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.IGNORE_QUERY_PARAMETERS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.INCLUDE_SUBDOMAINS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.LANGUAGES;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.LIMIT;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.LOCATION;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.SEARCH;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.SITEMAP;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.TIMEOUT;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.URL;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Marko Krišković
 */
public class FirecrawlMapAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("firecrawlMap")
        .title("Map")
        .description("Map multiple URLs from a website based on specified options.")
        .properties(
            string(URL)
                .label("URL")
                .description("The base URL to start mapping from.")
                .required(true),
            string(SEARCH)
                .label("Search")
                .description(
                    "Specify a search query to order the results by relevance. Example: 'blog' will return URLs that contain the word 'blog' in the URL ordered by relevance.")
                .required(false),
            string(SITEMAP)
                .label("Sitemap")
                .description(
                    "Sitemap mode when mapping. If you set it to 'skip', the sitemap won't be used to find URLs. If you set it to 'only', only URLs that are in the sitemap will be returned. By default ('include'), the sitemap and other methods will be used together to find URLs.")
                .options(
                    option("Include", "include"),
                    option("Skip", "skip"),
                    option("Only", "only"))
                .advancedOption(true)
                .required(false),
            bool(INCLUDE_SUBDOMAINS)
                .label("Include Subdomains")
                .description("Include subdomains of the website.")
                .advancedOption(true)
                .required(false),
            bool(IGNORE_QUERY_PARAMETERS)
                .label("Ignore Query Parameters")
                .description("Do not return URLs with query parameters.")
                .advancedOption(true)
                .required(false),
            bool(IGNORE_CACHE)
                .label("Ignore Cache")
                .description(
                    "Bypass the sitemap cache to retrieve fresh URLs. Sitemap data is cached for up to 7 days; use this parameter when your sitemap has been recently updated.")
                .advancedOption(true)
                .required(false),
            integer(LIMIT)
                .label("Limit")
                .description("Maximum number of links to return (1-100000).")
                .minValue(1)
                .maxValue(100000)
                .advancedOption(true)
                .required(false),
            integer(TIMEOUT)
                .label("Timeout")
                .description("Timeout in milliseconds. There is no timeout by default.")
                .advancedOption(true)
                .required(false),
            object(LOCATION)
                .label("Location")
                .description(
                    "Location settings for the request. When specified, this will use an appropriate proxy if available and emulate the corresponding language and timezone settings. Defaults to 'US' if not specified.")
                .properties(
                    string(COUNTRY)
                        .label("Country")
                        .description("ISO 3166-1 alpha-2 country code (e.g., 'US', 'AU', 'DE', 'JP').")
                        .required(false),
                    array(LANGUAGES)
                        .label("Languages")
                        .description(
                            "Preferred languages and locales for the request in order of priority. Defaults to the language of the specified location.")
                        .items(string())
                        .required(false))
                .advancedOption(true)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        bool("success"),
                        array("links")
                            .items(
                                object()
                                    .properties(
                                        string("url"),
                                        string("title"),
                                        string("description"))))))
        .perform(FirecrawlMapAction::perform);

    private FirecrawlMapAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.post("/map"))
            .body(Context.Http.Body.of(
                URL, inputParameters.getRequiredString(URL),
                SEARCH, inputParameters.getString(SEARCH),
                SITEMAP, inputParameters.getString(SITEMAP),
                INCLUDE_SUBDOMAINS, inputParameters.getBoolean(INCLUDE_SUBDOMAINS),
                IGNORE_QUERY_PARAMETERS, inputParameters.getBoolean(IGNORE_QUERY_PARAMETERS),
                IGNORE_CACHE, inputParameters.getBoolean(IGNORE_CACHE),
                LIMIT, inputParameters.getInteger(LIMIT),
                TIMEOUT, inputParameters.getInteger(TIMEOUT),
                LOCATION, inputParameters.get(LOCATION)))
            .configuration(Context.Http.responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
