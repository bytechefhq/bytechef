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

package com.bytechef.component.brave.action;

import static com.bytechef.component.brave.constant.BraveConstants.COUNT;
import static com.bytechef.component.brave.constant.BraveConstants.COUNTRY;
import static com.bytechef.component.brave.constant.BraveConstants.FRESHNESS;
import static com.bytechef.component.brave.constant.BraveConstants.OFFSET;
import static com.bytechef.component.brave.constant.BraveConstants.OPERATORS;
import static com.bytechef.component.brave.constant.BraveConstants.Q;
import static com.bytechef.component.brave.constant.BraveConstants.RESULT_FILTER;
import static com.bytechef.component.brave.constant.BraveConstants.SAFESEARCH;
import static com.bytechef.component.brave.constant.BraveConstants.SEARCH_LANG;
import static com.bytechef.component.brave.constant.BraveConstants.SUMMARY;
import static com.bytechef.component.definition.Authorization.API_TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Krišković
 */
public class BraveWebSearchAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("webSearch")
        .title("Web Search")
        .description("Search the web for relevant content.")
        .properties(
            string(Q)
                .label("Search Query")
                .description(
                    "The user’s search query term. Query can not be empty. Maximum of 400 characters and 50 words " +
                        "in the query.")
                .minLength(1)
                .maxLength(400)
                .required(true),
            integer(COUNT)
                .label("Count")
                .description(
                    "The number of search results returned in response. The maximum is 20. The actual number " +
                        "delivered may be less than requested. Combine this parameter with offset to paginate " +
                        "search results.")
                .minValue(1)
                .maxValue(20)
                .advancedOption(true)
                .required(false),
            integer(OFFSET)
                .label("Offset")
                .description(
                    "The zero based offset that indicates number of search result pages (count) to skip " +
                        "before returning the result. The actual number delivered may be less than requested.")
                .minValue(0)
                .maxValue(9)
                .advancedOption(true)
                .required(false),
            string(SAFESEARCH)
                .label("Safe Search")
                .description("The level of safe search filtering applied to the query.")
                .options(
                    option("Off", "off"),
                    option("Moderate", "moderate"),
                    option("Strict", "strict"))
                .advancedOption(true)
                .required(false),
            string(FRESHNESS)
                .label("Freshness")
                .description("Filters search results by when they were discovered.")
                .options(
                    option("Last 24 Hours", "pd"),
                    option("Last 7 Days", "pw"),
                    option("Last 31 Days", "pm"),
                    option("Last 365 Days", "py"))
                .advancedOption(true)
                .required(false),
            string(RESULT_FILTER)
                .label("Result Filter")
                .description("A comma delimited string of result types to include in the search response.")
                .defaultValue("discussions,faq,infobox,news,query,summarizer,videos,web,locations")
                .advancedOption(true)
                .required(false),
            bool(SUMMARY)
                .label("Summary")
                .description("This parameter enables summary key generation in web search results.")
                .advancedOption(true)
                .required(false),
            bool(OPERATORS)
                .label("Operators")
                .description("Whether to apply search operators.")
                .advancedOption(true)
                .required(false),
            string(COUNTRY)
                .label("Country")
                .description("The 2 character country code where the search results come from.")
                .options(
                    option("ALL", "ALL"),
                    option("US", "US"),
                    option("GB", "GB"),
                    option("AR", "AR"),
                    option("AT", "AT"),
                    option("BE", "BE"),
                    option("BR", "BR"),
                    option("CA", "CA"),
                    option("CL", "CL"),
                    option("DK", "DK"),
                    option("FI", "FI"),
                    option("FR", "FR"),
                    option("DE", "DE"),
                    option("GR", "GR"),
                    option("HK", "HK"),
                    option("IN", "IN"),
                    option("ID", "ID"),
                    option("IT", "IT"),
                    option("JP", "JP"),
                    option("KR", "KR"),
                    option("MY", "MY"),
                    option("MX", "MX"),
                    option("NL", "NL"),
                    option("NZ", "NZ"),
                    option("NO", "NO"),
                    option("CN", "CN"),
                    option("PL", "PL"),
                    option("PT", "PT"),
                    option("PH", "PH"),
                    option("RU", "RU"),
                    option("SA", "SA"),
                    option("ZA", "ZA"),
                    option("ES", "ES"),
                    option("SE", "SE"),
                    option("CH", "CH"),
                    option("TW", "TW"),
                    option("TR", "TR"))
                .advancedOption(true)
                .required(false),
            string(SEARCH_LANG)
                .label("Search Language")
                .description("The 2 or more character language code for which the search results are provided.")
                .options(
                    option("en", "en"),
                    option("en-gb", "en-gb"),
                    option("eu", "eu"),
                    option("ar", "ar"),
                    option("bn", "bn"),
                    option("bg", "bg"),
                    option("ca", "ca"),
                    option("zh-hans", "zh-hans"),
                    option("zh-hant", "zh-hant"),
                    option("hr", "hr"),
                    option("cs", "cs"),
                    option("da", "da"),
                    option("nl", "nl"),
                    option("et", "et"),
                    option("fi", "fi"),
                    option("fr", "fr"),
                    option("gl", "gl"),
                    option("de", "de"),
                    option("el", "el"),
                    option("gu", "gu"),
                    option("ge", "ge"),
                    option("he", "he"),
                    option("hi", "hi"),
                    option("hu", "hu"),
                    option("is", "is"),
                    option("it", "it"),
                    option("jp", "jp"),
                    option("kn", "kn"),
                    option("ko", "ko"),
                    option("lv", "lv"),
                    option("lt", "lt"),
                    option("ms", "ms"),
                    option("ml", "ml"),
                    option("mr", "mr"),
                    option("nb", "nb"),
                    option("pl", "pl"),
                    option("pt-br", "pt-br"),
                    option("pt-pt", "pt-pt"),
                    option("pa", "pa"),
                    option("ro", "ro"),
                    option("ru", "ur"),
                    option("sr", "sr"),
                    option("sk", "sk"),
                    option("sl", "sl"),
                    option("es", "es"),
                    option("sv", "sv"),
                    option("ta", "ta"),
                    option("te", "te"),
                    option("th", "th"),
                    option("tr", "tr"),
                    option("uk", "uk"),
                    option("vi", "vi"))
                .advancedOption(true)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("query")
                            .properties(string("original")),
                        object("discussions")
                            .properties(
                                array("results")
                                    .items(
                                        object()
                                            .properties(
                                                string("title"),
                                                string("url"),
                                                string("description")))),
                        object("faq")
                            .properties(
                                array("results")
                                    .items(
                                        object()
                                            .properties(
                                                string("question"),
                                                string("answer"),
                                                string("url"),
                                                string("title")))),
                        object("infobox")
                            .properties(
                                array("results")
                                    .items(
                                        object()
                                            .properties(
                                                string("title"),
                                                string("url"),
                                                string("description")))),
                        object("locations")
                            .properties(
                                array("results")
                                    .items(
                                        object()
                                            .properties(
                                                string("title"),
                                                string("url"),
                                                string("description")))),
                        object("news")
                            .properties(
                                array("results")
                                    .items(
                                        object()
                                            .properties(
                                                string("title"),
                                                string("url"),
                                                string("description")))),
                        object("videos")
                            .properties(
                                array("results")
                                    .items(
                                        object()
                                            .properties(
                                                string("title"),
                                                string("url"),
                                                string("description")))),
                        object("web")
                            .properties(
                                array("results")
                                    .items(
                                        object()
                                            .properties(
                                                string("title"),
                                                string("url"),
                                                string("description")))),
                        object("summarizer")
                            .properties(string("key")))))
        .help("", "https://docs.bytechef.io/reference/components/brave_v1#web-search")
        .perform(BraveWebSearchAction::perform);

    private BraveWebSearchAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.get("/web/search"))
            .headers(
                Map.of("Accept", List.of("application/json"),
                    "Accept-Encoding", List.of("gzip"),
                    "X-Subscription-Token", List.of(connectionParameters.getString(API_TOKEN))))
            .queryParameters(
                Q, inputParameters.getRequiredString(Q),
                COUNT, inputParameters.getInteger(COUNT),
                OFFSET, inputParameters.getInteger(OFFSET),
                SAFESEARCH, inputParameters.getString(SAFESEARCH),
                FRESHNESS, inputParameters.getString(FRESHNESS),
                RESULT_FILTER, inputParameters.getString(RESULT_FILTER),
                SUMMARY, inputParameters.getBoolean(SUMMARY),
                OPERATORS, inputParameters.getBoolean(OPERATORS),
                COUNTRY, inputParameters.getString(COUNTRY),
                SEARCH_LANG, inputParameters.getString(SEARCH_LANG))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
