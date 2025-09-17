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

package com.bytechef.component.zenrows.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zenrows.constant.ZenRowsConstants.AUTOPARSE;
import static com.bytechef.component.zenrows.constant.ZenRowsConstants.JS_RENDER;
import static com.bytechef.component.zenrows.constant.ZenRowsConstants.ORIGINAL_STATUS;
import static com.bytechef.component.zenrows.constant.ZenRowsConstants.URL;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

public class ZenRowsScrapeUrlAutoparseAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("scrapeUrlAutoparse")
        .title("Scrape URL Autoparse")
        .description("Get a JSON with the page's data. For most popular websites only.")
        .properties(
            string(URL)
                .label("URL")
                .description("URL of the site that will be scraped.")
                .required(true),
            bool(ORIGINAL_STATUS)
                .label("Original Status")
                .description(
                    "Return the original HTTP status code from the target page. Useful for debugging in case of " +
                        "errors.")
                .required(false),
            bool(JS_RENDER)
                .label("JS Render")
                .description(
                    "Enable JavaScript rendering with a headless browser. Essential for modern web apps, SPAs, and " +
                        "sites with dynamic content.")
                .required(false))
        .output(
            outputSchema(
                string()
                    .description("Scraped data.")))
        .perform(ZenRowsScrapeUrlAutoparseAction::perform);

    private ZenRowsScrapeUrlAutoparseAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.get(""))
            .configuration(responseType(ResponseType.TEXT))
            .queryParameters(
                URL, inputParameters.getRequiredString(URL),
                AUTOPARSE, true,
                ORIGINAL_STATUS, inputParameters.getBoolean(ORIGINAL_STATUS),
                JS_RENDER, inputParameters.getBoolean(JS_RENDER))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
