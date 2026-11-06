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

package com.bytechef.component.scrapingbee.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.COUNTRY_CODE;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.DEVICE;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.LIGHT_REQUEST;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.PAGE;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.SEARCH;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.SEARCH_TYPE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
public class ScrapingBeeGoogleSearchAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("googleSearch")
        .title("Google Search")
        .description(
            "Search Google through ScrapingBee and return the parsed results page as JSON. Costs 10 credits per " +
                "light request and 15 credits per regular request.")
        .properties(
            string(SEARCH)
                .label("Search")
                .description("The text to search for. Google search operators such as site: are supported.")
                .required(true),
            string(SEARCH_TYPE)
                .label("Search Type")
                .description("The type of Google search to run.")
                .options(
                    option("Classic", "classic"),
                    option("News", "news"),
                    option("Maps", "maps"),
                    option("Images", "images"),
                    option("Shopping", "shopping"),
                    option("AI Mode", "ai_mode"))
                .defaultValue("classic")
                .required(false),
            string(COUNTRY_CODE)
                .label("Country Code")
                .description("ISO 3166 country code the search is run from (e.g. us, gb, de). Defaults to us.")
                .required(false),
            string(DEVICE)
                .label("Device")
                .description("Device the search is run from.")
                .options(
                    option("Desktop", "desktop"),
                    option("Mobile", "mobile"))
                .required(false),
            integer(PAGE)
                .label("Page")
                .description("The results page to return, starting at 1.")
                .minValue(1)
                .required(false),
            bool(LIGHT_REQUEST)
                .label("Light Request")
                .description(
                    "Run a faster, cheaper request (10 credits) instead of a full browser request (15 credits). " +
                        "Enabled by default by ScrapingBee; AI overviews are only returned when it is disabled.")
                .advancedOption(true)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("meta_data"),
                        array("organic_results")
                            .items(
                                object()
                                    .properties(
                                        integer("position"),
                                        string("title"),
                                        string("url"),
                                        string("displayed_url"),
                                        string("description"))),
                        array("related_queries")
                            .items(object()),
                        array("questions")
                            .items(object()),
                        array("top_stories")
                            .items(object()),
                        array("news_results")
                            .items(object()),
                        array("local_results")
                            .items(object()),
                        object("knowledge_graph"))))
        .help("", "https://docs.bytechef.io/reference/components/scrapingbee_v1#google-search")
        .perform(ScrapingBeeGoogleSearchAction::perform);

    private ScrapingBeeGoogleSearchAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.get("/google"))
            .configuration(responseType(ResponseType.JSON))
            .queryParameters(
                SEARCH, inputParameters.getRequiredString(SEARCH),
                SEARCH_TYPE, inputParameters.getString(SEARCH_TYPE),
                COUNTRY_CODE, inputParameters.getString(COUNTRY_CODE),
                DEVICE, inputParameters.getString(DEVICE),
                PAGE, inputParameters.getInteger(PAGE),
                LIGHT_REQUEST, inputParameters.getBoolean(LIGHT_REQUEST))
            .execute()
            .getBody();
    }
}
