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
import static com.bytechef.component.specstream.constant.SpecStreamConstants.FORMAT;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.INCLUDE;
import static com.bytechef.component.specstream.constant.SpecStreamConstants.URLS;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import java.util.List;

/**
 * @author Tarek Etman
 */
public class SpecStreamExtractAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("extract")
        .title("Extract Page Content")
        .description(
            "Reads up to 20 web pages or documents (PDF, Word, Excel, EPUB) and returns the main text of each, " +
                "without menus, footers and ads, as Markdown, plain text or HTML.")
        .properties(
            array(URLS)
                .label("Page URLs")
                .description(
                    "Public HTTP or HTTPS URLs of the web pages or documents to read, from 1 to 20. SpecStream reads " +
                        "a duplicate URL only once.")
                .items(string())
                .minItems(1)
                .maxItems(20)
                .required(true),
            string(FORMAT)
                .label("Text Format")
                .description("Format of the main text in the text field of each result.")
                .options(
                    option("Markdown", "markdown"),
                    option("Plain Text", "text"),
                    option("HTML", "html"))
                .defaultValue("markdown")
                .required(false),
            array(INCLUDE)
                .label("Additional Result Fields")
                .description(
                    "Fields to add to each result: the raw page HTML, the links on the page, the HTTP response " +
                        "(status code and final URL after redirects), or the full page text.")
                .items(string())
                .options(
                    List.of(
                        option("Raw HTML", "html"),
                        option("Page Links", "links"),
                        option("HTTP Status Code and Final URL", "response"),
                        option("Full Page Text", "full_text")))
                .advancedOption(true)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("data")
                            .properties(
                                string("request_id"),
                                array("results")
                                    .items(
                                        object()
                                            .properties(
                                                string("url"),
                                                string("status"),
                                                string("reason"),
                                                string("title"),
                                                string("text"),
                                                bool("text_truncated"),
                                                string("published"),
                                                string("modified"),
                                                string("read_from"),
                                                string("captured_at"),
                                                string("read_from_url"),
                                                array("emails")
                                                    .items(
                                                        object()
                                                            .properties(
                                                                string("address"),
                                                                string("found_in"))),
                                                string("html"),
                                                bool("html_truncated"),
                                                string("full_text"),
                                                bool("full_text_truncated"),
                                                array("links")
                                                    .items(
                                                        object()
                                                            .properties(
                                                                string("url"),
                                                                string("text"))),
                                                integer("status_code"),
                                                string("final_url"))),
                                integer("took_ms"),
                                number("price_usd")))))
        .help("", "https://docs.bytechef.io/reference/components/specstream_v1#extract-page-content")
        .perform(SpecStreamExtractAction::perform);

    private SpecStreamExtractAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<String> include = inputParameters.getList(INCLUDE, String.class);

        return context
            .http(http -> http.post("/extract"))
            .body(
                Body.of(
                    URLS, inputParameters.getRequiredList(URLS, String.class),
                    FORMAT, inputParameters.getString(FORMAT),
                    INCLUDE, (include != null && !include.isEmpty()) ? include : null))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
