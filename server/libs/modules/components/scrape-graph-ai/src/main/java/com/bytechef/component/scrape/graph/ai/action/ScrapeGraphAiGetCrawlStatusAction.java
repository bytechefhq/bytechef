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

package com.bytechef.component.scrape.graph.ai.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ScrapeGraphAiGetCrawlStatusAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getCrawlStatus")
        .title("Get SmartCrawler Status")
        .description("Get the status and results of a previous smartcrawl request.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/crawl/{task_id}"

            ))
        .properties(string("task_id").label("Task Id")
            .description("The ID of the crawl job task.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .output(outputSchema(object().properties(string("status").description("Overall status of the request.")
            .required(false),
            object("result")
                .properties(
                    string("status").description("Status of the crawl job (e.g., 'done', 'processing', 'failed').")
                        .required(false),
                    object("llm_result")
                        .description(
                            "The extracted data from the crawled pages, structured according to the provided schema.")
                        .required(false),
                    array("crawled_urls").items(string().description("List of URLs that were crawled."))
                        .description("List of URLs that were crawled.")
                        .required(false),
                    array("pages").items(object().properties(string("url").description("The URL of the crawled page.")
                        .required(false),
                        string("markdown").description("The markdown content of the page.")
                            .required(false))
                        .description("List of crawled pages with their markdown content."))
                        .description("List of crawled pages with their markdown content.")
                        .required(false))
                .description("The crawl job result.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ScrapeGraphAiGetCrawlStatusAction() {
    }
}
