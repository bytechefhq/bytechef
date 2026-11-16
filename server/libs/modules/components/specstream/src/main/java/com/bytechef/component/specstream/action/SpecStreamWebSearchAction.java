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
import static com.bytechef.component.specstream.constant.SpecStreamConstants.COUNTRY;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.IMAGES;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.LANGUAGE;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.NEWS;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.NUM;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.PAGE;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.PASSAGES;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.Q;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.SAFE;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.SPEED;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;

/**
 * @author Tarek Etman
 */
public class SpecStreamWebSearchAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("webSearch")
        .title("Web Search")
        .description("Search the live web and return answers, structured rows, and ranked sources.")
        .properties(
            string(Q)
                .label("Query")
                .description(
                    "The search query in plain words, in any language. Supports operators such as site:, " +
                        "filetype:, and quoted phrases.")
                .minLength(1)
                .maxLength(2000)
                .required(true),
            string(SPEED)
                .label("Speed")
                .description(
                    "Search tier. Default returns answers, rows, and sources. Fast skips model reading and " +
                        "returns sources.")
                .options(
                    option("Default", "default"),
                    option("Fast", "fast"))
                .defaultValue("default")
                .required(false),
            integer(NUM)
                .label("Number of Results")
                .description(
                    "Maximum number of sources to return (1 to 100 for web search, up to 200 with images enabled).")
                .minValue(1)
                .maxValue(200)
                .advancedOption(true)
                .required(false),
            integer(PAGE)
                .label("Page")
                .description("Page number (1 to 10) to continue the ranked result list.")
                .minValue(1)
                .maxValue(10)
                .advancedOption(true)
                .required(false),
            bool(PASSAGES)
                .label("Include Passages")
                .description("When true, reads source pages and returns relevant text passages.")
                .advancedOption(true)
                .required(false),
            bool(NEWS)
                .label("News")
                .description("When true, searches news results on the fast tier.")
                .advancedOption(true)
                .required(false),
            bool(IMAGES)
                .label("Images")
                .description("When true, returns image search results instead of web results.")
                .advancedOption(true)
                .required(false),
            string(COUNTRY)
                .label("Country")
                .description("Two-letter country code to target results (for example US or DE).")
                .advancedOption(true)
                .required(false),
            string(LANGUAGE)
                .label("Language")
                .description("Two-letter language code for results (for example en or de).")
                .advancedOption(true)
                .required(false),
            string(SAFE)
                .label("Safe Search")
                .description("Safe search filtering level.")
                .options(
                    option("Off", "off"),
                    option("Moderate", "moderate"),
                    option("Strict", "strict"))
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

    private SpecStreamWebSearchAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.get("/web-search"))
            .queryParameters(
                Q, inputParameters.getRequiredString(Q),
                SPEED, inputParameters.getString(SPEED),
                NUM, inputParameters.getInteger(NUM),
                PAGE, inputParameters.getInteger(PAGE),
                PASSAGES, inputParameters.getBoolean(PASSAGES),
                NEWS, inputParameters.getBoolean(NEWS),
                IMAGES, inputParameters.getBoolean(IMAGES),
                COUNTRY, inputParameters.getString(COUNTRY),
                LANGUAGE, inputParameters.getString(LANGUAGE),
                SAFE, inputParameters.getString(SAFE))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
