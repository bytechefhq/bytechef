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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
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
public class ScrapeUnblockerGetPageSourceAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getPageSource")
        .title("Get Page Source")
        .description(
            "Scrape a URL and return its fully rendered HTML, bypassing anti-bot protection such as Cloudflare, " +
                "DataDome, PerimeterX and Akamai.")
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
                .description("Number of seconds to wait after the page loads before the HTML is captured.")
                .required(false))
        .help("", "https://developers.scrapeunblocker.com")
        .output(
            outputSchema(
                string()
                    .description("The fully rendered HTML of the page.")))
        .perform(ScrapeUnblockerGetPageSourceAction::perform);

    private ScrapeUnblockerGetPageSourceAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("/getPageSource"))
            .configuration(responseType(ResponseType.TEXT))
            .queryParameters(
                URL, inputParameters.getRequiredString(URL),
                PROXY_COUNTRY, inputParameters.getString(PROXY_COUNTRY),
                TIME_SLEEP, inputParameters.getInteger(TIME_SLEEP))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
