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

package com.bytechef.component.specstream.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.COUNTRY_CODE;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.IMAGE_SEARCH;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.LANGUAGE_CODE;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.NEWS_SEARCH;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.NUMBER_OF_SOURCES;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.RESULT_PAGE;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.RETURN_PAGE_PASSAGES;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.SAFE_SEARCH_LEVEL;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.SEARCH_QUERY;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.SEARCH_TIER;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.USE_HTTP_POST;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Tarek Etman
 * @author Igor Beslic
 */
public class SpecStreamWebSearchAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("webSearch")
        .title("Web Search")
        .description(
            "Searches the live web for a query. Returns an answer or structured rows, and the ranked source pages " +
                "that support each value.")
        .properties(
            string(SEARCH_QUERY)
                .label("Search Query")
                .exampleValue("site:stripe.com who is the CEO of Stripe")
                .description(
                    "Question or request in plain words, in any language, from 1 to 2000 characters. Accepts the " +
                        "operators site:, -site:, -word, -\"phrase\", filetype:, after: and before: (YYYY, YYYY-MM " +
                        "or YYYY-MM-DD).")
                .minLength(1)
                .maxLength(2000)
                .required(true),
            string(SEARCH_TIER)
                .label("Search Tier")
                .exampleValue("fast")
                .description(
                    "Default reads the result pages and returns an answer, structured rows and sources. Fast " +
                        "returns only the search engine results with snippets in about 1 second, with no " +
                        "answer, rows or passages. Fast costs less per call.")
                .options(
                    option("Default (Answer, Rows and Sources)", "default"),
                    option("Fast (Sources Only)", "fast"))
                .defaultValue("default")
                .required(false),
            integer(NUMBER_OF_SOURCES)
                .label("Number of Sources")
                .exampleValue(20)
                .description(
                    "Maximum number of sources to return: 1 to 100, or 1 to 200 when Image Search is true. If " +
                        "empty, the response contains all sources that the answer used.")
                .minValue(1)
                .maxValue(200)
                .advancedOption(true)
                .required(false),
            integer(RESULT_PAGE)
                .label("Result Page")
                .exampleValue(2)
                .description(
                    "Page of the ranked source list to return, from 1 to 10. Each page has Number of Sources " +
                        "results (10 if empty). Only page 1 contains the answer and the rows.")
                .minValue(1)
                .maxValue(10)
                .advancedOption(true)
                .required(false),
            bool(RETURN_PAGE_PASSAGES)
                .label("Return Page Passages")
                .exampleValue(true)
                .description(
                    "If true, returns text passages from the top source pages that match the query. Adds up to " +
                        "about 1 second. Do not use together with News Search.")
                .advancedOption(true)
                .required(false),
            bool(NEWS_SEARCH)
                .label("News Search")
                .exampleValue(true)
                .description(
                    "If true, returns news items, newest first, instead of web results. Always uses the Fast search " +
                        "tier. Do not use together with Return Page Passages.")
                .advancedOption(true)
                .required(false),
            bool(IMAGE_SEARCH)
                .label("Image Search")
                .exampleValue(true)
                .description(
                    "If true, returns image results instead of web results. Always uses the Fast search tier and " +
                        "HTTP POST.")
                .advancedOption(true)
                .required(false),
            string(COUNTRY_CODE)
                .label("Country Code")
                .exampleValue("DE")
                .description(
                    "Two-letter ISO 3166-1 code of the country that the results are for. SpecStream sends it to " +
                        "each search engine that can use a country code.")
                .advancedOption(true)
                .required(false),
            string(LANGUAGE_CODE)
                .label("Language Code")
                .exampleValue("de")
                .description(
                    "Two-letter ISO 639-1 code of the result language. SpecStream sends it to each search engine " +
                        "that can use a language code.")
                .advancedOption(true)
                .required(false),
            string(SAFE_SEARCH_LEVEL)
                .label("Safe Search Level")
                .exampleValue("moderate")
                .description(
                    "Filter level for explicit content. SpecStream sends it to each search engine that can use " +
                        "a level. If empty, each search engine uses its own default level.")
                .options(
                    option("Off", "off"),
                    option("Moderate", "moderate"),
                    option("Strict", "strict"))
                .advancedOption(true)
                .required(false),
            bool(USE_HTTP_POST)
                .label("Send as HTTP POST")
                .exampleValue(true)
                .description(
                    "If true, sends the request as HTTP POST with the parameters in a JSON body. If false, sends " +
                        "the request as HTTP GET with the parameters in the URL query. Image Search always uses " +
                        "HTTP POST.")
                .advancedOption(true)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("data")
                            .properties(
                                string("id"),
                                string("request_id"),
                                string("query"),
                                string("kind"),
                                array("columns")
                                    .items(string()),
                                object("answer")
                                    .properties(
                                        string("value"),
                                        string("page"),
                                        string("seen_at"),
                                        bool("confirmed"),
                                        array("sources")
                                            .items(string())),
                                array("rows")
                                    .items(
                                        object()
                                            .properties(
                                                string("name"),
                                                string("page"),
                                                string("snippet"),
                                                number("probability"),
                                                object("fields"),
                                                array("sources")
                                                    .items(integer()))),
                                array("sources")
                                    .items(
                                        object()
                                            .properties(
                                                string("url"),
                                                string("title"),
                                                string("site"),
                                                string("snippet"),
                                                bool("cited"),
                                                string("published"),
                                                string("modified"),
                                                string("file_type"))),
                                array("passages")
                                    .items(
                                        object()
                                            .properties(
                                                string("url"),
                                                string("title"),
                                                string("text"))),
                                integer("page"),
                                integer("next_page"),
                                string("corrected_query"),
                                integer("took_ms"),
                                number("price_usd")))))
        .help("", "https://docs.bytechef.io/reference/components/specstream_v1#web-search")
        .perform(SpecStreamWebSearchAction::perform);

    private static final String WEB_SEARCH_PATH = "/web-search";

    private SpecStreamWebSearchAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        boolean useHttpPost = inputParameters.getBoolean(USE_HTTP_POST, false) ||
            inputParameters.getBoolean(IMAGE_SEARCH, false);

        Object[] searchParameters = getSearchParameters(inputParameters, useHttpPost);

        Executor executor;

        if (useHttpPost) {
            executor = context
                .http(http -> http.post(WEB_SEARCH_PATH))
                .body(Body.of(searchParameters));
        } else {
            executor = context
                .http(http -> http.get(WEB_SEARCH_PATH))
                .queryParameters(searchParameters);
        }

        return executor
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }

    /**
     * SpecStream declares the images parameter only in the HTTP POST body, so it is left out of an HTTP GET query.
     */
    private static Object[] getSearchParameters(Parameters inputParameters, boolean useHttpPost) {
        List<Object> searchParameters = new ArrayList<>(
            Arrays.asList(
                SEARCH_QUERY, inputParameters.getRequiredString(SEARCH_QUERY),
                SEARCH_TIER, inputParameters.getString(SEARCH_TIER),
                NUMBER_OF_SOURCES, inputParameters.getInteger(NUMBER_OF_SOURCES),
                RESULT_PAGE, inputParameters.getInteger(RESULT_PAGE),
                RETURN_PAGE_PASSAGES, inputParameters.getBoolean(RETURN_PAGE_PASSAGES),
                NEWS_SEARCH, inputParameters.getBoolean(NEWS_SEARCH),
                COUNTRY_CODE, inputParameters.getString(COUNTRY_CODE),
                LANGUAGE_CODE, inputParameters.getString(LANGUAGE_CODE),
                SAFE_SEARCH_LEVEL, inputParameters.getString(SAFE_SEARCH_LEVEL)));

        if (useHttpPost) {
            searchParameters.add(IMAGE_SEARCH);
            searchParameters.add(inputParameters.getBoolean(IMAGE_SEARCH));
        }

        return searchParameters.toArray();
    }
}
