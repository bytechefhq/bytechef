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

package com.bytechef.component.scrapeunblocker.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.scrapeunblocker.constant.ScrapeUnblockerConstants.MAIN_CONTENT_ONLY;
import static com.bytechef.component.scrapeunblocker.constant.ScrapeUnblockerConstants.MARKDOWN;
import static com.bytechef.component.scrapeunblocker.constant.ScrapeUnblockerConstants.PROXY_COUNTRY;
import static com.bytechef.component.scrapeunblocker.constant.ScrapeUnblockerConstants.TIME_SLEEP;
import static com.bytechef.component.scrapeunblocker.constant.ScrapeUnblockerConstants.URL;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Nerius Rutkauskas
 */
public class ScrapeUnblockerGetMarkdownAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getMarkdown")
        .title("Get Markdown")
        .description(
            "Scrape a URL and return the page as clean Markdown for LLMs and AI agents: headings, lists, tables and " +
                "absolute links, without scripts, styles, navigation or footer. Bypasses anti-bot protection such " +
                "as Cloudflare, DataDome, PerimeterX and Akamai.")
        .properties(
            string(URL)
                .label("URL")
                .description("The URL of the page to scrape.")
                .required(true),
            string(PROXY_COUNTRY)
                .label("Proxy Country")
                .description("Two-letter country code for the proxy exit location (e.g. US, DE).")
                .required(false),
            integer(TIME_SLEEP)
                .label("Wait Seconds")
                .description("Number of seconds to wait after the page loads before it is converted to Markdown.")
                .required(false),
            bool(MAIN_CONTENT_ONLY)
                .label("Main Content Only")
                .description(
                    "Keep only the main content of the page, without navigation, site header, footer and " +
                        "sidebars. Turn off to keep the whole page.")
                .defaultValue(true)
                .required(false))
        .help("", "https://docs.scrapeunblocker.com/guides/markdown")
        .output(
            outputSchema(
                string()
                    .description("The page content as Markdown.")))
        .perform(ScrapeUnblockerGetMarkdownAction::perform);

    private ScrapeUnblockerGetMarkdownAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("/getPageSource"))
            .configuration(responseType(ResponseType.TEXT))
            .queryParameters(
                URL, inputParameters.getRequiredString(URL),
                MARKDOWN, true,
                PROXY_COUNTRY, inputParameters.getString(PROXY_COUNTRY),
                TIME_SLEEP, inputParameters.getInteger(TIME_SLEEP),
                // The API keeps only the main content by default, so the parameter is sent only to turn it off.
                MAIN_CONTENT_ONLY, inputParameters.getBoolean(MAIN_CONTENT_ONLY, true) ? null : false)
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
