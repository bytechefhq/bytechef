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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;

/**
 * @author Marko Krišković
 */
public class FirecrawlGetCrawlStatusAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getCrawlStatus")
        .title("Get Crawl Status")
        .description("Get the status and results of a crawl job.")
        .properties(
            string(ID)
                .label("Crawl ID")
                .description("The ID of the crawl job to retrieve status for.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("status")
                            .description("The current status of the crawl: scraping, completed, or failed."),
                        integer("total")
                            .description("The total number of pages that were attempted to be crawled."),
                        integer("completed")
                            .description("The number of pages that have been successfully crawled."),
                        integer("creditsUsed")
                            .description("The number of credits used for the crawl."),
                        string("expiresAt")
                            .description("The date and time when the crawl results will expire."),
                        string("next")
                            .description(
                                "URL to retrieve the next batch of data. Returned if the crawl is not completed " +
                                    "or if the response exceeds 10MB."),
                        array("data")
                            .description("The scraped data from each crawled page.")
                            .items(
                                object()
                                    .properties(
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
                                                string("language"),
                                                string("sourceURL"),
                                                string("keywords"),
                                                array("ogLocaleAlternate")
                                                    .items(string()),
                                                integer("statusCode"),
                                                string("error")))))))
        .help("", "https://docs.bytechef.io/reference/components/firecrawl_v1#get-crawl-status")
        .perform(FirecrawlGetCrawlStatusAction::perform);

    private FirecrawlGetCrawlStatusAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.get("/crawl/" + inputParameters.getRequiredString(ID)))
            .configuration(Http.responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
