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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ScrapeGraphAiSmartScraperAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("smartScraper")
        .title("Smart Scraper")
        .description("Extract content from a webpage using AI by providing a natural language prompt and a URL.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/smartscraper", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("user_prompt").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("User Prompt")
            .description("The search query or question you want to ask.")
            .required(true),
            string("website_url").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Website URL")
                .description("Website URL.")
                .required(true))
        .output(outputSchema(object()
            .properties(string("request_id").description("Unique identifier for the search request.")
                .required(false),
                string("status")
                    .description("Status of the request. One of: “queued”, “processing”, “completed”, “failed”.")
                    .required(false),
                string("website_url").description("The original website URL that was submitted.")
                    .required(false),
                string("user_prompt").description("The original search query that was submitted.")
                    .required(false),
                object("result").description("The search results.")
                    .required(false),
                string("error").description("Error message if the request failed. Empty string if successful.")
                    .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ScrapeGraphAiSmartScraperAction() {
    }
}
